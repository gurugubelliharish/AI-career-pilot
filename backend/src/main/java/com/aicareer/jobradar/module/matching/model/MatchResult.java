package com.aicareer.jobradar.module.matching.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MatchResult {

    private UUID jobId;
    private String jobTitle;
    private String company;

    /** 0–100 overall relevance score */
    private double matchScore;

    /** 0–100 freshness score */
    private double freshnessScore;

    /** 0–100 career-goal alignment score */
    private double goalAlignmentScore;

    /** Weighted composite: matchScore + freshnessScore + goalAlignmentScore */
    private double priorityScore;

    private String priority;   // CRITICAL | HIGH | MEDIUM | LOW

    private List<String> matchedSkills;
    private List<String> missingSkills;

    private String postedAgo;

    public static String toPriorityLabel(double score) {
        if (score >= 85) return "CRITICAL";
        if (score >= 70) return "HIGH";
        if (score >= 50) return "MEDIUM";
        return "LOW";
    }
}
