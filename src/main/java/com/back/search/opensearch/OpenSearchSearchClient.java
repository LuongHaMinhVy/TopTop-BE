package com.back.search.opensearch;

import com.back.user.model.entity.User;
import com.back.video.model.entity.Video;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSearchSearchClient {
    private final OpenSearchSearchProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public void ensureIndex() {
        if (!isEnabled()) {
            return;
        }

        try {
            // Check if index already exists
            ResponseEntity<Void> response = restTemplate.exchange(indexUrl(), HttpMethod.HEAD, new HttpEntity<>(authHeaders()), Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OpenSearch index {} already exists", properties.getIndexName());
                return;
            }
        } catch (Exception ex) {
            // Index likely doesn't exist (returns 404), proceed to create it
            log.info("OpenSearch index {} does not exist, creating it...", properties.getIndexName());
        }

        try {
            restTemplate.exchange(indexUrl(), HttpMethod.PUT, new HttpEntity<>(indexMapping(), jsonHeaders()), String.class);
            log.info("Successfully created OpenSearch index {}", properties.getIndexName());
        } catch (Exception ex) {
            log.warn("Could not ensure OpenSearch index {}", properties.getIndexName(), ex);
        }
    }

    public void bulkIndex(List<User> users, List<Video> videos) {
        if (!isEnabled() || (users.isEmpty() && videos.isEmpty())) {
            return;
        }

        StringBuilder body = new StringBuilder();
        users.forEach(user -> appendBulkDocument(body, documentId(SearchDocumentType.USER, user.getId()), userDocument(user)));
        videos.forEach(video -> appendBulkDocument(body, documentId(SearchDocumentType.VIDEO, video.getId()), videoDocument(video)));

        try {
            restTemplate.exchange(url("/_bulk"), HttpMethod.POST, new HttpEntity<>(body.toString(), ndjsonHeaders()), String.class);
        } catch (Exception ex) {
            log.warn("Could not bulk index search documents into OpenSearch", ex);
        }
    }

    public Optional<OpenSearchPage> searchIds(SearchDocumentType type, String keyword, Pageable pageable) {
        if (!isEnabled() || keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }

        Map<String, Object> query = Map.of(
                "from", Math.max(0, pageable.getPageNumber()) * pageable.getPageSize(),
                "size", pageable.getPageSize(),
                "track_total_hits", true,
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(Map.of("term", Map.of("type", type.name()))),
                                "should", List.of(
                                        Map.of("multi_match", Map.of(
                                                "query", keyword,
                                                "fields", fieldsFor(type),
                                                "fuzziness", "AUTO",
                                                "operator", "and"
                                        )),
                                        Map.of("multi_match", Map.of(
                                                "query", keyword,
                                                "fields", fieldsFor(type),
                                                "type", "phrase_prefix",
                                                "boost", 2
                                        ))
                                ),
                                "minimum_should_match", 1
                        )
                ),
                "sort", List.of(
                        Map.of("_score", Map.of("order", "desc")),
                        Map.of("popularity", Map.of("order", "desc")),
                        Map.of("createdAt", Map.of("order", "desc"))
                )
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url("/_search"),
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(query), jsonHeaders()),
                    String.class
            );
            return Optional.of(parseSearchPage(response.getBody()));
        } catch (Exception ex) {
            log.warn("OpenSearch search failed for type {} keyword '{}'", type, keyword, ex);
            return Optional.empty();
        }
    }

    public List<String> suggestKeywords(String keyword, int limit) {
        if (!isEnabled() || keyword == null || keyword.isBlank()) {
            return List.of();
        }

        Map<String, Object> query = Map.of(
                "size", limit,
                "_source", List.of("title", "username", "content"),
                "query", Map.of(
                        "multi_match", Map.of(
                                "query", keyword,
                                "fields", List.of("title^3", "username^4", "content", "authorUsername^2", "category"),
                                "type", "phrase_prefix"
                        )
                )
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url("/_search"),
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(query), jsonHeaders()),
                    String.class
            );
            JsonNode hits = objectMapper.readTree(response.getBody()).path("hits").path("hits");
            LinkedHashSet<String> suggestions = new LinkedHashSet<>();
            hits.forEach(hit -> {
                JsonNode source = hit.path("_source");
                addSuggestion(suggestions, source.path("title").asText(""));
                addSuggestion(suggestions, source.path("username").asText(""));
                addSuggestion(suggestions, source.path("content").asText(""));
            });
            return suggestions.stream().limit(limit).toList();
        } catch (Exception ex) {
            log.warn("OpenSearch suggestions failed for keyword '{}'", keyword, ex);
            return List.of();
        }
    }

    public Optional<String> suggestCorrection(String keyword) {
        if (!isEnabled() || keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }

        Map<String, Object> query = Map.of(
                "size", 0,
                "suggest", Map.of(
                        "title", termSuggest(keyword, "title"),
                        "username", termSuggest(keyword, "username"),
                        "content", termSuggest(keyword, "content"),
                        "authorUsername", termSuggest(keyword, "authorUsername"),
                        "category", termSuggest(keyword, "category")
                )
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url("/_search"),
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(query), jsonHeaders()),
                    String.class
            );
            return parseCorrection(keyword, response.getBody());
        } catch (Exception ex) {
            log.warn("OpenSearch correction suggestion failed for keyword '{}'", keyword, ex);
            return Optional.empty();
        }
    }

    private void appendBulkDocument(StringBuilder body, String id, Map<String, Object> document) {
        try {
            body.append(objectMapper.writeValueAsString(Map.of("index", Map.of("_index", properties.getIndexName(), "_id", id)))).append('\n');
            body.append(objectMapper.writeValueAsString(document)).append('\n');
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize OpenSearch search document {}", id, ex);
        }
    }

    private Map<String, Object> termSuggest(String keyword, String field) {
        return Map.of(
                "text", keyword,
                "term", Map.of(
                        "field", field,
                        "suggest_mode", "popular",
                        "min_word_length", 3,
                        "prefix_length", 1
                )
        );
    }

    private Optional<String> parseCorrection(String keyword, String body) throws JsonProcessingException {
        String normalizedKeyword = normalize(keyword);
        JsonNode suggest = objectMapper.readTree(body).path("suggest");
        for (String field : List.of("username", "title", "authorUsername", "content", "category")) {
            JsonNode entries = suggest.path(field);
            if (!entries.isArray() || entries.isEmpty()) {
                continue;
            }

            List<String> tokens = new ArrayList<>();
            boolean changed = false;
            for (JsonNode entry : entries) {
                JsonNode options = entry.path("options");
                String token = options.isArray() && !options.isEmpty()
                        ? options.get(0).path("text").asText(entry.path("text").asText(""))
                        : entry.path("text").asText("");
                if (!normalize(token).equals(normalize(entry.path("text").asText("")))) {
                    changed = true;
                }
                if (!token.isBlank()) {
                    tokens.add(token);
                }
            }

            String candidate = String.join(" ", tokens).strip();
            if (changed && !candidate.isBlank() && !normalize(candidate).equals(normalizedKeyword)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> userDocument(User user) {
        return Map.of(
                "type", SearchDocumentType.USER.name(),
                "targetId", user.getId(),
                "username", safe(user.getUsername()),
                "title", safe(user.getNickname()),
                "content", safe(user.getBio()),
                "popularity", safeLong(user.getFollowersCount()) + safeLong(user.getTotalLikes()),
                "createdAt", epochMillis(user.getCreatedAt())
        );
    }

    private Map<String, Object> videoDocument(Video video) {
        return Map.of(
                "type", SearchDocumentType.VIDEO.name(),
                "targetId", video.getId(),
                "title", safe(video.getTitle()),
                "content", safe(video.getDescription()),
                "authorUsername", video.getUser() == null ? "" : safe(video.getUser().getUsername()),
                "authorNickname", video.getUser() == null ? "" : safe(video.getUser().getNickname()),
                "category", safe(video.getCategory()),
                "popularity", safeLong(video.getLikeCount()) + safeLong(video.getViewCount()),
                "createdAt", epochMillis(video.getCreatedAt())
        );
    }

    private OpenSearchPage parseSearchPage(String body) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode hitsNode = root.path("hits");
        long total = hitsNode.path("total").path("value").asLong(0);
        List<Long> ids = new ArrayList<>();
        hitsNode.path("hits").forEach(hit -> ids.add(hit.path("_source").path("targetId").asLong()));
        return new OpenSearchPage(ids, total);
    }

    private List<String> fieldsFor(SearchDocumentType type) {
        if (type == SearchDocumentType.USER) {
            return List.of("username^5", "title^4", "content");
        }
        return List.of("title^5", "content^2", "authorUsername^3", "authorNickname^2", "category");
    }

    private Map<String, Object> indexMapping() {
        return Map.of(
                "settings", Map.of(
                        "analysis", Map.of(
                                "normalizer", Map.of(
                                        "lowercase_normalizer", Map.of("type", "custom", "filter", List.of("lowercase", "asciifolding"))
                                )
                        )
                ),
                "mappings", Map.of(
                        "properties", Map.of(
                                "type", Map.of("type", "keyword"),
                                "targetId", Map.of("type", "long"),
                                "username", Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword", "normalizer", "lowercase_normalizer"))),
                                "title", Map.of("type", "text"),
                                "content", Map.of("type", "text"),
                                "authorUsername", Map.of("type", "text"),
                                "authorNickname", Map.of("type", "text"),
                                "category", Map.of("type", "text"),
                                "popularity", Map.of("type", "long"),
                                "createdAt", Map.of("type", "date", "format", "epoch_millis")
                        )
                )
        );
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders ndjsonHeaders() {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-ndjson"));
        return headers;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (!properties.getUsername().isBlank()) {
            String raw = properties.getUsername() + ":" + properties.getPassword();
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
        }
        return headers;
    }

    private String indexUrl() {
        return properties.getUrl().replaceAll("/+$", "") + "/" + properties.getIndexName();
    }

    private String url(String path) {
        return indexUrl() + path;
    }

    private String documentId(SearchDocumentType type, Long id) {
        return type.name().toLowerCase() + "-" + id;
    }

    private void addSuggestion(LinkedHashSet<String> suggestions, String value) {
        if (value != null && !value.isBlank()) {
            suggestions.add(value.strip());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private long epochMillis(java.time.LocalDateTime value) {
        return value == null ? 0L : value.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }

    public record OpenSearchPage(List<Long> ids, long total) {
    }
}
