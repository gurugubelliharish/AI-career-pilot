package com.aicareer.jobradar.module.profile.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdatePreferencesRequest(

        String name,
        String phone,
        String linkedinUrl,
        String githubUrl,

        @Size(max = 10, message = "Maximum 10 preferred roles")
        List<String> preferredRoles,

        @Size(max = 10, message = "Maximum 10 preferred locations")
        List<String> preferredLocations,

        /** REMOTE | HYBRID | ONSITE */
        String workMode,

        /** FRESHER | 0_2 | 2_5 | 5_PLUS */
        String experienceLevel,

        String expectedSalary,
        String noticePeriod,

        @Size(max = 10, message = "Maximum 10 career goals")
        List<String> careerGoals
) {}
