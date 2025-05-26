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
import java.net.URLEncoder;

import java.net.URI;
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

    private String cleanHTMLExceptBold(String html) {
        return html.replaceAll("<(?!/?b>)[^>]+>", ""); // <b> 태그만 유지
    }

    @GetMapping("/news")
    public ResponseEntity<List<Map<String, Object>>> getNews(
        @RequestParam("keyword") String keyword,
        @RequestParam(value ="display", defaultValue = "10") int display,
        @RequestParam(value="start", defaultValue = "1") int start
    ) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());

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
                    .header("X-Naver-Client-Id","Czd_lY45xNZVMbwiOqTF")
                    .header("X-Naver-Client-Secret","yhJYNi8nun")
                    .build();

            ResponseEntity<String> result = restTemplate.exchange(req, String.class);

            JSONObject json = new JSONObject(result.getBody());
            JSONArray items = json.getJSONArray("items");

            List<Map<String, Object>> list = new ArrayList<>();

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                Map<String, Object> news = new HashMap<>();

                news.put("title", cleanHTMLExceptBold(item.getString("title")));
                news.put("originallink", item.getString("originallink"));
                news.put("link", item.getString("link"));
                news.put("description", cleanHTMLExceptBold(item.getString("description"))); // 또는 stripAllHTML
                news.put("pubDate", item.getString("pubDate"));

                list.add(news);
            }

            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}