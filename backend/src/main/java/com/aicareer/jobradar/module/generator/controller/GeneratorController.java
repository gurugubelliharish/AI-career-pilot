package com.aicareer.jobradar.module.generator.controller;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.discovery.model.Job;
import com.aicareer.jobradar.module.discovery.repository.JobRepository;
import com.aicareer.jobradar.module.generator.service.CoverLetterService;
import com.aicareer.jobradar.module.generator.service.ResumeOptimizerService;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import com.aicareer.jobradar.module.profile.service.ProfileService;
import com.aicareer.jobradar.module.resume.repository.ResumeRepository;
import com.aicareer.jobradar.shared.dto.ApiResponse;
import com.aicareer.jobradar.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/generate")
@RequiredArgsConstructor
public class GeneratorController {

    private final ProfileService profileService;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeOptimizerService resumeOptimizer;
    private final CoverLetterService coverLetterService;

    @PostMapping("/resume/{jobId}")
    public ApiResponse<Map<String, String>> generateResume(@PathVariable UUID jobId,
                                                           @AuthenticationPrincipal User user) {
        CandidateProfile profile = profileService.getProfileByUserId(user.getId());
        Job job = getJob(jobId);
        String baseText = resumeRepository
                .findTopByUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId())
                .map(r -> r.getRawText()).orElse("");

        String optimized = resumeOptimizer.generateOptimizedResume(profile, job, baseText);
        return ApiResponse.ok(Map.of("optimizedResume", optimized));
    }

    @PostMapping("/cover-letter/{jobId}")
    public ApiResponse<Map<String, String>> generateCoverLetter(@PathVariable UUID jobId,
                                                                 @AuthenticationPrincipal User user) {
        CandidateProfile profile = profileService.getProfileByUserId(user.getId());
        Job job = getJob(jobId);
        String letter = coverLetterService.generateCoverLetter(profile, job);
        return ApiResponse.ok(Map.of("coverLetter", letter));
    }

    private Job getJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Job not found"));
    }
}
