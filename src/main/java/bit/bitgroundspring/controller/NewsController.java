package bit.bitgroundspring.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NewsController {

    @GetMapping("/news")
    public ResponseEntity<List<Map<String, Object>>> getNews(
        @RequestParam("keyword") String keyword,
        @RequestParam(value ="display", defaultValue = "10") int display,
        @RequestParam(value="start", defaultValue = "1") int start
    ) {
        try {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(keyword);
            String encodedKeyword = StandardCharsets.UTF_8.decode(buffer).toString();

            URI uri = UriComponentsBuilder
                    .fromUriString("https://openapi.naver.com")
                    .path("/v1/search/news.json")
                    .queryParam("query", encodedKeyword)
                    .queryParam("display", display)
                    .queryParam("start", start)
                    .queryParam("sort", "date")
                    .build()
                    .toUri();

            RestTemplate restTemplate = new RestTemplate();
            RequestEntity<Void> req = RequestEntity
                    .get(uri)
                    .header("X-Naver-Client-Id", "Czd_lY45xNZVMbwiOqTF")
                    .header("X-Naver-Client-Secret", "aQxk119Y2b")
                    .build();

            ResponseEntity<String> result = restTemplate.exchange(req, String.class);

            JSONObject json = new JSONObject(result.getBody());
            JSONArray items = json.getJSONArray("items");

            List<Map<String, Object>> list = new ArrayList<>();

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                Map<String, Object> news = new HashMap<>();
                news.put("title", item.getString("title"));
                news.put("originallink", item.getString("originallink"));
                news.put("link", item.getString("link"));
                news.put("description", item.getString("description"));
                news.put("pubDate", item.getString("pubDate"));
                list.add(news);
            }

            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}