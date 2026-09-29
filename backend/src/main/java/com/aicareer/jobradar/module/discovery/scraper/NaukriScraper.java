package com.aicareer.jobradar.module.discovery.scraper;

import com.aicareer.jobradar.module.discovery.model.Job;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Scrapes Naukri.com using Playwright headless Chrome.
 *
 * SELECTOR GUIDE — if Naukri changes their DOM, inspect any job card with
 * Chrome DevTools (F12 → Elements) and update the constants below:
 *   CARD_SELECTOR   → the repeating job card wrapper
 *   TITLE_SELECTOR  → the job title anchor tag
 *   COMPANY_SEL     → company name element
 *   LOCATION_SEL    → location element
 *   SKILLS_SEL      → each skill tag li element
 *   POSTED_SEL      → "X days ago" freshness text
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.scrapers.naukri-enabled", havingValue = "true", matchIfMissing = false)
public class NaukriScraper implements JobScraper {

    // ── Selectors (Naukri DOM as of mid-2024) ──────────────────────────────
    private static final String CARD_SELECTOR    = ".srp-jobtuple-wrapper";
    private static final String TITLE_SELECTOR   = "a.title";
    private static final String COMPANY_SEL      = ".comp-name";
    private static final String LOCATION_SEL     = ".locWdth";
    private static final String SKILLS_SEL       = ".tag-li";
    private static final String POSTED_SEL       = ".job-post-day";
    // ───────────────────────────────────────────────────────────────────────

    private static final int PAGE_LOAD_TIMEOUT_MS = 15_000;

    @Override
    public String getSource() {
        return "NAUKRI";
    }

    @Override
    public List<Job> scrape(String query, String location) {
        List<Job> jobs = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                                          "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
            Page page = context.newPage();

            String url = buildSearchUrl(query, location);
            log.info("Scraping Naukri: {}", url);

            page.navigate(url);

            // Dismiss cookie banner if present
            try {
                page.click("#cookie_consent_accept",
                        new Page.ClickOptions().setTimeout(3_000));
            } catch (Exception ignored) { /* no banner */ }

            // Wait for job cards to appear
            try {
                page.waitForSelector(CARD_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(PAGE_LOAD_TIMEOUT_MS));
            } catch (Exception e) {
                log.warn("Naukri: no job cards found for query='{}' location='{}' — " +
                         "possible CAPTCHA or selector change", query, location);
                return jobs;
            }

            List<ElementHandle> cards = page.querySelectorAll(CARD_SELECTOR);
            log.info("Naukri found {} cards for query='{}'", cards.size(), query);

            for (ElementHandle card : cards) {
                try {
                    jobs.add(parseCard(card));
                } catch (Exception e) {
                    log.debug("Skipped Naukri card: {}", e.getMessage());
                }
            }

            context.close();
            browser.close();
        } catch (Exception e) {
            log.error("Naukri scraper failed", e);
        }

        return jobs;
    }

    private Job parseCard(ElementHandle card) {
        String title   = getText(card, TITLE_SELECTOR);
        String company = getText(card, COMPANY_SEL);
        String location = getText(card, LOCATION_SEL);
        String href    = getAttribute(card, TITLE_SELECTOR, "href");
        String posted  = getText(card, POSTED_SEL);

        List<String> skills = new ArrayList<>();
        for (ElementHandle tag : card.querySelectorAll(SKILLS_SEL)) {
            String skill = tag.innerText().trim();
            if (!skill.isBlank()) skills.add(skill);
        }

        return Job.builder()
                .title(title)
                .company(company)
                .location(location)
                .source(getSource())
                .externalJobId(extractJobId(href))
                .sourceUrl(href != null ? href : "")
                .requiredSkills(skills)
                .postedAt(parseFreshness(posted))
                .build();
    }

    private String buildSearchUrl(String query, String location) {
        String q = query.trim().replace(" ", "-").toLowerCase();
        String l = location.trim().replace(" ", "-").toLowerCase();
        return "https://www.naukri.com/" + q + "-jobs-in-" + l;
    }

    /**
     * Extracts the numeric job ID from a Naukri job URL.
     * URL pattern: .../job-listings-...-{numericId}
     */
    private String extractJobId(String href) {
        if (href == null || href.isBlank()) return null;
        String[] parts = href.split("-");
        String last = parts[parts.length - 1].split("\\?")[0];
        return last.matches("\\d+") ? last : null;
    }

    /**
     * Converts Naukri's human-readable freshness string to an Instant.
     * Examples: "Just Now", "1 Day Ago", "3 Days Ago", "2 Weeks Ago"
     */
    private Instant parseFreshness(String text) {
        if (text == null || text.isBlank()) return Instant.now();
        String t = text.toLowerCase().trim();

        if (t.contains("just now") || t.contains("today") || t.contains("few hours")) {
            return Instant.now();
        }
        if (t.contains("yesterday")) {
            return Instant.now().minus(1, ChronoUnit.DAYS);
        }

        try {
            int number = Integer.parseInt(t.replaceAll("[^0-9]", ""));
            if (t.contains("week"))  return Instant.now().minus(number * 7L, ChronoUnit.DAYS);
            if (t.contains("day"))   return Instant.now().minus(number, ChronoUnit.DAYS);
            if (t.contains("hour"))  return Instant.now().minus(number, ChronoUnit.HOURS);
            if (t.contains("month")) return Instant.now().minus(number * 30L, ChronoUnit.DAYS);
        } catch (NumberFormatException ignored) { /* unparseable */ }

        return Instant.now();
    }

    private String getText(ElementHandle parent, String selector) {
        ElementHandle el = parent.querySelector(selector);
        return el != null ? el.innerText().trim() : "";
    }

    private String getAttribute(ElementHandle parent, String selector, String attr) {
        ElementHandle el = parent.querySelector(selector);
        return el != null ? (String) el.getAttribute(attr) : null;
    }
}
