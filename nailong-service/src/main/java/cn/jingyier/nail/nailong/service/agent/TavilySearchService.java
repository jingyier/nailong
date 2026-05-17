package cn.jingyier.nail.nailong.service.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TavilySearchService implements Function<TavilySearchService.SearchRequest, TavilySearchService.SearchResponse> {

    private static final Logger log = LoggerFactory.getLogger(TavilySearchService.class);
    private static final String API_URL = "https://api.tavily.com/search";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TavilySearchService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SearchResponse apply(SearchRequest request) {
        try {
            Map<String, Object> body = Map.of(
                    "query", request.query,
                    "search_depth", request.searchDepth != null ? request.searchDepth : "basic",
                    "max_results", request.maxResults > 0 ? request.maxResults : 5,
                    "include_answer", true
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            log.info("Tavily search: {}", request.query);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> resultMap = objectMapper.readValue(response.body(),
                        new TypeReference<Map<String, Object>>() {});

                String answer = (String) resultMap.getOrDefault("answer", "");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawResults = (List<Map<String, Object>>) resultMap.getOrDefault("results", Collections.emptyList());
                List<SearchResult> results = rawResults.stream()
                        .map(r -> new SearchResult(
                                (String) r.getOrDefault("title", ""),
                                (String) r.getOrDefault("url", ""),
                                (String) r.getOrDefault("content", ""),
                                ((Number) r.getOrDefault("score", 0.0)).doubleValue()
                        ))
                        .toList();

                return new SearchResponse(answer, results);
            } else {
                log.error("Tavily API error: {}", response.statusCode());
                return new SearchResponse("搜索暂时不可用，请稍后再试", Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("Tavily search failed", e);
            return new SearchResponse("搜索遇到了一点问题", Collections.emptyList());
        }
    }

    public record SearchRequest(
            @JsonProperty("query") String query,
            @JsonProperty("search_depth") String searchDepth,
            @JsonProperty("max_results") int maxResults
    ) {}

    public record SearchResponse(
            @JsonProperty("answer") String answer,
            @JsonProperty("results") List<SearchResult> results
    ) {}

    public record SearchResult(
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("content") String content,
            @JsonProperty("score") double score
    ) {}
}
