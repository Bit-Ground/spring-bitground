// src/main/java/bit/bitgroundspring/service/GeminiService.java

package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.AiAdviceDto;
import bit.bitgroundspring.dto.InvestmentSummaryDto;
import bit.bitgroundspring.entity.UserDailyBalance; // UserDailyBalance import

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Comparator; // Comparator 임포트 추가
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    // 이 필드는 이제 사용되지 않을 수 있지만, 기존 구조 유지를 위해 둡니다.
    private final String modelName;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    // HttpClient는 한 번만 생성하여 재사용하는 것이 효율적입니다.
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";


    public GeminiService(
            @Value("${gemini.model-name}") String modelName,
            @Value("${gemini.api-key}") String apiKey,
            ObjectMapper objectMapper) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        log.info("GeminiService initialized with HttpClient approach.");
    }

    /**
     * InvestmentSummaryDto를 기반으로 Gemini AI로부터 투자 조언을 생성합니다.
     * 이 조언은 사용자별 맞춤형이며, 일일 시장 동향 분석과는 별개로 수행됩니다.
     *
     * @param summaryDto 사용자의 투자 요약 데이터
     * @return Gemini AI로부터 받은 점수와 조언이 포함된 AiAdviceDto
     */
    public AiAdviceDto generateInvestmentAdvice(InvestmentSummaryDto summaryDto) {
        try {
            // Gemini API에 보낼 프롬프트 생성
            String prompt = createPromptFromSummary(summaryDto);
            log.info("Gemini AI 프롬프트 생성 완료: {}", prompt.substring(0, Math.min(prompt.length(), 200)) + "..."); // 긴 프롬프트는 잘라서 로깅

            // API 요청 본문 생성 (responseSchema를 사용하여 structured output 요청)
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "responseSchema", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "score", Map.of("type", "INTEGER"), // score 필드는 INTEGER로 요청
                                            "advice", Map.of("type", "STRING")
                                    ),
                                    "propertyOrdering", List.of("score", "advice")
                            ),
                            "temperature", 0.8F,
                            "topK", 1.0F,
                            "topP", 1.0F,
                            "maxOutputTokens", 1000
                    )
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL + apiKey)) // apiKey를 URI에 포함
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Gemini API 응답 상태 코드: {}", response.statusCode());
            log.debug("Gemini API 응답 본문: {}", response.body());

            if (response.statusCode() == 200) {
                // 응답 파싱
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");

                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

                    if (parts != null && !parts.isEmpty()) {
                        String jsonString = (String) parts.get(0).get("text");
                        // JSON 문자열을 AiAdviceDto의 score와 advice 필드에 매핑
                        Map<String, Object> parsedAdvice = objectMapper.readValue(jsonString, Map.class);

                        // score는 JSON 스키마에서 INTEGER로 요청했으므로, Integer로 직접 받을 수 있도록 처리
                        Integer score = (Integer) parsedAdvice.get("score");
                        String advice = (String) parsedAdvice.get("advice");

                        // 점수 범위를 1~100으로 조정
                        if (score != null && (score < 1 || score > 100)) {
                            log.warn("AI generated score out of 1-100 range for investment advice. Score: {}. Adjusting to nearest valid boundary.", score);
                            score = Math.max(1, Math.min(100, score)); // 1-100 범위로 강제 조정
                        } else if (score == null) {
                            log.warn("Score가 null입니다. 기본값 0으로 설정.");
                            score = 0; // null일 경우 기본값 설정
                        }

                        AiAdviceDto adviceDto = AiAdviceDto.builder()
                                .userId(summaryDto.getUserId())
                                .seasonId(summaryDto.getSeasonId())
                                .score(score)
                                .advice(advice)
                                .createdAt(LocalDateTime.now())
                                .build();

                        log.info("[GeminiService] 최종 AiAdviceDto 생성 완료: score={}, advice.length={}",
                                adviceDto.getScore(), adviceDto.getAdvice() != null ? adviceDto.getAdvice().length() : 0);

                        return adviceDto;
                    }
                }
            } else {
                log.error("Gemini API 호출 실패. 상태 코드: {}, 응답: {}", response.statusCode(), response.body());
            }

        } catch (IOException | InterruptedException e) {
            log.error("Gemini API 호출 중 오류 발생: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 또는 처리 중 알 수 없는 오류 발생: {}", e.getMessage(), e);
        }

        // 오류 발생 시 기본 AiAdviceDto 반환
        log.warn("Gemini AI 조언 생성 실패. 기본값 반환.");
        return AiAdviceDto.builder()
                .userId(summaryDto.getUserId())
                .seasonId(summaryDto.getSeasonId())
                .score(0)
                .advice("AI 조언을 생성하는 데 실패했습니다. 다시 시도해 주세요.")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * InvestmentSummaryDto를 기반으로 Gemini AI에게 보낼 프롬프트 문자열을 생성합니다.
     * 이 프롬프트는 사용자의 투자 요약 정보를 상세하게 설명합니다.
     *
     * @param summary InvestmentSummaryDto 객체
     * @return Gemini AI에게 보낼 프롬프트 문자열
     */
    private String createPromptFromSummary(InvestmentSummaryDto summary) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("당신은 사용자에게 투자 조언을 제공하는 전문 AI 트레이딩 어드바이저입니다.\n");
        promptBuilder.append("다음은 사용자의 가상 투자 시즌 요약 데이터입니다. 이 데이터를 바탕으로 종합적인 투자 성과 점수(0-100점)와 구체적인 투자 조언을 한국어로 제공해주세요.\n");
        promptBuilder.append("응답은 반드시 JSON 형식으로 'score' (정수)와 'advice' (문자열) 필드를 포함해야 합니다. 다른 텍스트는 포함하지 마세요.\n\n");

        promptBuilder.append("--- 투자 요약 ---\n");
        promptBuilder.append("사용자: ").append(summary.getUserName()).append("\n");
        promptBuilder.append("시즌: ").append(summary.getSeasonName()).append(" (")
                .append(summary.getSeasonStartAt()).append(" ~ ").append(summary.getSeasonEndAt()).append(")\n");
        promptBuilder.append("초기 현금 잔고: ").append(summary.getInitialCashBalance()).append("원\n");
        promptBuilder.append("최종 총 자산: ").append(summary.getFinalTotalValue()).append("원\n");
        promptBuilder.append("총 손익 금액: ").append(summary.getTotalProfitLossAmount()).append("원\n");
        promptBuilder.append("총 수익률: ").append(String.format("%.2f", summary.getTotalProfitLossPercentage())).append("%%\n");
        promptBuilder.append("최종 랭크: ").append(summary.getFinalRank() != null ? summary.getFinalRank() + "위" : "N/A").append("\n");
        promptBuilder.append("총 거래 횟수: ").append(summary.getTotalTradeCount()).append("회 (매수: ")
                .append(summary.getBuyOrderCount()).append("회, 매도: ").append(summary.getSellOrderCount()).append("회)\n");
        promptBuilder.append("평균 거래 금액: ").append(String.format("%.2f", summary.getAvgTradeAmount())).append("원\n");
        promptBuilder.append("총 실현 손익: ").append(String.format("%.2f", summary.getTotalRealizedProfitLoss())).append("원\n");

        if (summary.getCoinRealizedProfitLoss() != null && !summary.getCoinRealizedProfitLoss().isEmpty()) {
            promptBuilder.append("코인별 실현 손익:\n");
            summary.getCoinRealizedProfitLoss().forEach((coin, pl) ->
                    promptBuilder.append("  - ").append(coin).append(": ").append(String.format("%.2f", pl)).append("원\n")
            );
        }
        if (summary.getCoinTradeCounts() != null && !summary.getCoinTradeCounts().isEmpty()) {
            promptBuilder.append("코인별 거래 횟수:\n");
            summary.getCoinTradeCounts().forEach((coin, count) ->
                    promptBuilder.append("  - ").append(coin).append(": ").append(count).append("회\n")
            );
        }

        promptBuilder.append("가장 많이 거래한 코인: ").append(summary.getMostTradedCoinSymbol() != null ? summary.getMostTradedCoinSymbol() : "N/A").append(" (거래 금액: ")
                .append(String.format("%.2f", summary.getMostTradedCoinTradeVolume())).append("원)\n");
        promptBuilder.append("가장 수익을 많이 낸 코인: ").append(summary.getHighestProfitCoinSymbol() != null ? summary.getHighestProfitCoinSymbol() : "N/A").append(" (수익 금액: ")
                .append(String.format("%.2f", summary.getHighestProfitCoinAmount())).append("원)\n");
        promptBuilder.append("가장 손실을 많이 낸 코인: ").append(summary.getLowestProfitCoinSymbol() != null ? summary.getLowestProfitCoinSymbol() : "N/A").append(" (손실 금액: ")
                .append(String.format("%.2f", summary.getLowestProfitCoinAmount())).append("원)\n");
        promptBuilder.append("하루 평균 거래 횟수: ").append(String.format("%.2f", summary.getAvgTradesPerDay())).append("회\n");
        promptBuilder.append("소수 코인 집중 투자 여부: ").append(summary.getFocusedOnFewCoins() ? "예" : "아니오").append("\n");

        // 주요 코인들의 시즌 내 시세 변동 요약 추가
        promptBuilder.append("\n--- 주요 코인 시장 성능 요약 (시즌 시작 시가 -> 시즌 종료 종가) ---\n");
        if (summary.getCoinMarketPerformanceSummary() != null && !summary.getCoinMarketPerformanceSummary().isEmpty()) {
            summary.getCoinMarketPerformanceSummary().forEach((symbol, performance) -> {
                promptBuilder.append(String.format("%s: %s\n", symbol, performance));
            });
        } else {
            promptBuilder.append("주요 코인 시장 성능 데이터 없음.\n");
        }


        // 일별 자산 잔고 추이 (투자 수동성 판단을 위해 사용)
        if (summary.getDailyBalanceTrend() != null && !summary.getDailyBalanceTrend().isEmpty()) {
            promptBuilder.append("\n--- 일별 자산 잔고 추이 (일부) ---\n");
            // 최신 7일치 또는 전체 데이터를 포함 (프롬프트 길이 제한 고려)
            List<UserDailyBalance> recentBalances = summary.getDailyBalanceTrend().stream()
                    .sorted(Comparator.comparing(UserDailyBalance::getSnapshotDate).reversed()) // 최신 날짜부터 정렬
                    .limit(7) // 최대 7개만 포함
                    .collect(Collectors.toList());

            for (UserDailyBalance balance : recentBalances) {
                promptBuilder.append(balance.getSnapshotDate()).append(": 총 자산 ")
                        .append(balance.getTotalValue()).append("원, 현금 잔고 ")
                        .append(balance.getCashBalance()).append("원\n");
            }
            if (summary.getDailyBalanceTrend().size() > 7) {
                promptBuilder.append("...(전체 ").append(summary.getDailyBalanceTrend().size()).append("일치 데이터 중 일부)\n");
            }
        }
        promptBuilder.append("\nAI 조언은 다음 형식으로 제공해주세요:\n");
        promptBuilder.append("```json\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"score\": [점수 (0-100)],\n");
        promptBuilder.append("  \"advice\": \"[구체적인 한국어 조언]\"\n");
        promptBuilder.append("}\n");
        promptBuilder.append("```\n");
        promptBuilder.append("이 외의 어떤 추가적인 설명이나 텍스트도 포함하지 마세요. 오직 JSON만 포함되어야 합니다.");

        return promptBuilder.toString();
    }
}
