// src/main/java/bit/bitgroundspring/service/GeminiService.java

package bit.bitgroundspring.service;

import bit.bitgroundspring.dto.AiAdviceDto;
import bit.bitgroundspring.dto.InvestmentSummaryDto;
import bit.bitgroundspring.entity.AiInsight;
import bit.bitgroundspring.repository.AiInsightRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final String modelName;
    private final String apiKey;
    private final Client geminiClient;
    private final AiInsightRepository aiInsightRepository; // AI 인사이트 조회용으로 유지
    private final ObjectMapper objectMapper;

    public GeminiService(
            @Value("${gemini.model-name}") String modelName,
            @Value("${gemini.api-key}") String apiKey,
            AiInsightRepository aiInsightRepository,
            ObjectMapper objectMapper) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.aiInsightRepository = aiInsightRepository;
        this.objectMapper = objectMapper;

        Client clientInstance = null;
        try {
            clientInstance = Client.builder().apiKey(apiKey).build();
            log.info("GeminiService initialized with model: {}", modelName);
        } catch (Exception e) {
            log.error("Failed to initialize GeminiService: {}", e.getMessage(), e);
        }
        this.geminiClient = clientInstance;
    }

    /**
     * 특정 심볼에 대한 오늘자 AI 분석 결과를 조회합니다.
     * 이 메서드는 Go 서비스에 의해 미리 생성되어 DB에 저장된 데이터를 조회하는 용도입니다.
     * API 요청을 직접 트리거하지 않습니다.
     *
     * @param symbol 조회할 코인 또는 시장의 심볼
     * @return AiInsight 객체 (존재할 경우) 또는 null
     */
    public AiInsight getTodayInsight(String symbol) {
        // Go 서비스가 이미 AI 인사이트를 생성하여 DB에 저장한다고 가정
        return aiInsightRepository.findBySymbolAndDate(symbol, LocalDate.now()).orElse(null);
    }

    /**
     * InvestmentSummaryDto를 기반으로 Gemini AI로부터 투자 조언을 생성합니다.
     * 이 조언은 사용자별 맞춤형이며, 일일 시장 동향 분석과는 별개로 수행됩니다.
     *
     * @param summaryDto 사용자의 투자 요약 데이터
     * @return Gemini AI로부터 받은 점수와 조언이 포함된 AiAdviceDto
     */
    public AiAdviceDto generateInvestmentAdvice(InvestmentSummaryDto summaryDto) {
        if (geminiClient == null) {
            log.error("Gemini Client가 초기화되지 않았습니다. AI 조언을 수행할 수 없습니다.");
            return null;
        }

        // --- Gemini 프롬프트 구성 ---
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format("당신은 전문적인 코인 모의 투자 분석가입니다. 다음은 사용자 '%s'의 지난 시즌(%s ~ %s) 투자 요약 데이터입니다. 이 데이터를 기반으로 투자 성과를 점수(1-100점)로 평가하고, 강점, 개선점, 그리고 다음 시즌을 위한 구체적이고 합리적인 투자 조언(500자 이내)을 한국어로 제공해 주세요. 조언은 친절하고 전문적인 어조로 작성해 주세요. JSON 형식으로만 응답하며, 'score'(점수)와 'advice'(조언 내용) 필드를 포함해야 합니다. (예: {\"score\": 85, \"advice\": \"내용\"})\n\n",
                summaryDto.getUserName(), summaryDto.getSeasonStartAt(), summaryDto.getSeasonEndAt()));

        promptBuilder.append(String.format("--- 시즌 성과 요약 ---\n"));
        promptBuilder.append(String.format("초기 자본: %.0f KRW\n", summaryDto.getInitialCashBalance()));
        promptBuilder.append(String.format("최종 자산: %.0f KRW\n", summaryDto.getFinalTotalValue()));
        promptBuilder.append(String.format("총 손익: %.0f KRW (%.2f%%)\n", summaryDto.getTotalProfitLossAmount(), summaryDto.getTotalProfitLossPercentage()));
        if (summaryDto.getFinalRank() != null) {
            promptBuilder.append(String.format("최종 랭킹: %d위\n", summaryDto.getFinalRank()));
        }

        promptBuilder.append(String.format("\n--- 거래 활동 요약 ---\n"));
        promptBuilder.append(String.format("총 거래 횟수: %d회 (매수 %d회, 매도 %d회)\n",
                summaryDto.getTotalTradeCount(), summaryDto.getBuyOrderCount(), summaryDto.getSellOrderCount()));
        promptBuilder.append(String.format("평균 거래 금액: %.0f KRW\n", summaryDto.getAvgTradeAmount()));
        promptBuilder.append(String.format("총 실현 손익 (단순 합계): %.0f KRW\n", summaryDto.getTotalRealizedProfitLoss()));

        if (summaryDto.getCoinRealizedProfitLoss() != null && !summaryDto.getCoinRealizedProfitLoss().isEmpty()) {
            promptBuilder.append("코인별 실현 손익:\n");
            // Map<String, Float> 이므로 forEach는 Map.Entry를 사용해야 합니다.
            summaryDto.getCoinRealizedProfitLoss().forEach((symbol, pl) ->
                    promptBuilder.append(String.format("  - %s: %.0f KRW\n", symbol, pl)));
        }
        if (summaryDto.getCoinTradeCounts() != null && !summaryDto.getCoinTradeCounts().isEmpty()) {
            promptBuilder.append("코인별 거래 횟수:\n");
            summaryDto.getCoinTradeCounts().forEach((symbol, count) ->
                    promptBuilder.append(String.format("  - %s: %d회\n", symbol, count)));
        }

        promptBuilder.append(String.format("\n--- 투자 성향 지표 ---\n"));
        if (summaryDto.getMostTradedCoinSymbol() != null) {
            promptBuilder.append(String.format("가장 많이 거래한 코인: %s (총 거래 금액: %.0f KRW)\n",
                    summaryDto.getMostTradedCoinSymbol(), summaryDto.getMostTradedCoinTradeVolume()));
        }
        if (summaryDto.getHighestProfitCoinSymbol() != null) {
            promptBuilder.append(String.format("가장 수익을 많이 낸 코인: %s (수익 금액: %.0f KRW)\n",
                    summaryDto.getHighestProfitCoinSymbol(), summaryDto.getHighestProfitCoinAmount()));
        }
        if (summaryDto.getLowestProfitCoinSymbol() != null) {
            promptBuilder.append(String.format("가장 손실을 많이 낸 코인: %s (손실 금액: %.0f KRW)\n",
                    summaryDto.getLowestProfitCoinSymbol(), summaryDto.getLowestProfitCoinAmount()));
        }
        if (summaryDto.getAvgTradesPerDay() != null) {
            promptBuilder.append(String.format("하루 평균 거래 횟수: %.2f회\n", summaryDto.getAvgTradesPerDay()));
        }
        if (summaryDto.getFocusedOnFewCoins() != null) {
            promptBuilder.append(String.format("소수 코인 집중 투자 여부: %s\n", summaryDto.getFocusedOnFewCoins() ? "예" : "아니오"));
        }

        String prompt = promptBuilder.toString();
        log.info("Gemini AI 조언 생성 프롬프트:\n{}", prompt);

        // --- Gemini API 호출 설정 ---
        Map<String, Schema> adviceProperties = new HashMap<>();
        adviceProperties.put("score", Schema.builder().type(Type.Known.INTEGER).build());
        adviceProperties.put("advice", Schema.builder().type(Type.Known.STRING).build());

        Schema responseSchema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(adviceProperties)
                .required(List.of("score", "advice"))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(responseSchema)
                .temperature(0.8F)
                .topK(1.0F)
                .topP(1.0F)
                .maxOutputTokens(1000)
                .build();

        GenerateContentResponse response = null;
        try {
            response = geminiClient.models
                    .generateContent(modelName, Content.fromParts(Part.fromText(prompt)), config);
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생 (AI 조언 생성): {}", e.getMessage(), e);
            return null;
        }

        // --- Gemini 응답 파싱 ---
        if (response != null && response.text() != null && !response.text().isEmpty()) {
            try {
                String jsonResponseText = response.text();
                log.info("Gemini로부터 받은 AI 조언 원본 JSON: {}", jsonResponseText);

                JsonNode rootNode = objectMapper.readTree(jsonResponseText);
                int score = rootNode.path("score").asInt(0);
                String advice = rootNode.path("advice").asText("");

                // 점수 범위를 1~100으로 조정
                if (score < 1 || score > 100) {
                    log.warn("AI generated score out of 1-100 range for investment advice. Score: {}. Adjusting to nearest valid boundary.", score);
                    score = Math.max(1, Math.min(100, score)); // 1-100 범위로 강제 조정
                }

                return AiAdviceDto.builder()
                        .score(score)
                        .advice(advice)
                        .build();

            } catch (JsonProcessingException e) {
                log.error("Gemini 응답 JSON 파싱 중 오류 발생: {}", e.getMessage(), e);
                return null;
            }
        } else {
            log.error("Gemini로부터 유효한 AI 조언 내용을 생성할 수 없습니다. 응답이 비어있거나 유효하지 않습니다.");
            return null;
        }
    }
}
