package com.aicareer.jobradar.module.discovery.repository;

import com.aicareer.jobradar.module.discovery.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findBySourceAndExternalJobId(String source, String externalJobId);

    boolean existsBySourceAndExternalJobId(String source, String externalJobId);

    Page<Job> findByActiveTrueOrderByPostedAtDesc(Pageable pageable);

    List<Job> findByActiveTrueAndPostedAtAfterOrderByPostedAtDesc(Instant since);

    @Query("""
            SELECT j FROM Job j
            WHERE j.active = true
              AND j.postedAt >= :since
              AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
            ORDER BY j.postedAt DESC
            """)
    List<Job> findRecentJobsByLocation(@Param("since") Instant since,
                                       @Param("location") String location);
}
