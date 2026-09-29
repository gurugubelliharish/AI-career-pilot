package com.aicareer.jobradar.module.tracker.repository;

import com.aicareer.jobradar.module.tracker.model.ApplicationStatus;
import com.aicareer.jobradar.module.tracker.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    List<JobApplication> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<JobApplication> findByUserIdAndStatus(UUID userId, ApplicationStatus status);

    Optional<JobApplication> findByUserIdAndJobId(UUID userId, UUID jobId);

    boolean existsByUserIdAndJobId(UUID userId, UUID jobId);

    @Query("SELECT COUNT(a) FROM JobApplication a WHERE a.user.id = :userId AND a.status = :status")
    long countByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ApplicationStatus status);
}
