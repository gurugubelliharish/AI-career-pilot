package com.aicareer.jobradar.module.discovery.scraper;

import com.aicareer.jobradar.module.discovery.model.Job;

import java.util.List;

/**
 * Contract for all job-board scrapers.
 * Each implementation targets a specific platform (Naukri, Foundit, Greenhouse, etc.)
 */
public interface JobScraper {

    /** Platform identifier — stored in Job.source */
    String getSource();

    /**
     * Fetches newly posted jobs matching the given query and location.
     *
     * @param query    role / keyword to search
     * @param location preferred location (or "remote")
     * @return list of raw Job objects (not yet persisted)
     */
    List<Job> scrape(String query, String location);
}
