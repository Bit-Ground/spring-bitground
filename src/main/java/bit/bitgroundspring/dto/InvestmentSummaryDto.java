// bit/bitgroundspring/dto/InvestmentSummaryDto.java

package bit.bitgroundspring.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentSummaryDto {

    // 1. 사용자 및 시즌 기본 정보
    private Integer userId;
    private String userName;
    private Integer seasonId;
    private String seasonName;
    private LocalDate seasonStartAt;
    private LocalDate seasonEndAt;

    // 2. 시즌 전체 성과 지표
    private Integer initialCashBalance;       // 시즌 시작 시 초기 현금 잔고 (Integer로 변경)
    private Integer finalTotalValue;          // 시즌 종료 시점 최종 총 자산 (Integer로 변경)
    private Integer totalProfitLossAmount;    // 총 손익 금액 (Integer로 변경)
    private Float totalProfitLossPercentage; // 총 수익률 (%) (Float 유지)
    private Integer finalRank;             // 시즌 최종 랭크

    // 3. 거래 활동 요약
    private Integer totalTradeCount;             // 총 거래 횟수
    private Integer buyOrderCount;               // 총 매수 주문 횟수
    private Integer sellOrderCount;              // 총 매도 주문 횟수
    private Float avgTradeAmount;           // 평균 거래 금액 (Float 유지)
    private Float totalRealizedProfitLoss;  // 총 실현 손익 (Float 유지)
    private Map<String, Float> coinRealizedProfitLoss; // 코인별 실현 손익 (Float 유지)
    private Map<String, Integer> coinTradeCounts; // 코인별 거래 횟수

    // 4. 투자 성향 관련 지표
    private String mostTradedCoinSymbol;       // 가장 많이 거래한 코인 심볼
    private Float mostTradedCoinTradeVolume; // 가장 많이 거래한 코인의 총 거래 금액 (Float 유지)
    private String highestProfitCoinSymbol;    // 가장 수익을 많이 낸 코인 심볼
    private Float highestProfitCoinAmount;  // 가장 수익을 많이 낸 코인의 수익 금액 (Float 유지)
    private String lowestProfitCoinSymbol;     // 가장 손실을 많이 낸 코인 심볼
    private Float lowestProfitCoinAmount;   // 가장 손실을 많이 낸 코인의 손실 금액 (Float 유지)
    private Double avgTradesPerDay;            // 하루 평균 거래 횟수 (Double 유지)
    private Boolean focusedOnFewCoins;         // 소수 코인에 집중했는지 여부
}
