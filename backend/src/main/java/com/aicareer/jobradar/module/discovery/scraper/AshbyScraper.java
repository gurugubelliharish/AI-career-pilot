package com.aicareer.jobradar.module.discovery.scraper;

import com.aicareer.jobradar.config.ScraperProperties;
import com.aicareer.jobradar.module.discovery.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches jobs from companies using Ashby ATS.
 * Free public Job Board API — no auth required.
 * API: GET https://api.ashbyhq.com/posting-api/job-board/{company}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AshbyScraper implements JobScraper {

    private final RestTemplate restTemplate;
    private final ScraperProperties props;

    private static final String API_URL = "https://api.ashbyhq.com/posting-api/job-board/%s";

    @Override
    public String getSource() {
        return "ASHBY";
    }

    @Override
    public List<Job> scrape(String query, String location) {
        if (props.getAshbySlugs().isEmpty()) return List.of();

        List<Job> results = new ArrayList<>();
        String queryLower = query.toLowerCase();
        String locationLower = location.toLowerCase();

        for (String slug : props.getAshbySlugs()) {
            try {
                results.addAll(fetchCompanyJobs(slug, queryLower, locationLower));
            } catch (Exception e) {
                log.warn("Ashby fetch failed for company '{}': {}", slug, e.getMessage());
            }
        }

        log.info("Ashby scraped {} jobs for query='{}'", results.size(), query);
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Job> fetchCompanyJobs(String slug, String queryLower, String locationLower) {
        String url = String.format(API_URL, slug);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) return List.of();

        List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
        if (jobs == null || jobs.isEmpty()) return List.of();

        String companyName = slugToName(slug);

        return jobs.stream()
                .filter(j -> Boolean.TRUE.equals(j.get("isListed")))
                .filter(j -> matchesQuery((String) j.get("title"), queryLower))
                .filter(j -> matchesLocation((String) j.get("location"), (Boolean) j.get("isRemote"), locationLower))
                .map(j -> toJob(j, companyName))
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(String title, String queryLower) {
        if (title == null || queryLower.isBlank()) return true;
        return Arrays.stream(queryLower.split("\\s+"))
                .anyMatch(word -> title.toLowerCase().contains(word));
    }

    private boolean matchesLocation(String jobLocation, Boolean isRemote, String candidateLocation) {
        if (candidateLocation.isBlank()) return true;
        if (Boolean.TRUE.equals(isRemote)) return true;
        String loc = jobLocation == null ? "" : jobLocation.toLowerCase();
        return loc.contains("remote") || loc.contains(candidateLocation);
    }

    private Job toJob(Map<String, Object> raw, String companyName) {
        String location = (String) raw.getOrDefault("location", "Remote");
        Boolean isRemote = (Boolean) raw.getOrDefault("isRemote", false);
        String workplaceType = (String) raw.get("workplaceType");
        String workMode = deriveWorkMode(isRemote, workplaceType);

        String description = (String) raw.getOrDefault("descriptionPlain", "");
        if (description == null || description.isBlank()) {
            description = cleanHtml((String) raw.getOrDefault("descriptionHtml", ""));
        }

        return Job.builder()
                .title((String) raw.get("title"))
                .company(companyName)
                .location(location)
                .description(description)
                .workMode(workMode)
                .source(getSource())
                .externalJobId((String) raw.get("id"))
                .sourceUrl((String) raw.getOrDefault("jobUrl", ""))
                .postedAt(parseDate((String) raw.get("publishedAt")))
                .requiredSkills(new ArrayList<>())
                .build();
    }

    private String deriveWorkMode(Boolean isRemote, String workplaceType) {
        if (Boolean.TRUE.equals(isRemote)) return "REMOTE";
        if (workplaceType == null) return null;
        return switch (workplaceType.toLowerCase()) {
            case "remote" -> "REMOTE";
            case "hybrid" -> "HYBRID";
            case "onsite", "on-site" -> "ONSITE";
            default -> null;
        };
    }

    private Instant parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return Instant.now();
        try {
            return Instant.parse(dateStr.length() > 24
                    ? dateStr.substring(0, 23) + "Z"
                    : dateStr);
        } catch (DateTimeParseException e) {
            return Instant.now();
        }
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                   .replaceAll("&[a-zA-Z#0-9]+;", " ")
                   .replaceAll("\\s+", " ").trim();
    }

    private String slugToName(String slug) {
        return Arrays.stream(slug.split("[-_]"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }
}
