package bit.bitgroundspring.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController
@RequestMapping("/api/coin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CoinController {

    private final String UPBIT_API_URL = "https://api.upbit.com/v1";
    private final RestTemplate restTemplate;
    private final HttpHeaders headers;

    public CoinController() {
        this.restTemplate = new RestTemplate();
        this.headers = new HttpHeaders();
        this.headers.add("Accept", "application/json");
        this.headers.add("Accept-Encoding", "gzip"); // gzip 압축 지원
    }

    @GetMapping("/ticker")
    public ResponseEntity<?> getTicker(@RequestParam String markets) {
        try {
            // 파라미터 로깅
            System.out.println("Fetching ticker for markets: " + markets);
            // markets 파라미터 URL 인코딩
            String encodedMarkets = URLEncoder.encode(markets, StandardCharsets.UTF_8);
            String url = UPBIT_API_URL + "/ticker?markets=" + encodedMarkets;
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );
            return ResponseEntity.ok(Arrays.asList(response.getBody()));
        } catch (Exception e) {
            e.printStackTrace(); // 자세한 에러 로깅
            return ResponseEntity.badRequest().body("Failed to fetch ticker data: " + e.getMessage() + ", Cause: " + e.getCause());
        }
    }

    @GetMapping("/market/all")
    public ResponseEntity<?> getMarketCodes() {
        try {
            System.out.println("Fetching market codes");
            String url = UPBIT_API_URL + "/market/all";
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );
            return ResponseEntity.ok(Arrays.asList(response.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to fetch market codes: " + e.getMessage() + ", Cause: " + e.getCause());
        }
    }

    @GetMapping("/candles/minutes/1")
    public ResponseEntity<?> getMinuteCandles(
            @RequestParam String market,
            @RequestParam(defaultValue = "200") Integer count
    ) {
        try {
            // 파라미터 검증 및 로깅
            System.out.println("Fetching candles for market: " + market + ", count: " + count);
            if (count > 200) {
                return ResponseEntity.badRequest().body("Count cannot exceed 200");
            }
            // market 파라미터 URL 인코딩
            String encodedMarket = URLEncoder.encode(market, StandardCharsets.UTF_8);
            String url = UPBIT_API_URL + "/candles/minutes/1?market=" + encodedMarket + "&count=" + count;
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );
            return ResponseEntity.ok(Arrays.asList(response.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to fetch candle data: " + e.getMessage() + ", Cause: " + e.getCause());
        }
    }
}