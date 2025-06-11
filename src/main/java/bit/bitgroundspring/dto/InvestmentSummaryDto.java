package bit.bitgroundspring.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
public class InvestmentSummaryDto {
    // 1. 사용자 및 시즌 기본 정보
    private Integer userId;
    private String userName;
    private Integer seasonId;
    private String seasonName;
    private LocalDate seasonStartAt;
    private LocalDate seasonEndAt;

    // 2. 시즌 전체 성과 지표 (UserRanking.totalValue 및 고정 초기 자본 기반)
    // 참고: Float 타입 사용 시 금융 계산에서 정밀도 문제가 발생할 수 있습니다.
    private Float initialCashBalance; // 시즌 시작 시 초기 현금 잔고 (고정값 사용)
    private Float finalTotalValue;    // 시즌 종료 시점 총 자산 (UserRanking.totalValue)
    private Float totalProfitLossAmount; // 총 손익 금액 (finalTotalValue - initialCashBalance)
    private Float totalProfitLossPercentage; // 총 수익률 (%) ((finalTotalValue - initialCashBalance) / initialCashBalance * 100)
    private Integer finalRank;             // 시즌 최종 랭크 (UserRanking.ranks)

    // 3. 거래 활동 요약 (Order 엔티티 기반)
    private Integer totalTradeCount;             // 총 거래 횟수 (매수/매도 합계)
    private Integer buyOrderCount;               // 총 매수 주문 횟수
    private Integer sellOrderCount;              // 총 매도 주문 횟수
    private Float avgTradeAmount;           // 평균 거래 금액 (tradePrice * amount의 평균)
    private Float totalRealizedProfitLoss;  // 시즌 동안의 총 실현 손익 (단순 매도-매수 합계)
    private Map<String, Float> coinRealizedProfitLoss; // 코인별 실현 손익 (예: "KRW-BTC": 100000.0f)
    private Map<String, Integer> coinTradeCounts; // 코인별 거래 횟수 (예: "KRW-ETH": 15)

    // 4. 투자 성향 관련 지표 (Order 엔티티 기반 심화 분석)
    private String mostTradedCoinSymbol;       // 가장 많이 거래한 코인 심볼
    private Float mostTradedCoinTradeVolume; // 가장 많이 거래한 코인의 총 거래 금액
    private String highestProfitCoinSymbol;    // 가장 수익을 많이 낸 코인 심볼
    private Float highestProfitCoinAmount;  // 가장 수익을 많이 낸 코인의 수익 금액
    private String lowestProfitCoinSymbol;     // 가장 손실을 많이 낸 코인 심볼
    private Float lowestProfitCoinAmount;   // 가장 손실을 많이 낸 코인의 손실 금액
    // private Double avgHoldingPeriodDays;       // 평균 보유 기간 (구현 난이도 높음, 현재는 미포함)
    private Double avgTradesPerDay;            // 하루 평균 거래 횟수 (totalTradeCount / seasonDurationDays)
    // private Boolean engagedInHighFrequencyTrading; // 단타 위주였는지 여부 (구현 난이도 높음, 현재는 미포함)
    private Boolean focusedOnFewCoins;         // 소수 코인에 집중했는지 여부
}
