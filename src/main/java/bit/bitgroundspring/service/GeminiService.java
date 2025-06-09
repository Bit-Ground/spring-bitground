// src/main/java/bit/bitgroundspring/service/GeminiService.java

package bit.bitgroundspring.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type; // Type 임포트 유지
import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.entity.AiInsight;
import bit.bitgroundspring.repository.AiInsightRepository;
import bit.bitgroundspring.repository.CoinRepository; // CoinRepository 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger; // SLF4J Logger 임포트
import org.slf4j.LoggerFactory; // SLF4J LoggerFactory 임포트
import org.springframework.scheduling.annotation.Scheduled; // @Scheduled 임포트

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap; // Map.of 대신 HashMap 사용 (Java 8 호환성을 위해)
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiService {

    // SLF4J Logger 인스턴스 선언
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final String modelName;
    private final String apiKey;
    private final Client geminiClient;
    private final AiInsightRepository aiInsightRepository;
    private final CoinRepository coinRepository; // CoinRepository 주입 추가
    private final ObjectMapper objectMapper;

    // 전체 시장 분석을 위한 심볼 상수 정의 (프롬프트와 일치시켜야 함)
    public static final String MARKET_OVERALL_SYMBOL = "MARKET_OVERALL";

    public GeminiService(
            @Value("${gemini.model-name}") String modelName,
            @Value("${gemini.api-key}") String apiKey,
            AiInsightRepository aiInsightRepository,
            CoinRepository coinRepository, // 주입받도록 추가
            ObjectMapper objectMapper) { // ObjectMapper도 주입받도록 변경 (스프링 관리)
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.aiInsightRepository = aiInsightRepository;
        this.coinRepository = coinRepository; // 초기화
        this.objectMapper = objectMapper; // 초기화

        Client clientInstance = null;
        try {
            clientInstance = Client.builder().apiKey(apiKey).build();
            log.info("GeminiService initialized with model: {}", modelName); // System.out.println -> log.info
        } catch (Exception e) {
            log.error("Failed to initialize GeminiService: {}", e.getMessage(), e); // System.err.println + e.printStackTrace -> log.error
        }
        this.geminiClient = clientInstance;
    }

    /**
     * 특정 심볼에 대한 오늘자 AI 분석 결과를 조회합니다.
     * 만약 없다면 null을 반환합니다. 이 메서드는 API 요청을 직접 트리거하지 않습니다.
     * 스케줄러에 의해 미리 생성된 데이터를 조회하는 용도입니다.
     *
     * @param symbol 조회할 코인 또는 시장의 심볼
     * @return AiInsight 객체 (존재할 경우) 또는 null
     */
    public AiInsight getTodayInsight(String symbol) {
        return aiInsightRepository.findBySymbolAndDate(symbol, LocalDate.now()).orElse(null);
    }

    /**
     * Gemini 모델에 코인 분석 프롬프트를 보내고, JSON 형식의 응답을 파싱하여 DB에 저장합니다.
     * 이미 오늘 날짜로 해당 코인에 대한 AiInsight가 있다면 새로운 요청을 보내지 않고 기존 결과를 반환합니다.
     * @param coin Coin 엔티티 (코인 정보)
     * @return 저장된 AiInsight 객체 또는 null (분석 생성 또는 저장 실패 시)
     */
    public AiInsight generateAndSaveAnalysis(Coin coin) {
        if (geminiClient == null) {
            log.error("Gemini client is not initialized. Cannot generate analysis."); // System.err.println -> log.error
            return null;
        }

        LocalDate today = LocalDate.now();

        Optional<AiInsight> existingInsight = aiInsightRepository.findBySymbolAndDate(coin.getSymbol(), today);
        if (existingInsight.isPresent()) {
            log.info("Returning existing AiInsight for {} on {}. ID: {}", coin.getSymbol(), today, existingInsight.get().getId()); // System.out.println -> log.info
            return existingInsight.get();
        }

        String prompt = String.format(
                "암호화폐 '%s'(%s)에 대해 분석해 주세요. " +
                        "다음 데이터가 포함되도록 해주세요: 24시간 거래대금: %,d 원, 24시간 변동률: %.2f%%. " +
                        "해당 코인이 현재 투자유의 종목인지 (isCaution: %b), 투자주의 종목인지 (isWarning: %b) 여부를 언급하며, " +
                        "이러한 상태가 투자에 어떤 의미를 가지는지 함께 설명해주세요. " +
                        "최신 시장 동향과 투자 시 유의할 점에 대해 간략하게 요약해 주세요. " +
                        "응답은 다음 JSON 형식으로만 제공해 주세요: " +
                        "{\"insight\": \"여기 분석 내용을 입력하세요 (최대 500자 이내)\", \"score\": \"여기 -100에서 100 사이의 정수 점수를 입력하세요\"}. " +
                        "score는 -100 (매우 부정적), 0 (중립), 100 (매우 긍정적)입니다. " +
                        "다른 설명이나 추가적인 문구 없이 JSON 객체만 반환해주세요.",
                coin.getKoreanName(),
                coin.getSymbol(),
                coin.getTradePrice24h(),
                coin.getChangeRate() * 100,
                coin.getIsCaution(),
                coin.getIsWarning()
        );

        try {
            Map<String, Schema> properties = new HashMap<>();
            properties.put("insight", Schema.builder().type(Type.Known.STRING).description("Analysis insight").build());
            properties.put("score", Schema.builder().type(Type.Known.INTEGER).description("Sentiment score (-100 to 100)").build());

            Schema responseSchema = Schema.builder()
                    .type(Type.Known.OBJECT) // "object" 대신 Type.Known.OBJECT 사용
                    .properties(properties)
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(responseSchema)
                    .candidateCount(1)
                    .build();

            Content content = Content.fromParts(Part.fromText(prompt));

            GenerateContentResponse response = geminiClient.models
                    .generateContent(modelName, content, config);

            if (response != null && response.text() != null && !response.text().isEmpty()) {
                String rawJson = response.text();
                log.info("Raw Gemini JSON response for {}: {}", coin.getSymbol(), rawJson); // System.out.println -> log.info

                JsonNode rootNode = objectMapper.readTree(rawJson);
                String insightText = rootNode.path("insight").asText();
                Integer score = rootNode.path("score").asInt();

                AiInsight newInsight = AiInsight.builder()
                        .symbol(coin.getSymbol())
                        .date(today)
                        .insight(insightText)
                        .score(score)
                        .build();

                AiInsight savedInsight = aiInsightRepository.save(newInsight);
                log.info("Gemini analysis saved for {}. Insight ID: {}", coin.getSymbol(), savedInsight.getId()); // System.out.println -> log.info
                return savedInsight;

            } else {
                log.error("Gemini로부터 유효한 JSON 분석 내용을 생성할 수 없습니다. 응답이 비어있거나 유효하지 않습니다. 코인: {}", coin.getSymbol()); // System.err.println -> log.error
                return null;
            }

        } catch (JsonProcessingException e) {
            log.error("Gemini 응답 JSON 파싱 중 오류 발생 (코인: {}): {}", coin.getSymbol(), e.getMessage(), e); // System.err.println + e.printStackTrace -> log.error
            return null;
        } catch (Exception e) {
            log.error("Gemini 분석 생성 및 저장 중 오류 발생 (코인: {}): {}", coin.getSymbol(), e.getMessage(), e); // System.err.println + e.printStackTrace -> log.error
            return null;
        }
    }


    /**
     * 여러 코인 및 전체 시장에 대한 AI 분석을 단일 Gemini API 호출로 생성하고 저장합니다.
     * 이 메서드는 스케줄러에 의해 하루 한 번 호출됩니다.
     *
     * @param targetCoinSymbols AI 분석을 요청할 개별 코인 심볼 리스트 (예: KRW-ETH, KRW-SOL)
     * @return 저장된 AiInsight 객체 리스트
     */
    public List<AiInsight> generateAndSaveBatchAnalysis(List<String> targetCoinSymbols) {
        if (geminiClient == null) {
            log.error("Gemini client is not initialized. Cannot generate batch analysis."); // System.err.println -> log.error
            return new ArrayList<>();
        }

        LocalDate today = LocalDate.now();
        List<AiInsight> savedInsights = new ArrayList<>();

        // 이미 오늘자 분석이 완료되었는지 확인 (MARKET_OVERALL 기준으로)
        if (aiInsightRepository.findBySymbolAndDate(MARKET_OVERALL_SYMBOL, today).isPresent()) {
            log.info("Market and individual coin analysis already performed for today. Skipping batch analysis."); // System.out.println -> log.info
            return savedInsights; // 빈 리스트 반환 또는 이미 저장된 내용 조회 후 반환
        }

        // 프롬프트 구성 (전체 시장 + 개별 코인 + AI의 자유 추가 코인)
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("[지시사항: 너는 전문적인 암호화폐 시장 분석가이다. 사용자에게 포괄적이고 통찰력 있는 일일 시장 동향 분석을 제공하라.]\n\n");
        promptBuilder.append("[역할 및 데이터 전제]:\n");
        promptBuilder.append("- 너는 최신 암호화폐 시장 데이터 (가격, 거래량, 시가총액 등), 주요 뉴스, 거시 경제 지표 및 차트 정보를 종합적으로 분석할 수 있다고 가정한다.\n");
        promptBuilder.append("- 너의 분석은 투자 조언이 아니며, 정보 제공 목적임을 명심하라.\n\n");

        // 첫 번째 요구사항: 전체 시장 분석
        promptBuilder.append("[첫 번째 요구사항: 전체 암호화폐 시장에 대한 종합적인 분석을 제공하라.]\n");
        promptBuilder.append("- 현재 시장 감성 (강세, 약세, 중립/혼조세) 및 그 감성을 뒷받침하는 주요 요인들.\n");
        promptBuilder.append("- 시장을 움직이는 핵심 동인 (예: 비트코인 현물 ETF 유입/유출, 미국 연준의 통화 정책, 글로벌 경제 상황, 주요 규제 발표, 기관 투자 동향 등).\n");
        promptBuilder.append("- 비트코인의 시장 지배력(도미넌스) 변화와 이것이 전체 알트코인 시장에 미치는 영향.\n");
        promptBuilder.append("- 전체 알트코인 시장의 요약된 동향 (예: 특정 섹터의 강세, 전반적인 BTC 추종 여부).\n");
        promptBuilder.append("- 단기 (향후 24-48시간) 전망 및 이 기간 동안 주의해야 할 주요 리스크 요인 (예: 중요한 경제 지표 발표, 주요 회의, 기술적 저항/지지선).\n");
        promptBuilder.append("- 이 분석에 대해 1 (매우 부정적/강한 약세)부터 100 (매우 긍정적/강한 강세)까지의 **종합 점수**를 부여하라.\n");
        promptBuilder.append("- 이 분석 결과의 'symbol'은 \"").append(MARKET_OVERALL_SYMBOL).append("\"로 설정하라.\n\n");

        // 두 번째 요구사항: 개별 코인 분석
        promptBuilder.append("[두 번째 요구사항: 다음 개별 코인들에 대해 간결하고 전문적인 분석을 제공하라.]\n");
        promptBuilder.append("- 각 코인의 현재 시장 동향, 최근 뉴스(긍정적/부정적), 그리고 단기 (향후 24-48시간) 전망에 초점을 맞춰라.\n");
        promptBuilder.append("- 각 코인 분석에 대해 1 (매우 부정적)부터 100 (매우 긍정적)까지의 **개별 감성 점수**를 부여하라.\n");
        promptBuilder.append("- 각 분석의 'symbol'은 해당 코인의 심볼(예: KRW-ETH)로 설정하라.\n\n");

        promptBuilder.append("[요청 코인 목록:]\n");
        List<Coin> selectedCoins = coinRepository.findAllBySymbolIn(targetCoinSymbols);
        for (Coin coin : selectedCoins) {
            promptBuilder.append(String.format("- %s (%s)\n", coin.getKoreanName(), coin.getSymbol()));
        }
        promptBuilder.append("\n[추가 요구사항: 위 요청 목록 외에, 업비트에 상장되어 있는 코인 중 오늘자 또는 현재 시장 트렌드 기준으로 거래자들이 특별히 관심 가질 만하거나, 관심 가져야 할 필요가 있는 코인(최대 2개)이 있다면, 그 코인들에 대한 동향 분석을 위와 동일한 형식으로 추가하라.]\n");
        promptBuilder.append("- 추가 코인 선정 시, 최근 거래량 급증, 주요 기술 업데이트, 대형 파트너십 발표, 특정 섹터의 급부상, 혹은 높은 변동성 등의 요인을 고려하라.\n");
        promptBuilder.append("- 추가되는 코인 또한 반드시 아래 JSON 객체 배열 형식의 규칙을 따라야 한다.\n\n");

        // 출력 형식
        promptBuilder.append("[출력 형식: 모든 분석 결과는 아래와 같은 JSON 객체의 배열 형태로만 제공되어야 한다.]\n");
        promptBuilder.append("```json\n");
        promptBuilder.append("[\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("    \"symbol\": \"").append(MARKET_OVERALL_SYMBOL).append("\",\n");
        promptBuilder.append("    \"insight\": \"여기에 전체 시장 분석 텍스트를 작성합니다. (최대 500자 이내)\",\n");
        promptBuilder.append("    \"score\": 75\n");
        promptBuilder.append("  },\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("    \"symbol\": \"KRW-ETH\",\n");
        promptBuilder.append("    \"insight\": \"이더리움 개별 분석 텍스트를 작성합니다. (최대 500자 이내)\",\n");
        promptBuilder.append("    \"score\": 82\n");
        promptBuilder.append("  }\n");
        promptBuilder.append("  // ... (다른 개별 코인들의 분석 및 AI가 추가한 코인 분석)\n");
        promptBuilder.append("]\n");
        promptBuilder.append("```\n");
        promptBuilder.append("[최종 제약 사항: 각 'insight' 필드의 텍스트는 최대 500자(한글 포함)를 넘지 않아야 한다. 출력은 오직 위 JSON 형식의 배열이어야 하며, 다른 서론/결론 텍스트는 포함하지 않는다.]");


        log.debug("Constructed batch prompt:\n{}", promptBuilder.toString()); // System.out.println -> log.debug

        try {
            // Gemini에게 JSON 배열 응답을 요청하는 스키마
            Map<String, Schema> itemProperties = new HashMap<>();
            itemProperties.put("symbol", Schema.builder().type(Type.Known.STRING).build());
            itemProperties.put("insight", Schema.builder().type(Type.Known.STRING).build());
            itemProperties.put("score", Schema.builder().type(Type.Known.INTEGER).build()); // 점수는 정수

            Schema arrayItemSchema = Schema.builder()
                    .type(Type.Known.OBJECT) // 배열의 각 항목은 객체
                    .properties(itemProperties)
                    .required(List.of("symbol", "insight", "score"))
                    .build();

            Schema responseSchema = Schema.builder()
                    .type(Type.Known.ARRAY) // 전체 응답은 배열
                    .items(arrayItemSchema) // 배열의 각 항목 스키마 지정
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(responseSchema)
                    .candidateCount(1) // 하나의 응답만 받도록 설정
                    .build();

            Content content = Content.fromParts(Part.fromText(promptBuilder.toString()));

            GenerateContentResponse response = geminiClient.models
                    .generateContent(modelName, content, config);
            String rawJson = response.text();

            if (rawJson != null && !rawJson.trim().isEmpty()) {
                JsonNode rootNode = objectMapper.readTree(rawJson);

                if (rootNode.isArray()) {
                    for (JsonNode node : rootNode) {
                        String symbol = node.path("symbol").asText();
                        String insightText = node.path("insight").asText();
                        Integer score = node.path("score").asInt();

                        // 데이터 유효성 검사 및 점수 범위 조정
                        if (symbol.isEmpty() || insightText.isEmpty() || score == 0) {
                            log.error("Skipping incomplete or invalid AI insight from batch response for symbol: {} | Raw node: {}", symbol, node.toString()); // System.err.println -> log.error
                            continue;
                        }
                        if (score < 1 || score > 100) { // 점수 범위 1-100 검증
                            log.warn("AI generated score out of 1-100 range for symbol: {}. Score: {}. Adjusting to nearest valid boundary.", symbol, score); // System.err.println -> log.warn
                            score = Math.max(1, Math.min(100, score)); // 1-100 범위로 강제 조정
                        }

                        // 중복 분석 방지 (오늘 날짜의 동일 심볼 분석이 이미 있는지 확인)
                        Optional<AiInsight> existingInsight = aiInsightRepository.findBySymbolAndDate(symbol, today);
                        if (existingInsight.isPresent()) {
                            log.info("   Insight for {} on {} already exists. Skipping save.", symbol, today); // System.out.println -> log.info
                            continue;
                        }

                        AiInsight newInsight = AiInsight.builder()
                                .symbol(symbol)
                                .date(today)
                                .insight(insightText)
                                .score(score)
                                .build();

                        AiInsight savedInsight = aiInsightRepository.save(newInsight);
                        savedInsights.add(savedInsight);
                        log.info("   Batch analysis saved for {}. Insight ID: {}", symbol, savedInsight.getId()); // System.out.println -> log.info
                    }
                } else {
                    log.error("Gemini 응답이 예상한 JSON 배열 형태가 아닙니다: {}", rawJson); // System.err.println -> log.error
                }
            } else {
                log.error("Gemini로부터 유효한 JSON 분석 내용을 생성할 수 없습니다. 응답이 비어있거나 유효하지 않습니다."); // System.err.println -> log.error
            }
        } catch (JsonProcessingException e) {
            log.error("Gemini 응답 JSON 파싱 중 오류 발생: {}", e.getMessage(), e); // System.err.println + e.printStackTrace -> log.error
        } catch (Exception e) {
            log.error("Gemini 배치 분석 생성 및 저장 중 오류 발생: {}", e.getMessage(), e); // System.err.println + e.printStackTrace -> log.error
        }
        return savedInsights;
    }

    /**
     * 스케줄러: 매일 자정에 모든 코인과 전체 시장에 대한 AI 분석을 일괄 생성하여 저장합니다.
     * 이 스케줄러는 `CoinScheduler`가 아닌 `GeminiService` 자체에 존재해야 합니다.
     * (이전 코드에서 제공해주셨던 generateAndSaveBatchAnalysis를 호출하도록 변경)
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정 (00시 00분 00초)
    public void generateAndSaveAllDailyInsights() {
        log.info("Batch AI analysis scheduled task initiated for all coins and overall market.");
        List<String> allCoinSymbols = coinRepository.findAll().stream()
                .map(Coin::getSymbol)
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll); // Collectors.toList() 대신 대체

        // 기존 generateAndSaveBatchAnalysis 메서드가 여러 코인과 전체 시장을 한 번에 처리하도록 되어 있으므로,
        // 이를 호출하여 모든 분석을 수행합니다.
        generateAndSaveBatchAnalysis(allCoinSymbols);
        log.info("Batch AI analysis scheduled task completed.");
    }
}