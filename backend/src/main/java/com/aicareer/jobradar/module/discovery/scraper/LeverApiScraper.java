package com.aicareer.jobradar.module.discovery.scraper;

import com.aicareer.jobradar.config.ScraperProperties;
import com.aicareer.jobradar.module.discovery.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches jobs from companies that use Lever ATS.
 * Public API — no auth required.
 * API docs: https://api.lever.co/v0/postings/{company}?mode=json
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeverApiScraper implements JobScraper {

    private final RestTemplate restTemplate;
    private final ScraperProperties props;

    private static final String API_URL = "https://api.lever.co/v0/postings/%s?mode=json";

    @Override
    public String getSource() {
        return "LEVER";
    }

    @Override
    public List<Job> scrape(String query, String location) {
        List<Job> results = new ArrayList<>();

        for (String slug : props.getLeverSlugs()) {
            try {
                results.addAll(fetchCompanyJobs(slug, query, location));
            } catch (Exception e) {
                log.warn("Lever fetch failed for company '{}': {}", slug, e.getMessage());
            }
        }

        log.info("Lever scraped {} jobs for query='{}'", results.size(), query);
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Job> fetchCompanyJobs(String slug, String query, String location) {
        String url = String.format(API_URL, slug);
        List<Map<String, Object>> rawJobs = restTemplate.getForObject(url, List.class);

        if (rawJobs == null) return List.of();

        String queryLower = query.toLowerCase();
        String locationLower = location.toLowerCase();
        String companyName = slugToCompanyName(slug);

        return rawJobs.stream()
                .filter(j -> matchesQuery((String) j.get("text"), queryLower))
                .filter(j -> matchesLocation(extractLocation(j), locationLower))
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
    private String extractLocation(Map<String, Object> raw) {
        Map<String, Object> categories = (Map<String, Object>) raw.get("categories");
        return categories != null ? (String) categories.getOrDefault("location", "") : "";
    }

    @SuppressWarnings("unchecked")
    private String extractWorkMode(Map<String, Object> raw) {
        Map<String, Object> categories = (Map<String, Object>) raw.get("categories");
        if (categories == null) return null;
        String commitment = (String) categories.get("commitment");
        if (commitment == null) return null;
        String lower = commitment.toLowerCase();
        if (lower.contains("remote")) return "REMOTE";
        if (lower.contains("hybrid")) return "HYBRID";
        if (lower.contains("onsite") || lower.contains("on-site")) return "ONSITE";
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractDescription(Map<String, Object> raw) {
        // Lever returns both description (HTML) and descriptionPlain
        String plain = (String) raw.get("descriptionPlain");
        if (plain != null && !plain.isBlank()) return plain.trim();
        String html = (String) raw.get("description");
        return html != null ? stripHtml(html) : "";
    }

    private Job toJob(Map<String, Object> raw, String companyName) {
        Object createdAtRaw = raw.get("createdAt");
        Instant postedAt = createdAtRaw instanceof Number
                ? Instant.ofEpochMilli(((Number) createdAtRaw).longValue())
                : Instant.now();

        return Job.builder()
                .title((String) raw.get("text"))
                .company(companyName)
                .location(extractLocation(raw))
                .description(extractDescription(raw))
                .workMode(extractWorkMode(raw))
                .source(getSource())
                .externalJobId((String) raw.get("id"))
                .sourceUrl((String) raw.getOrDefault("hostedUrl", ""))
                .postedAt(postedAt)
                .requiredSkills(new ArrayList<>())
                .build();
    }

    private String stripHtml(String html) {
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
