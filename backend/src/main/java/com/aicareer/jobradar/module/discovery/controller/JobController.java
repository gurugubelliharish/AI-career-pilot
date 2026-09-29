package com.aicareer.jobradar.module.discovery.controller;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.discovery.model.Job;
import com.aicareer.jobradar.module.discovery.repository.JobRepository;
import com.aicareer.jobradar.module.discovery.service.JobDiscoveryService;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import com.aicareer.jobradar.module.profile.repository.CandidateProfileRepository;
import com.aicareer.jobradar.shared.dto.ApiResponse;
import com.aicareer.jobradar.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final JobDiscoveryService discoveryService;
    private final CandidateProfileRepository profileRepository;

    @GetMapping
    public ApiResponse<Page<Job>> listJobs(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(jobRepository.findByActiveTrueOrderByPostedAtDesc(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<Job> getJob(@PathVariable UUID id) {
        return ApiResponse.ok(jobRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Job not found")));
    }

    /**
     * Starts a background scan and returns 202 immediately.
     *
     * @param source   optional — if set, only that scraper runs (e.g. WORKABLE, REMOTEOK)
     * @param keywords optional — comma-separated search terms for Workable (e.g. "java,python")
     *                 ignored when source is not WORKABLE
     */
    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<String>> scan(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String keywords) {

        UUID userId = Objects.requireNonNull(user.getId(), "authenticated user has no id");
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Profile not found — save your profile first"));

        CompletableFuture.runAsync(() -> {
            try {
                if (source != null && !source.isBlank()) {
                    List<String> kwList = (keywords != null && !keywords.isBlank())
                            ? Arrays.stream(keywords.split(","))
                                    .map(String::trim)
                                    .filter(k -> !k.isEmpty())
                                    .collect(Collectors.toList())
                            : List.of();
                    discoveryService.discoverJobsForSource(source, kwList);
                } else {
                    discoveryService.discoverJobsForProfile(profile);
                }
            } catch (Exception e) {
                log.error("Background scan failed: {}", e.getMessage());
            }
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Scan started"));
    }
}
