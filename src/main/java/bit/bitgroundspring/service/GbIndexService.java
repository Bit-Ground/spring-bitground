package bit.bitgroundspring.service;

import bit.bitgroundspring.entity.GbIndexHistory;
import bit.bitgroundspring.repository.GbIndexHistoryRepository;
import jakarta.transaction.Transactional;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class GbIndexService {
    private final GbIndexHistoryRepository historyRepository;

    public GbIndexService(GbIndexHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    // 소수점 2자리 반올림 함수
    private double roundToTwoDecimalPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Transactional
    public void saveGbIndexToDbAt(LocalDateTime timestamp) {
        Map<String, Double> current = calculateGbIndices();

        GbIndexHistory history = GbIndexHistory.builder()
                .timestamp(timestamp) // 무조건 정각 기준으로 저장
                .gbmi(current.get("GBMI"))
                .gbai(current.get("GBAI"))
                .build();

        historyRepository.save(history);
    }

    @Transactional
    public Map<String, Double> calculateGbIndices() {
        String[] marketList = {
                "KRW-BTC", "KRW-ETH",
                "KRW-XRP", "KRW-ADA", "KRW-DOGE", "KRW-SAND",
                "KRW-MANA", "KRW-ATOM", "KRW-AXS", "KRW-STX"
        };
        String marketQuery = String.join(",", marketList);
        String url = "https://api.upbit.com/v1/ticker?markets=" + marketQuery;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JSONArray jsonArray = new JSONArray(response.getBody());

        Map<String, Double> nowPrices = new HashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            nowPrices.put(obj.getString("market"), obj.getDouble("trade_price"));
        }

        // Raw 계산
        double gbmiRaw = 0.6 * nowPrices.get("KRW-BTC") + 0.4 * nowPrices.get("KRW-ETH");
        double gbaiRaw = (
                nowPrices.get("KRW-XRP") + nowPrices.get("KRW-ADA") + nowPrices.get("KRW-DOGE") +
                        nowPrices.get("KRW-SAND") + nowPrices.get("KRW-MANA") + nowPrices.get("KRW-ATOM") +
                        nowPrices.get("KRW-AXS") + nowPrices.get("KRW-STX")
        ) / 8;

        // 표준화
        double gbmi = roundToTwoDecimalPlaces((gbmiRaw / 91000000.0) * 20000.0);
        double gbai = roundToTwoDecimalPlaces((gbaiRaw / 2160.0) * 8500.0);

        // 반올림 적용
        gbmi = roundToTwoDecimalPlaces(gbmi);
        gbai = roundToTwoDecimalPlaces(gbai);

        return Map.of("GBMI", gbmi, "GBAI", gbai);
    }

}

