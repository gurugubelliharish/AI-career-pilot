package com.aicareer.jobradar.module.profile.controller;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.profile.dto.UpdatePreferencesRequest;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import com.aicareer.jobradar.module.profile.service.ProfileService;
import com.aicareer.jobradar.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<CandidateProfile> getProfile(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(profileService.getOrCreateProfile(user));
    }

    @PutMapping("/preferences")
    public ApiResponse<CandidateProfile> updatePreferences(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ApiResponse.ok(profileService.updatePreferences(user.getId(), request));
    }

    @PostMapping("/skills/confirm")
    public ApiResponse<CandidateProfile> confirmSkills(
            @AuthenticationPrincipal User user,
            @RequestBody List<Map<String, String>> skillConfirmations) {
        return ApiResponse.ok(profileService.confirmSkills(user.getId(), skillConfirmations));
    }
}
