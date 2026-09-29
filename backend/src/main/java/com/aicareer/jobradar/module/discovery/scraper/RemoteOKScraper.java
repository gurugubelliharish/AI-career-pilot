package com.aicareer.jobradar.module.discovery.scraper;

import com.aicareer.jobradar.config.ScraperProperties;
import com.aicareer.jobradar.module.discovery.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches remote tech jobs from RemoteOK.
 * Free JSON API — returns tagged jobs with tech stack labels.
 * API: https://remoteok.com/api?tags={comma-separated-tags}
 * Note: RemoteOK requires a User-Agent header or it returns 403.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteOKScraper implements JobScraper {

    private final RestTemplate restTemplate;
    private final ScraperProperties props;

    private static final String API_URL_ALL  = "https://remoteok.com/api";
    private static final String API_URL_TAGS = "https://remoteok.com/api?tags=%s";

    @Override
    public String getSource() {
        return "REMOTEOK";
    }

    @Override
    public List<Job> scrape(String query, String location) {
        if (!props.isRemoteokEnabled()) return List.of();

        List<Job> results = new ArrayList<>();
        try {
            // Empty query → fetch all remote jobs; otherwise map to tags
            String url = (query == null || query.isBlank())
                    ? API_URL_ALL
                    : String.format(API_URL_TAGS, buildTagsFromQuery(query));

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "AIJobRadar/1.0 (+https://github.com/ai-career-pilot)");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Object> body = response.getBody();

            if (body == null || body.size() <= 1) return List.of();

            // First element is always a legal notice object — skip it
            for (int i = 1; i < body.size(); i++) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> raw = (Map<String, Object>) body.get(i);
                    results.add(toJob(raw));
                } catch (Exception e) {
                    log.debug("Skipped RemoteOK job entry: {}", e.getMessage());
                }
            }

            log.info("RemoteOK scraped {} jobs (query='{}')", results.size(), query);
        } catch (Exception e) {
            log.error("RemoteOK scraper failed: {}", e.getMessage());
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Job toJob(Map<String, Object> raw) {
        List<String> tags = raw.containsKey("tags")
                ? (List<String>) raw.get("tags")
                : new ArrayList<>();

        Object epochRaw = raw.get("epoch");
        Instant postedAt = epochRaw instanceof Number
                ? Instant.ofEpochSecond(((Number) epochRaw).longValue())
                : Instant.now();

        String location = (String) raw.getOrDefault("location", "Remote");
        String workMode = deriveWorkMode(location, tags);

        return Job.builder()
                .title((String) raw.get("position"))
                .company((String) raw.get("company"))
                .location(location)
                .description(cleanDescription((String) raw.get("description")))
                .workMode(workMode)
                .source(getSource())
                .externalJobId(String.valueOf(raw.get("id")))
                .sourceUrl((String) raw.getOrDefault("url", ""))
                .postedAt(postedAt)
                .requiredSkills(normalizeTags(tags))
                .build();
    }

    private String buildTagsFromQuery(String query) {
        // Map common role names to tech tags RemoteOK understands
        String queryLower = query.toLowerCase();
        if (queryLower.contains("java"))       return "java";
        if (queryLower.contains("python"))     return "python";
        if (queryLower.contains("react"))      return "react";
        if (queryLower.contains("node"))       return "node";
        if (queryLower.contains("golang") || queryLower.contains("go developer")) return "golang";
        if (queryLower.contains("typescript")) return "typescript";
        if (queryLower.contains("backend"))    return "backend";
        if (queryLower.contains("frontend"))   return "frontend";
        if (queryLower.contains("fullstack") || queryLower.contains("full stack")) return "fullstack";
        if (queryLower.contains("devops"))     return "devops";
        if (queryLower.contains("data"))       return "data";
        // Fall back to the configured default tags
        return props.getRemoteokTags();
    }

    private String deriveWorkMode(String location, List<String> tags) {
        String locLower = location == null ? "" : location.toLowerCase();
        if (locLower.contains("remote") || tags.stream().anyMatch(t -> t.equalsIgnoreCase("remote"))) {
            return "REMOTE";
        }
        return null;
    }

    private List<String> normalizeTags(List<String> tags) {
        return tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::toLowerCase)
                .filter(t -> !t.equals("remote") && !t.equals("full-time") && !t.equals("part-time"))
                .collect(Collectors.toList());
    }

    private String cleanDescription(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("<[^>]+>", " ")
                  .replaceAll("&[a-zA-Z]+;", " ")
                  .replaceAll("\\s+", " ")
                  .trim();
    }
}
