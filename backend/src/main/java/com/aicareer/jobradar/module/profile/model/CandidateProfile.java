package com.aicareer.jobradar.module.profile.model;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.shared.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile extends BaseEntity {

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String name;
    private String email;
    private String phone;
    private String linkedinUrl;
    private String githubUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_preferred_roles", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "role")
    @Builder.Default
    private List<String> preferredRoles = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_preferred_locations", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "location")
    @Builder.Default
    private List<String> preferredLocations = new ArrayList<>();

    private String workMode;         // REMOTE | HYBRID | ONSITE
    private String experienceLevel;  // FRESHER | 0_2 | 2_5 | 5_PLUS

    private String expectedSalary;
    private String noticePeriod;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_career_goals", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "goal")
    @Builder.Default
    private List<String> careerGoals = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> education;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> experience;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> projects;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> certifications;

    @Builder.Default
    private boolean profileComplete = false;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<CandidateSkill> skills = new ArrayList<>();
}
