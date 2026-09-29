package com.aicareer.jobradar.module.matching.service;

import com.aicareer.jobradar.module.discovery.model.Job;
import com.aicareer.jobradar.module.discovery.service.FreshnessService;
import com.aicareer.jobradar.module.matching.model.MatchResult;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchingService {

    private final OllamaEmbeddingModel embeddingModel;
    private final FreshnessService freshnessService;

    /**
     * Calculates a MatchResult for a single job against a candidate profile.
     * Uses both keyword-based skill matching and semantic embedding similarity.
     */
    public MatchResult match(CandidateProfile profile, Job job) {
        Set<String> candidateSkills = profile.getSkills().stream()
                .filter(s -> s.isConfirmed() || !s.isInferred())
                .map(s -> s.getSkillName().toLowerCase())
                .collect(Collectors.toSet());

        List<String> matched  = new ArrayList<>();
        List<String> missing  = new ArrayList<>();

        for (String required : job.getRequiredSkills()) {
            if (candidateSkills.contains(required.toLowerCase())) {
                matched.add(required);
            } else {
                missing.add(required);
            }
        }

        double skillMatch = job.getRequiredSkills().isEmpty() ? 50.0
                : (double) matched.size() / job.getRequiredSkills().size() * 100.0;

        double freshness = freshnessService.calculateScore(job);
        double goalAlign = calculateGoalAlignment(profile, job);

        double priority = (skillMatch * 0.50) + (freshness * 0.30) + (goalAlign * 0.20);

        return MatchResult.builder()
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .company(job.getCompany())
                .matchScore(Math.round(skillMatch * 10.0) / 10.0)
                .freshnessScore(Math.round(freshness * 10.0) / 10.0)
                .goalAlignmentScore(Math.round(goalAlign * 10.0) / 10.0)
                .priorityScore(Math.round(priority * 10.0) / 10.0)
                .priority(MatchResult.toPriorityLabel(priority))
                .matchedSkills(matched)
                .missingSkills(missing)
                .postedAgo(freshnessService.describeAge(job))
                .build();
    }

    private double calculateGoalAlignment(CandidateProfile profile, Job job) {
        List<String> goals = profile.getCareerGoals();
        if (goals.isEmpty()) return 50.0;

        String jobText = buildJobText(job);
        if (jobText.isBlank()) return 50.0;

        try {
            String goalsText = String.join(". ", goals);
            float[] goalsVec = embeddingModel.embed(goalsText).content().vector();
            float[] jobVec   = embeddingModel.embed(jobText).content().vector();
            // cosine similarity is [-1, 1] — map to [0, 100]
            double similarity = cosineSimilarity(goalsVec, jobVec);
            return Math.max(0, Math.min(100, (similarity + 1.0) * 50.0));
        } catch (Exception e) {
            log.debug("Goal alignment embedding failed for '{}': {}", job.getTitle(), e.getMessage());
            return 50.0;
        }
    }

    private String buildJobText(Job job) {
        StringBuilder sb = new StringBuilder(job.getTitle() != null ? job.getTitle() : "");
        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            sb.append(" ").append(
                job.getDescription().substring(0, Math.min(500, job.getDescription().length())));
        }
        return sb.toString().trim();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
