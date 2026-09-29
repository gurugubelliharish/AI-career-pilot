package com.aicareer.jobradar.module.discovery.model;

import com.aicareer.jobradar.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"source", "external_job_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job extends BaseEntity {

    private String externalJobId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String workMode;
    private String experienceLevel;
    private String salaryRange;

    @Column(nullable = false)
    private String source;   // NAUKRI | FOUNDIT | GREENHOUSE | etc.

    private String sourceUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_required_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    private Instant postedAt;

    @Builder.Default
    private double freshnessScore = 0.0;

    @Builder.Default
    private boolean active = true;
}
