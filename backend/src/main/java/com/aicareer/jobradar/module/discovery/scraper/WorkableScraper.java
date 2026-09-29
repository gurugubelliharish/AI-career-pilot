package com.aicareer.jobradar.module.discovery.scraper;

import com.aicareer.jobradar.config.ScraperProperties;
import com.aicareer.jobradar.module.discovery.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fetches jobs from Workable's public aggregator.
 *
 * Background mode (no query): cursor-based incremental crawl, 10 pages per scheduler run.
 *   Cursor stored in Redis — each run continues where the last left off.
 *   ~2,400ms per run (10 pages × 200ms delay). All 173K fetched over ~24h.
 *
 * Keyword mode (query provided): full pagination for that specific query until exhausted.
 *   200ms delay between pages. Stops immediately on 429.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkableScraper implements JobScraper {

    private final RestTemplate restTemplate;
    private final ScraperProperties props;
    private final WorkableCursorStore cursorStore;

    private static final String API_URL              = "https://jobs.workable.com/api/v1/jobs";
    private static final int    PAGE_SIZE            = 20;   // Workable API max
    private static final int    PAGES_PER_SCAN       = 10;   // conservative — avoids 429
    private static final long   DELAY_MS             = 200;  // 200ms between requests

    @Override
    public String getSource() { return "WORKABLE"; }

    @Override
    public List<Job> scrape(String query, String location) {
        if (!props.isWorkableEnabled()) return List.of();

        boolean keyword = query != null && !query.isBlank();
        if (keyword) {
            // Keyword mode: user-initiated, always attempt (ignore background rate limit)
            return scrapePages(query, null, Integer.MAX_VALUE, false);
        } else {
            // Background mode: skip if rate-limited
            if (cursorStore.isRateLimited()) {
                log.info("Workable background scan skipped — rate limit backoff active");
                return List.of();
            }
            String cursor = cursorStore.getCursor();
            return scrapePages(null, cursor, PAGES_PER_SCAN, true);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Job> scrapePages(String query, String startToken, int maxPages, boolean updateBackoffOnRateLimit) {
        List<Job> results = new ArrayList<>();
        String pageToken = startToken;
        int pages = 0;

        while (pages < maxPages) {
            try {
                UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(API_URL)
                        .queryParam("limit", PAGE_SIZE);
                if (query != null && !query.isBlank()) b.queryParam("query", query);
                if (pageToken != null && !pageToken.isBlank()) b.queryParam("pageToken", pageToken);

                Map<String, Object> response = restTemplate.getForObject(b.toUriString(), Map.class);
                if (response == null) break;

                List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
                if (jobs == null || jobs.isEmpty()) {
                    cursorStore.resetCursor();
                    pageToken = null;
                    break;
                }

                jobs.stream().map(this::toJob).forEach(results::add);
                pages++;

                pageToken = (String) response.get("nextPageToken");
                if (pageToken == null || pageToken.isBlank()) {
                    cursorStore.resetCursor();
                    pageToken = null;
                    break;
                }

                // Polite delay between pages
                Thread.sleep(DELAY_MS);

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    if (updateBackoffOnRateLimit) {
                        cursorStore.markRateLimited(Duration.ofMinutes(30));
                        if (pageToken != null && !pageToken.isBlank()) {
                            cursorStore.saveCursor(pageToken);
                        }
                    } else {
                        log.warn("Workable 429 on keyword search — returning {} jobs fetched so far", results.size());
                    }
                    break;
                }
                log.error("Workable HTTP error: {}", e.getMessage());
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Workable scraper error: {}", e.getMessage());
                break;
            }
        }

        // Persist cursor only in background (no-query) mode
        if (query == null || query.isBlank()) {
            if (updateBackoffOnRateLimit && cursorStore.isRateLimited()) {
                log.info("Workable background: {} jobs before rate limit — will resume after backoff", results.size());
            } else if (pageToken != null && !pageToken.isBlank()) {
                cursorStore.saveCursor(pageToken);
                log.info("Workable background: {} jobs this run, cursor saved", results.size());
            } else {
                log.info("Workable background: {} jobs, full cycle done — will restart from page 1", results.size());
            }
        } else {
            log.info("Workable keyword='{}': {} jobs fetched", query, results.size());
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Job toJob(Map<String, Object> raw) {
        Map<String, Object> company    = (Map<String, Object>) raw.getOrDefault("company", Map.of());
        Map<String, Object> locationMap = (Map<String, Object>) raw.getOrDefault("location", Map.of());
        List<String> locations         = (List<String>) raw.getOrDefault("locations", List.of());

        String city    = (String) locationMap.getOrDefault("city", "");
        String country = (String) locationMap.getOrDefault("countryName", "");
        String loc     = buildLocation(city, country, locations);
        String mode    = deriveWorkMode((String) raw.getOrDefault("workplace", ""), locations);

        return Job.builder()
                .title((String) raw.getOrDefault("title", ""))
                .company((String) company.getOrDefault("title", ""))
                .location(loc)
                .description(cleanHtml((String) raw.getOrDefault("description", "")))
                .workMode(mode)
                .source(getSource())
                .externalJobId((String) raw.get("id"))
                .sourceUrl((String) raw.getOrDefault("url", ""))
                .postedAt(parseDate((String) raw.get("created")))
                .requiredSkills(new ArrayList<>())
                .build();
    }

    private String buildLocation(String city, String country, List<String> locations) {
        if (!locations.isEmpty()) {
            String first = locations.get(0);
            return first.toLowerCase().contains("remote") ? "Remote" : first;
        }
        if (!city.isBlank() && !country.isBlank()) return city + ", " + country;
        if (!city.isBlank()) return city;
        return country.isBlank() ? "" : country;
    }

    private String deriveWorkMode(String workplace, List<String> locations) {
        if ("remote".equalsIgnoreCase(workplace)) return "REMOTE";
        if ("hybrid".equalsIgnoreCase(workplace)) return "HYBRID";
        if ("on_site".equalsIgnoreCase(workplace) || "onsite".equalsIgnoreCase(workplace)) return "ONSITE";
        if (locations.stream().anyMatch(l -> l.toLowerCase().contains("remote"))) return "REMOTE";
        return null;
    }

    private Instant parseDate(String s) {
        if (s == null || s.isBlank()) return Instant.now();
        try { return Instant.parse(s); } catch (DateTimeParseException e) { return Instant.now(); }
    }

    private String cleanHtml(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("<[^>]+>", " ")
                  .replaceAll("&[a-zA-Z#0-9]+;", " ")
                  .replaceAll("\\s+", " ").trim();
    }
}
