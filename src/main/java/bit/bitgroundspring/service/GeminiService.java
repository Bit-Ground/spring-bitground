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
import com.google.genai.types.Type;
import bit.bitgroundspring.entity.Coin;
import bit.bitgroundspring.entity.AiInsight; // AiInsight 엔티티 임포트
import bit.bitgroundspring.repository.AiInsightRepository; // AiInsightRepository 임포트
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiService {

    private final String modelName;
    private final String apiKey;
    private final Client geminiClient;
    private final AiInsightRepository aiInsightRepository; // AiInsightRepository 주입
    private final ObjectMapper objectMapper;

    public GeminiService(
            @Value("${gemini.model-name}") String modelName,
            @Value("${gemini.api-key}") String apiKey,
            AiInsightRepository aiInsightRepository) {
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.aiInsightRepository = aiInsightRepository;
        this.objectMapper = new ObjectMapper();

        Client clientInstance = null;
        try {
            clientInstance = Client.builder().apiKey(apiKey).build();
            System.out.println("GeminiService initialized with model: " + modelName);
        } catch (Exception e) {
            System.err.println("Failed to initialize GeminiService: " + e.getMessage());
            e.printStackTrace();
        }
        this.geminiClient = clientInstance;
    }

    /**
     * Gemini 모델에 코인 분석 프롬프트를 보내고, JSON 형식의 응답을 파싱하여 DB에 저장합니다.
     * 이미 오늘 날짜로 해당 코인에 대한 AiInsight가 있다면 새로운 요청을 보내지 않고 기존 결과를 반환합니다.
     * @param coin Coin 엔티티 (코인 정보)
     * @return 저장된 AiInsight 객체 또는 null (분석 생성 또는 저장 실패 시)
     */
    public AiInsight generateAndSaveAnalysis(Coin coin) {
        if (geminiClient == null) {
            System.err.println("Gemini client is not initialized. Cannot generate analysis.");
            return null;
        }

        LocalDate today = LocalDate.now();

        // 1. 이미 오늘 날짜로 해당 코인에 대한 AiInsight가 있는지 확인 (중복 AI 호출 방지)
        Optional<AiInsight> existingInsight = aiInsightRepository.findBySymbolAndDate(coin.getSymbol(), today);
        if (existingInsight.isPresent()) {
            System.out.println("Returning existing AiInsight for " + coin.getSymbol() + " on " + today + ". ID: " + existingInsight.get().getId());
            return existingInsight.get();
        }

        // 2. Gemini AI에게 전달할 프롬프트를 JSON 형식으로 응답하도록 명시적으로 요청합니다.
        //    score는 -100 (매우 부정)에서 100 (매우 긍정) 사이의 정수로 요청합니다.
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
            // 3. 응답을 JSON 형식으로 받기 위한 GenerateContentConfig 설정
            Map<String, Schema> properties = new HashMap<>();
            properties.put("insight", Schema.builder().type(Type.Known.STRING).description("Analysis insight").build());
            properties.put("score", Schema.builder().type(Type.Known.INTEGER).description("Sentiment score (-100 to 100)").build());

            Schema responseSchema = Schema.builder()
                    .type("object")
                    .properties(properties)
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(responseSchema)
                    .candidateCount(1)
                    .build();

            Content content = Content.fromParts(Part.fromText(prompt));

            // 4. Gemini API 호출
            GenerateContentResponse response = geminiClient.models
                    .generateContent(modelName, content, config);

            if (response != null && response.text() != null && !response.text().isEmpty()) {
                String rawJson = response.text();
                System.out.println("Raw Gemini JSON response: " + rawJson);

                // 5. JSON 응답 파싱
                JsonNode rootNode = objectMapper.readTree(rawJson);
                String insightText = rootNode.path("insight").asText();
                Integer score = rootNode.path("score").asInt();

                // 6. 파싱된 데이터로 AiInsight 엔티티 생성 (symbol 필드 포함)
                AiInsight newInsight = AiInsight.builder()
                        .symbol(coin.getSymbol())
                        .date(today)
                        .insight(insightText)
                        .score(score)
                        .build();

                // 7. DB에 저장
                AiInsight savedInsight = aiInsightRepository.save(newInsight);
                System.out.println("Gemini analysis saved for " + coin.getSymbol() + ". Insight ID: " + savedInsight.getId());
                return savedInsight;

            } else {
                System.err.println("Gemini로부터 유효한 JSON 분석 내용을 생성할 수 없습니다. 응답이 비어있거나 유효하지 않습니다.");
                return null;
            }

        } catch (JsonProcessingException e) {
            System.err.println("Gemini 응답 JSON 파싱 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Gemini 분석 생성 및 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}