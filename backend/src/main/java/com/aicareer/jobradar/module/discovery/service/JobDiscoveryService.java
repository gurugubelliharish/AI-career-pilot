package com.aicareer.jobradar.module.discovery.service;

import com.aicareer.jobradar.module.discovery.model.Job;
import com.aicareer.jobradar.module.discovery.repository.JobRepository;
import com.aicareer.jobradar.module.discovery.scraper.JobScraper;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobDiscoveryService {

    private final List<JobScraper> scrapers;
    private final JobRepository jobRepository;
    private final FreshnessService freshnessService;

    /** Scheduled scan — runs all scrapers with no filter. */
    public int discoverJobsForProfile(CandidateProfile profile) {
        int total = discoverJobs("", "");
        refreshFreshnessScores();
        return total;
    }

    /**
     * On-demand scan for a single source.
     * keywords: list of search terms (used by Workable); empty = fetch everything.
     */
    public int discoverJobsForSource(String source, List<String> keywords) {
        JobScraper scraper = scrapers.stream()
                .filter(s -> s.getSource().equalsIgnoreCase(source))
                .findFirst()
                .orElse(null);

        if (scraper == null) {
            log.warn("No scraper found for source: {}", source);
            return 0;
        }

        int total = 0;
        if (keywords != null && !keywords.isEmpty()) {
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank()) continue;
                try {
                    List<Job> jobs = scraper.scrape(keyword.trim(), "");
                    total += saveNewJobs(jobs, source);
                    log.info("{} saved {} new jobs for keyword='{}'", source, total, keyword.trim());
                } catch (Exception e) {
                    log.error("Scraper {} failed for keyword '{}': {}", source, keyword, e.getMessage());
                }
            }
        } else {
            try {
                List<Job> jobs = scraper.scrape("", "");
                total += saveNewJobs(jobs, source);
            } catch (Exception e) {
                log.error("Scraper {} failed: {}", source, e.getMessage());
            }
        }

        refreshFreshnessScores();
        return total;
    }

    /** Runs all scrapers. Empty query = fetch everything from each source. */
    @Transactional
    public int discoverJobs(String query, String location) {
        int newJobsCount = 0;
        for (JobScraper scraper : scrapers) {
            log.info("Running scraper: {}", scraper.getSource());
            try {
                List<Job> scraped = scraper.scrape(query, location);
                newJobsCount += saveNewJobs(scraped, scraper.getSource());
            } catch (Exception e) {
                log.error("Scraper {} failed: {}", scraper.getSource(), e.getMessage());
            }
        }
        log.info("Job discovery complete. New jobs saved: {}", newJobsCount);
        return newJobsCount;
    }

    private int saveNewJobs(List<Job> jobs, String source) {
        int saved = 0;
        for (Job job : jobs) {
            if (job.getExternalJobId() == null ||
                    jobRepository.existsBySourceAndExternalJobId(source, job.getExternalJobId())) {
                continue;
            }
            job.setFreshnessScore(freshnessService.calculateScore(job));
            jobRepository.save(job);
            saved++;
        }
        return saved;
    }

    public void refreshFreshnessScores() {
        List<Job> activeJobs = jobRepository.findAll();
        activeJobs.forEach(job -> job.setFreshnessScore(freshnessService.calculateScore(job)));
        jobRepository.saveAll(activeJobs);
    }
}
