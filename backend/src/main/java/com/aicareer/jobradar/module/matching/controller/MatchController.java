package com.aicareer.jobradar.module.matching.controller;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.discovery.model.Job;
import com.aicareer.jobradar.module.discovery.repository.JobRepository;
import com.aicareer.jobradar.module.matching.model.MatchResult;
import com.aicareer.jobradar.module.matching.service.JobMatchingService;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import com.aicareer.jobradar.module.profile.service.ProfileService;
import com.aicareer.jobradar.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class MatchController {

    private final JobMatchingService matchingService;
    private final ProfileService profileService;
    private final JobRepository jobRepository;

    /**
     * Returns the top N jobs ranked by priority score for the authenticated user.
     * Computes match scores, freshness, and goal alignment on demand.
     */
    @GetMapping("/matches")
    public ApiResponse<List<MatchResult>> getMatches(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "30") int limit) {

        CandidateProfile profile = profileService.getOrCreateProfile(user);

        List<Job> recentJobs = jobRepository
                .findAll(PageRequest.of(0, 100, Sort.by("postedAt").descending()))
                .getContent()
                .stream()
                .filter(Job::isActive)
                .collect(Collectors.toList());

        if (recentJobs.isEmpty()) {
            return ApiResponse.ok(List.of());
        }

        List<MatchResult> ranked = recentJobs.stream()
                .map(job -> matchingService.match(profile, job))
                .sorted(Comparator.comparingDouble(MatchResult::getPriorityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Computed {} match results for userId={}", ranked.size(), user.getId());
        return ApiResponse.ok(ranked);
    }
}
