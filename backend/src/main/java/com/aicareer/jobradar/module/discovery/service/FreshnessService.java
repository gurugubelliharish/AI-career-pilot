package com.aicareer.jobradar.module.discovery.service;

import com.aicareer.jobradar.module.discovery.model.Job;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Calculates a freshness score (0–100) based on how recently a job was posted.
 * Score degrades linearly from 100 (posted now) to 0 (posted 7+ days ago).
 */
@Service
public class FreshnessService {

    private static final long MAX_AGE_HOURS = 168;  // 7 days

    public double calculateScore(Job job) {
        if (job.getPostedAt() == null) return 0.0;
        long ageMinutes = Duration.between(job.getPostedAt(), Instant.now()).toMinutes();
        if (ageMinutes <= 0) return 100.0;
        double ageHours = ageMinutes / 60.0;
        return Math.max(0.0, 100.0 - (ageHours / MAX_AGE_HOURS * 100.0));
    }

    public String describeAge(Job job) {
        if (job.getPostedAt() == null) return "Unknown";
        long minutes = Duration.between(job.getPostedAt(), Instant.now()).toMinutes();
        if (minutes < 60)   return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        long hours = minutes / 60;
        if (hours < 24)     return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        long days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }
}
