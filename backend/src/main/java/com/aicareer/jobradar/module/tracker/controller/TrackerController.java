package com.aicareer.jobradar.module.tracker.controller;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.tracker.model.ApplicationStatus;
import com.aicareer.jobradar.module.tracker.model.JobApplication;
import com.aicareer.jobradar.module.tracker.service.ApplicationTrackerService;
import com.aicareer.jobradar.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tracker")
@RequiredArgsConstructor
public class TrackerController {

    private final ApplicationTrackerService trackerService;

    @GetMapping
    public ApiResponse<List<JobApplication>> list(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(trackerService.getApplications(user.getId()));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(trackerService.getDashboardStats(user.getId()));
    }

    @PostMapping("/save/{jobId}")
    public ApiResponse<JobApplication> saveJob(@PathVariable UUID jobId,
                                               @AuthenticationPrincipal User user) {
        return ApiResponse.ok(trackerService.saveJob(user, jobId));
    }

    @PostMapping("/apply/{jobId}")
    public ApiResponse<JobApplication> markApplied(@PathVariable UUID jobId,
                                                    @AuthenticationPrincipal User user) {
        return ApiResponse.ok(trackerService.markApplied(user, jobId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<JobApplication> updateStatus(@PathVariable UUID id,
                                                     @RequestParam ApplicationStatus status,
                                                     @AuthenticationPrincipal User user) {
        return ApiResponse.ok(trackerService.updateStatus(user, id, status));
    }
}
