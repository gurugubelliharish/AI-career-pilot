package com.aicareer.jobradar.module.tracker.model;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.discovery.model.Job;
import com.aicareer.jobradar.shared.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "job_applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.SAVED;

    @Builder.Default
    private double matchScore = 0.0;

    private String priority;

    private String notes;

    private String tailoredResumePath;
    private String coverLetterPath;

    private Instant appliedAt;
}
