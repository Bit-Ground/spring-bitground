package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.OrderRequestDto;
import bit.bitgroundspring.dto.response.OrderResponseDto;
import bit.bitgroundspring.entity.*;
import bit.bitgroundspring.event.OrderCreatedEvent;
import bit.bitgroundspring.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final UserRepository userRepository;
    private final CoinRepository coinRepository;
    private final UserAssetRepository assetRepository;
    private final OrderRepository orderRepository;
    private final SeasonRepository seasonRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 슬리피지 허용 범위: 0.5% */
    private static final double SLIPPAGE_TOLERANCE = 0.005;

    @Value("${upbit.api.ticker-url}")
    private String tickerUrl;

    @Transactional
    public OrderResponseDto placeOrder(Integer userId, OrderRequestDto req) {
        // 1) 사용자 & 코인 엔티티 로드 (락 모드로 동시성 방어)
        User user = userRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Coin coin = coinRepository.findBySymbol(req.getSymbol())
                .orElseThrow(() -> new IllegalArgumentException("Coin not found"));

        Season season = seasonRepository.findByStatus(Status.PENDING)
                .orElseThrow(() -> new IllegalStateException("진행 중인 시즌이 없습니다."));

        RestTemplate restTemplate = new RestTemplate();
        // 2) 현재 시장가 조회
        String url = tickerUrl.replace("{symbol}", req.getSymbol());
        ResponseEntity<List<Map<String,Object>>> resp = restTemplate.exchange(
                url, HttpMethod.GET, HttpEntity.EMPTY,
                new org.springframework.core.ParameterizedTypeReference<>() {}
        );
        float marketPrice = ((Number)resp.getBody().get(0).get("trade_price")).floatValue();

        // 3) 주문 종류 분기
        boolean isLimitOrder = req.getLimitPrice() != null;
        double execPrice = marketPrice;
        LocalDateTime now = LocalDateTime.now();

        // 3-1) 지정가 주문: 허용 슬리피지 검증
        if (isLimitOrder) {
            double limitPrice = req.getLimitPrice();
            double diffRatio = Math.abs(limitPrice - marketPrice) / marketPrice;
            if (diffRatio > SLIPPAGE_TOLERANCE) {
                throw new IllegalArgumentException("지정가가 시장가에서 너무 벗어났습니다.");
            }
            execPrice = limitPrice;
        }

        // 4) 비용 계산 및 잔액/수량 검증
        boolean isBuy = req.getOrderType() == OrderType.BUY;
        double qty = req.getAmount();

        // 4) 비용 계산 및 잔액/수량 검증
        UserAsset asset = assetRepository.findByUserAndCoinWithLock(user, coin)
                .orElseGet(() -> UserAsset.builder()
                        .user(user)
                        .coin(coin)
                        .amount(0f)
                        .avgPrice(0f)
                        .build());

// 매수/매도 분기
        if (isBuy) {
            int cost = (int)(qty * execPrice);
            if (user.getCash() < cost) throw new IllegalArgumentException("잔액이 부족합니다.");
            user.setCash(user.getCash() - cost);

            float newQty   = asset.getAmount() + (float)qty;
            float newTotal = asset.getAmount()*asset.getAvgPrice() + cost;
            asset.setAmount(newQty);
            asset.setAvgPrice(newTotal / newQty);

            assetRepository.save(asset);
            userRepository.save(user);

        } else {  // SELL
            // EPS 허용 오차
            final float EPS = 0.00000001f;
            float currentAmt = asset.getAmount();
            if (currentAmt + EPS < qty) throw new IllegalArgumentException("보유 수량이 부족합니다.");

            int cost = (int)(qty * execPrice);
            user.setCash(user.getCash() + cost);

            float remaining = currentAmt - (float)qty;
            if (remaining < EPS) {
                assetRepository.delete(asset);
            } else {
                asset.setAmount(remaining);
                assetRepository.save(asset);
            }
            userRepository.save(user);
        }

// 5) 주문 저장 및 이벤트 발행(공통)
        Order order = Order.builder()
                .user(user)
                .coin(coin)
                .season(season)
                .orderType(req.getOrderType())
                .status(Status.COMPLETED)
                .tradePrice((float)execPrice)
                .amount((float)Math.abs(qty))
                .createdAt(now)
                .updatedAt(now)
                .build();
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCreatedEvent(this, savedOrder));

        // 6) 결과 반환
        return new OrderResponseDto(
                order.getId(),
                req.getSymbol(),
                coin.getKoreanName(),
                order.getOrderType(),
                order.getAmount(),
                order.getTradePrice(),
                order.getCreatedAt()
        );
    }
}
