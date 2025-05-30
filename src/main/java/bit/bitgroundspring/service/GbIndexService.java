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
import java.util.Map;

@Service
public class GbIndexService {
    private final GbIndexHistoryRepository historyRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public GbIndexService(GbIndexHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Transactional
    public void saveGbIndexToDbAt(LocalDateTime timestamp) {
        Map<String, Double> indices = calculateGbIndices();

        GbIndexHistory history = GbIndexHistory.builder()
                .timestamp(timestamp)
                .gbmi(indices.get("GBMI"))
                .gbai(indices.get("GBAI"))
                .build();

        historyRepository.save(history);
    }

    @Transactional
    public Map<String, Double> calculateGbIndices() {
        String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=krw&order=market_cap_desc&per_page=12&page=1";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JSONArray jsonArray = new JSONArray(response.getBody());

        double gbmiNumerator = 0.0;
        double gbmiDenominator = 0.0;
        double gbaiNumerator = 0.0;
        double gbaiDenominator = 0.0;

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject coin = jsonArray.getJSONObject(i);
            String id = coin.getString("id");
            String name = coin.getString("name");
            double price = coin.getDouble("current_price");
            double marketCap = coin.getDouble("market_cap");

            // GBMI: 모든 12개 코인
            gbmiNumerator += price * marketCap;
            gbmiDenominator += marketCap;

            // GBAI: 비트코인 제외
            if (!id.equals("bitcoin")) {
                gbaiNumerator += price * marketCap;
                gbaiDenominator += marketCap;
            }
        }

        // 표준화 계수: 예시값. 조정 가능
        double gbmiRaw = gbmiNumerator / gbmiDenominator;
        double gbaiRaw = gbaiNumerator / gbaiDenominator;

        double gbmi = round(gbmiRaw / 20000.0);
        double gbai = round(gbaiRaw / 1000.0);

        return Map.of("GBMI", gbmi, "GBAI", gbai);
    }
}

