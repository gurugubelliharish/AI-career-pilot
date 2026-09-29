package com.aicareer.jobradar.module.resume.repository;

import com.aicareer.jobradar.module.resume.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByUserIdAndActiveTrue(UUID userId);

    Optional<Resume> findTopByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    Optional<Resume> findByIdAndUserId(UUID id, UUID userId);
}
