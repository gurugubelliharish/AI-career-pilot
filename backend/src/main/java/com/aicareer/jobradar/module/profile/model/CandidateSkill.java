package com.aicareer.jobradar.module.profile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_skills",
       uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "skill_name"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private CandidateProfile profile;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    private String proficiency;  // BEGINNER | INTERMEDIATE | ADVANCED

    @Builder.Default
    private boolean inferred = false;

    @Builder.Default
    private boolean confirmed = false;

    @CreationTimestamp
    private Instant createdAt;
}
