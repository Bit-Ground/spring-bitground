// src/main/java/bit/bitgroundspring/service/InvestmentAnalysisService.java

package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.InvestmentSummaryDto;
import bit.bitgroundspring.entity.*;
import bit.bitgroundspring.repository.OrderRepository;
import bit.bitgroundspring.repository.RankRepository; // UserRankingRepository로 변경 권장
import bit.bitgroundspring.repository.UserRepository; // User 엔티티 조회용
import bit.bitgroundspring.repository.SeasonRepository; // Season 엔티티 조회용
import bit.bitgroundspring.util.InitialCashUtil; // InitialCashUtil 임포트

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j // 로깅을 위한 Lombok 어노테이션
public class InvestmentAnalysisService {

    private final OrderRepository orderRepository;
    private final RankRepository userRankingRepository; // RankRepository 대신 UserRankingRepository 사용 권장
    private final UserRepository userRepository; // User 엔티티를 찾기 위해 추가
    private final SeasonRepository seasonRepository; // Season 엔티티를 찾기 위해 추가
    private final InitialCashUtil initialCashUtil; // InitialCashUtil 주입

    /**
     * 특정 사용자 및 시즌에 대한 투자 요약 데이터를 생성합니다.
     * 이 데이터는 AI 조언 생성을 위한 입력으로 사용됩니다.
     *
     * @param userId 분석할 사용자의 ID
     * @param seasonId 분석할 시즌의 ID
     * @return InvestmentSummaryDto DTO (분석 결과 요약)
     */
    public InvestmentSummaryDto getUserInvestmentSummaryForSeason(Integer userId, Integer seasonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new IllegalArgumentException("Season not found with ID: " + seasonId));

        // 1. 시즌 전체 성과 지표 로딩 및 계산
        UserRanking userRanking = userRankingRepository.findByUserAndSeason(user, season)
                .orElse(null); // 해당 시즌의 랭킹이 없을 수도 있음

        // finalTotalValue는 UserRanking에서 Integer로 가져오며, DTO에도 Integer로 전달
        Integer finalTotalValue = (userRanking != null && userRanking.getTotalValue() != null)
                ? userRanking.getTotalValue() // Integer totalValue를 그대로 사용
                : 0; // Integer 기본값

        // InitialCashUtil에서 가져온 값은 Double이므로 Integer로 변환
        Integer initialCashBalance = (int) initialCashUtil.getInitialCash(); // Double -> Integer로 변환

        // 총 손익 금액 계산 (Integer)
        Integer totalProfitLossAmount = finalTotalValue - initialCashBalance;

        // 총 수익률 계산 (Float으로 형변환하여 소수점 계산)
        Float totalProfitLossPercentage = 0.0F;
        // initialCashBalance가 0이 아닌 경우에만 수익률 계산
        if (initialCashBalance != 0) {
            totalProfitLossPercentage = ((float) totalProfitLossAmount / initialCashBalance) * 100.0F;
        }

        // 2. 거래 활동 요약 (Order 엔티티 기반)
        // 해당 시즌의 완료된 주문만 조회 (PENDING 상태는 제외)
        List<Order> completedOrders = orderRepository.findByUserAndSeasonAndStatus(
                user, season, Status.COMPLETED);

        Integer totalTradeCount = completedOrders.size();
        Integer buyOrderCount = (int) completedOrders.stream()
                .filter(order -> order.getOrderType() == OrderType.BUY)
                .count();
        Integer sellOrderCount = (int) completedOrders.stream()
                .filter(order -> order.getOrderType() == OrderType.SELL)
                .count();

        // 평균 거래 금액 계산 (Order의 tradePrice와 amount는 Float/Double이므로, 결과는 Float 유지)
        Float totalTradeValueSum = completedOrders.stream()
                .map(order -> (float) (order.getTradePrice() * order.getAmount()))
                .reduce(0.0F, Float::sum);
        Float avgTradeAmount = (totalTradeCount > 0)
                ? totalTradeValueSum / totalTradeCount
                : 0.0F;

        // 시즌 동안의 총 실현 손익 및 코인별 실현 손익 계산 (단순화된 현금 흐름 기반)
        // Order의 tradePrice와 amount는 Float/Double이므로, 결과는 Float 유지
        Float totalRealizedProfitLoss = 0.0F;
        Map<String, Float> coinRealizedProfitLoss = new HashMap<>();

        Map<String, List<Order>> ordersByCoin = completedOrders.stream()
                .collect(Collectors.groupingBy(order -> order.getCoin().getSymbol()));

        for (Map.Entry<String, List<Order>> entry : ordersByCoin.entrySet()) {
            String coinSymbol = entry.getKey();
            List<Order> coinOrders = entry.getValue();

            Float coinBuyValue = 0.0F;
            Float coinSellValue = 0.0F;

            for (Order order : coinOrders) {
                Float tradeValue = (float)(order.getTradePrice() * order.getAmount());
                if (order.getOrderType() == OrderType.BUY) {
                    coinBuyValue += tradeValue;
                } else if (order.getOrderType() == OrderType.SELL) {
                    coinSellValue += tradeValue;
                }
            }
            Float currentCoinPL = coinSellValue - coinBuyValue;
            coinRealizedProfitLoss.put(coinSymbol, currentCoinPL);
            totalRealizedProfitLoss += currentCoinPL;
        }

        // 코인별 거래 횟수
        Map<String, Integer> coinTradeCounts = completedOrders.stream()
                .collect(Collectors.groupingBy(order -> order.getCoin().getSymbol(),
                        Collectors.summingInt(order -> 1)));

        // 3. 투자 성향 관련 지표 (Order 엔티티 기반 심화 분석)
        String mostTradedCoinSymbol = null;
        Float mostTradedCoinTradeVolume = 0.0F; // Float 유지
        if (!completedOrders.isEmpty()) {
            Map<String, Float> coinVolumes = new HashMap<>();
            for (Order order : completedOrders) {
                String symbol = order.getCoin().getSymbol();
                Float tradeVolume = (float)(order.getTradePrice() * order.getAmount());
                coinVolumes.put(symbol, coinVolumes.getOrDefault(symbol, 0.0F) + tradeVolume);
            }

            mostTradedCoinSymbol = coinVolumes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            mostTradedCoinTradeVolume = coinVolumes.getOrDefault(mostTradedCoinSymbol, 0.0F);
        }

        // 가장 수익/손실 많이 낸 코인 (coinRealizedProfitLoss 맵 활용) - Float 유지
        String highestProfitCoinSymbol = null;
        Float highestProfitCoinAmount = 0.0F;
        String lowestProfitCoinSymbol = null;
        Float lowestProfitCoinAmount = 0.0F;

        if (!coinRealizedProfitLoss.isEmpty()) {
            highestProfitCoinSymbol = coinRealizedProfitLoss.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            highestProfitCoinAmount = coinRealizedProfitLoss.getOrDefault(highestProfitCoinSymbol, 0.0F);

            lowestProfitCoinSymbol = coinRealizedProfitLoss.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            lowestProfitCoinAmount = coinRealizedProfitLoss.getOrDefault(lowestProfitCoinSymbol, 0.0F);
        }

        // 하루 평균 거래 횟수
        long seasonDurationDays = ChronoUnit.DAYS.between(season.getStartAt(), season.getEndAt().plusDays(1)); // endAt 포함
        Double avgTradesPerDay = (seasonDurationDays > 0)
                ? (double) totalTradeCount / seasonDurationDays
                : (totalTradeCount > 0 ? (double)totalTradeCount : 0.0);

        // 소수 코인에 집중했는지 여부
        Boolean focusedOnFewCoins = false;
        if (totalTradeCount > 0) {
            long distinctCoinsTraded = completedOrders.stream()
                    .map(order -> order.getCoin().getSymbol())
                    .distinct()
                    .count();
            if (distinctCoinsTraded <= 3 && totalTradeCount > 0) {
                focusedOnFewCoins = true;
            }
        }

        return InvestmentSummaryDto.builder()
                .userId(user.getId())
                .userName(user.getName())
                .seasonId(season.getId())
                .seasonName(season.getName())
                .seasonStartAt(season.getStartAt())
                .seasonEndAt(season.getEndAt())
                .initialCashBalance(initialCashBalance) // Integer 값 전달
                .finalTotalValue(finalTotalValue)    // Integer 값 전달
                .totalProfitLossAmount(totalProfitLossAmount) // Integer 값 전달
                .totalProfitLossPercentage(totalProfitLossPercentage) // Float 값 전달
                .finalRank((userRanking != null && userRanking.getRanks() != null) ? userRanking.getRanks() : null)
                .totalTradeCount(totalTradeCount)
                .buyOrderCount(buyOrderCount)
                .sellOrderCount(sellOrderCount)
                .avgTradeAmount(avgTradeAmount)
                .totalRealizedProfitLoss(totalRealizedProfitLoss)
                .coinRealizedProfitLoss(coinRealizedProfitLoss)
                .coinTradeCounts(coinTradeCounts)
                .mostTradedCoinSymbol(mostTradedCoinSymbol)
                .mostTradedCoinTradeVolume(mostTradedCoinTradeVolume)
                .highestProfitCoinSymbol(highestProfitCoinSymbol)
                .highestProfitCoinAmount(highestProfitCoinAmount)
                .lowestProfitCoinSymbol(lowestProfitCoinSymbol)
                .lowestProfitCoinAmount(lowestProfitCoinAmount)
                .avgTradesPerDay(avgTradesPerDay)
                .focusedOnFewCoins(focusedOnFewCoins)
                .build();
    }
}