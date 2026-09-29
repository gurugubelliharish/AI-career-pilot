package com.aicareer.jobradar.module.discovery.scraper;

import com.aicareer.jobradar.config.ScraperProperties;
import com.aicareer.jobradar.module.discovery.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches jobs from companies that use Greenhouse ATS.
 * Public API — no auth, no scraping, no bot detection.
 * API docs: https://boards-api.greenhouse.io/v1/boards/{company}/jobs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GreenhouseApiScraper implements JobScraper {

    private final RestTemplate restTemplate;
    private final ScraperProperties props;

    private static final String API_URL = "https://boards-api.greenhouse.io/v1/boards/%s/jobs?content=true";

    @Override
    public String getSource() {
        return "GREENHOUSE";
    }

    @Override
    public List<Job> scrape(String query, String location) {
        List<Job> results = new ArrayList<>();

        for (String slug : props.getGreenhouseSlugs()) {
            try {
                results.addAll(fetchCompanyJobs(slug, query, location));
            } catch (Exception e) {
                log.warn("Greenhouse fetch failed for company '{}': {}", slug, e.getMessage());
            }
        }

        log.info("Greenhouse scraped {} jobs for query='{}'", results.size(), query);
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Job> fetchCompanyJobs(String slug, String query, String location) {
        String url = String.format(API_URL, slug);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null || !response.containsKey("jobs")) {
            return List.of();
        }

        List<Map<String, Object>> rawJobs = (List<Map<String, Object>>) response.get("jobs");
        String queryLower = query.toLowerCase();
        String locationLower = location.toLowerCase();
        String companyName = slugToCompanyName(slug);

        return rawJobs.stream()
                .filter(j -> matchesQuery((String) j.get("title"), queryLower))
                .filter(j -> matchesLocation(extractLocationName(j), locationLower))
                .map(j -> toJob(j, companyName))
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(String title, String queryLower) {
        if (title == null || queryLower.isBlank()) return true;
        return Arrays.stream(queryLower.split("\\s+"))
                .anyMatch(word -> title.toLowerCase().contains(word));
    }

    private boolean matchesLocation(String jobLocation, String candidateLocation) {
        if (candidateLocation.isBlank()) return true;
        String loc = jobLocation.toLowerCase();
        return loc.contains("remote") || loc.contains(candidateLocation);
    }

    @SuppressWarnings("unchecked")
    private String extractLocationName(Map<String, Object> raw) {
        Map<String, Object> loc = (Map<String, Object>) raw.get("location");
        return loc != null ? (String) loc.getOrDefault("name", "") : "";
    }

    private Job toJob(Map<String, Object> raw, String companyName) {
        return Job.builder()
                .title((String) raw.get("title"))
                .company(companyName)
                .location(extractLocationName(raw))
                .description(stripHtml((String) raw.getOrDefault("content", "")))
                .source(getSource())
                .externalJobId(String.valueOf(raw.get("id")))
                .sourceUrl((String) raw.getOrDefault("absolute_url", ""))
                .postedAt(parseInstant((String) raw.get("updated_at")))
                .requiredSkills(new ArrayList<>())
                .build();
    }

    private Instant parseInstant(String value) {
        if (value == null) return Instant.now();
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                   .replaceAll("&[a-zA-Z]+;", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private String slugToCompanyName(String slug) {
        return Arrays.stream(slug.split("[-_]"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }
}
