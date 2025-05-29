package bit.bitgroundspring.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GbIndexService {
    public Map<String, Map<String, Double>> calculateGbIndices() {
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
        Map<String, Double> prevPrices = new HashMap<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String market = obj.getString("market");
            nowPrices.put(market, obj.getDouble("trade_price"));
            prevPrices.put(market, obj.getDouble("prev_closing_price"));
        }

        // 현재 기준 지수
        double gbmi = 0.6 * nowPrices.get("KRW-BTC") + 0.4 * nowPrices.get("KRW-ETH");
        double gbai = (
                nowPrices.get("KRW-XRP") + nowPrices.get("KRW-ADA") + nowPrices.get("KRW-DOGE") +
                        nowPrices.get("KRW-SAND") + nowPrices.get("KRW-MANA") + nowPrices.get("KRW-ATOM") +
                        nowPrices.get("KRW-AXS") + nowPrices.get("KRW-STX")
        ) / 8;

        // 전일 기준 지수
        double gbmiPrev = 0.6 * prevPrices.get("KRW-BTC") + 0.4 * prevPrices.get("KRW-ETH");
        double gbaiPrev = (
                prevPrices.get("KRW-XRP") + prevPrices.get("KRW-ADA") + prevPrices.get("KRW-DOGE") +
                        prevPrices.get("KRW-SAND") + prevPrices.get("KRW-MANA") + prevPrices.get("KRW-ATOM") +
                        prevPrices.get("KRW-AXS") + prevPrices.get("KRW-STX")
        ) / 8;

        Map<String, Map<String, Double>> result = new HashMap<>();
        result.put("current", Map.of("GBMI", gbmi, "GBAI", gbai));
        result.put("previous", Map.of("GBMI", gbmiPrev, "GBAI", gbaiPrev));
        return result;
    }
}
