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
        UserAsset asset = assetRepository.findByUserAndCoinWithLock(user, coin)
                .orElseGet(() -> UserAsset.builder()
                        .user(user)
                        .coin(coin)
                        .amount(0f)
                        .avgPrice(0f)
                        .build()
                );

        boolean isBuy = req.getOrderType() == OrderType.BUY;
        double qty = req.getAmount();
        int cost = (int) (req.getAmount() * execPrice);

        // 매수/매도 공통 계산: 프론트에서 구분
        if (isBuy) {
            if (user.getCash() < cost) throw new IllegalArgumentException("잔액이 부족합니다.");
            user.setCash(user.getCash() - cost);
            // 평균 단가 재계산
            float totalQty = asset.getAmount() + (float)req.getAmount();
            float totalCost = asset.getAmount() * asset.getAvgPrice() + (float)cost;
            asset.setAmount(totalQty);
            asset.setAvgPrice(totalCost / totalQty);
        } else {  // SELL
            asset = assetRepository.findByUserAndCoinWithLock(user, coin)
                    .orElseThrow(() -> new IllegalArgumentException("보유 자산이 없습니다."));
            if (asset.getAmount() < qty) throw new IllegalArgumentException("보유 수량이 부족합니다.");
            user.setCash(user.getCash() + cost);
            asset.setAmount(asset.getAmount() - (float)qty);
        }
//        if (req.getAmount() > 0) {
//            // 매수
//            if (user.getCash() < cost) {
//                throw new IllegalArgumentException("잔액이 부족합니다.");
//            }
//            user.setCash(user.getCash() - cost);
//            // 평균 단가 재계산
//            float totalQty = asset.getAmount() + (float)req.getAmount();
//            float totalCost = asset.getAmount() * asset.getAvgPrice() + (float)cost;
//            asset.setAmount(totalQty);
//            asset.setAvgPrice(totalCost / totalQty);
//        } else {
//            // 매도
//            double sellQty = -req.getAmount();
//            if (asset.getAmount() < sellQty) {
//                throw new IllegalArgumentException("보유 수량이 부족합니다.");
//            }
//            user.setCash(user.getCash() + cost);
//            asset.setAmount(asset.getAmount() - (float)sellQty);
//        }

        // 5) 주문 엔티티 저장
        Order order = Order.builder()
                .user(user)
                .coin(coin)
                .season(season)
                .orderType(req.getOrderType())
                .status(Status.COMPLETED)
                .tradePrice((float)execPrice)
                .amount((float)Math.abs(req.getAmount()))
                .updatedAt(now)
                .createdAt(now)
                .build();
        Order savedOrder = orderRepository.save(order);
        assetRepository.save(asset);
        userRepository.save(user);
        // websocket 이벤트 발행
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
