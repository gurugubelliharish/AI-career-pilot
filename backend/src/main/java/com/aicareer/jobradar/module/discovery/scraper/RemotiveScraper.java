package com.aicareer.jobradar.module.discovery.scraper;

import com.aicareer.jobradar.config.ScraperProperties;
import com.aicareer.jobradar.module.discovery.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches remote jobs from Remotive (remotive.com).
 * Free public API — no auth required.
 * API: GET https://remotive.com/api/remote-jobs?search={query}&limit=100
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemotiveScraper implements JobScraper {

    private final RestTemplate restTemplate;
    private final ScraperProperties props;

    private static final String API_URL = "https://remotive.com/api/remote-jobs";

    @Override
    public String getSource() {
        return "REMOTIVE";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Job> scrape(String query, String location) {
        if (!props.isRemotiveEnabled()) return List.of();

        List<Job> results = new ArrayList<>();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("limit", 100);
            if (query != null && !query.isBlank()) {
                builder.queryParam("search", query);
            }
            String url = builder.toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return List.of();

            List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
            if (jobs == null) return List.of();

            results = jobs.stream()
                    .map(this::toJob)
                    .collect(Collectors.toList());

            log.info("Remotive scraped {} jobs for query='{}'", results.size(), query);
        } catch (Exception e) {
            log.error("Remotive scraper failed: {}", e.getMessage());
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Job toJob(Map<String, Object> raw) {
        List<String> tags = raw.containsKey("tags")
                ? (List<String>) raw.get("tags")
                : new ArrayList<>();

        String candidateLocation = (String) raw.getOrDefault("candidate_required_location", "Remote");
        String workMode = deriveWorkMode(candidateLocation, (String) raw.getOrDefault("job_type", ""));

        Instant postedAt = parseDate((String) raw.get("publication_date"));

        String id = String.valueOf(raw.get("id"));
        String url = (String) raw.getOrDefault("url", "");

        return Job.builder()
                .title((String) raw.get("title"))
                .company((String) raw.get("company_name"))
                .location(candidateLocation)
                .description(cleanHtml((String) raw.get("description")))
                .workMode(workMode)
                .salaryRange((String) raw.getOrDefault("salary", null))
                .source(getSource())
                .externalJobId(id)
                .sourceUrl(url)
                .postedAt(postedAt)
                .requiredSkills(normalizeTags(tags))
                .build();
    }

    private String deriveWorkMode(String location, String jobType) {
        String loc = location == null ? "" : location.toLowerCase();
        if (loc.contains("remote") || loc.isBlank() || loc.equals("worldwide")) return "REMOTE";
        if (jobType != null && jobType.toLowerCase().contains("hybrid")) return "HYBRID";
        return "REMOTE"; // Remotive is all-remote by definition
    }

    private Instant parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return Instant.now();
        try {
            return Instant.parse(dateStr.endsWith("Z") ? dateStr : dateStr + "Z");
        } catch (DateTimeParseException e) {
            return Instant.now();
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) return new ArrayList<>();
        return tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }

    private String cleanHtml(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("<[^>]+>", " ")
                  .replaceAll("&[a-zA-Z#0-9]+;", " ")
                  .replaceAll("\\s+", " ")
                  .trim();
    }
}
