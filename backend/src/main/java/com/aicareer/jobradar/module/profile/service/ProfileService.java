package com.aicareer.jobradar.module.profile.service;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.profile.dto.UpdatePreferencesRequest;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import com.aicareer.jobradar.module.profile.model.CandidateSkill;
import com.aicareer.jobradar.module.profile.repository.CandidateProfileRepository;
import com.aicareer.jobradar.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository profileRepository;

    public CandidateProfile getOrCreateProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    CandidateProfile profile = CandidateProfile.builder()
                            .user(user)
                            .name(user.getName())
                            .email(user.getEmail())
                            .build();
                    return profileRepository.save(profile);
                });
    }

    public CandidateProfile getProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Profile not found"));
    }

    @Transactional
    public CandidateProfile updatePreferences(UUID userId, UpdatePreferencesRequest req) {
        CandidateProfile profile = getProfileByUserId(userId);

        if (req.name()            != null) profile.setName(req.name());
        if (req.phone()           != null) profile.setPhone(req.phone());
        if (req.linkedinUrl()     != null) profile.setLinkedinUrl(req.linkedinUrl());
        if (req.githubUrl()       != null) profile.setGithubUrl(req.githubUrl());
        if (req.workMode()        != null) profile.setWorkMode(req.workMode());
        if (req.experienceLevel() != null) profile.setExperienceLevel(req.experienceLevel());
        if (req.expectedSalary()  != null) profile.setExpectedSalary(req.expectedSalary());
        if (req.noticePeriod()    != null) profile.setNoticePeriod(req.noticePeriod());

        if (req.preferredRoles() != null) {
            profile.getPreferredRoles().clear();
            profile.getPreferredRoles().addAll(req.preferredRoles());
        }
        if (req.preferredLocations() != null) {
            profile.getPreferredLocations().clear();
            profile.getPreferredLocations().addAll(req.preferredLocations());
        }
        if (req.careerGoals() != null) {
            profile.getCareerGoals().clear();
            profile.getCareerGoals().addAll(req.careerGoals());
        }

        profile.setProfileComplete(isProfileComplete(profile));
        log.info("Profile updated for userId={}, complete={}", userId, profile.isProfileComplete());
        return profileRepository.save(profile);
    }

    private boolean isProfileComplete(CandidateProfile profile) {
        return !profile.getPreferredRoles().isEmpty()
                && !profile.getPreferredLocations().isEmpty();
    }

    @Transactional
    public CandidateProfile confirmSkills(UUID userId, List<Map<String, String>> skillConfirmations) {
        CandidateProfile profile = getProfileByUserId(userId);

        profile.getSkills().forEach(skill -> {
            skillConfirmations.stream()
                    .filter(c -> skill.getSkillName().equalsIgnoreCase(c.get("skillName")))
                    .findFirst()
                    .ifPresent(c -> {
                        skill.setConfirmed(true);
                        skill.setProficiency(c.get("proficiency"));
                    });
        });

        return profileRepository.save(profile);
    }

    @Transactional
    public CandidateProfile addInferredSkills(UUID userId, List<String> skillNames) {
        CandidateProfile profile = getProfileByUserId(userId);

        skillNames.stream()
                .filter(name -> profile.getSkills().stream()
                        .noneMatch(s -> s.getSkillName().equalsIgnoreCase(name)))
                .map(name -> CandidateSkill.builder()
                        .profile(profile)
                        .skillName(name)
                        .inferred(true)
                        .build())
                .forEach(profile.getSkills()::add);

        return profileRepository.save(profile);
    }
}
