package com.aicareer.jobradar.module.tracker.service;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.discovery.model.Job;
import com.aicareer.jobradar.module.discovery.repository.JobRepository;
import com.aicareer.jobradar.module.tracker.model.ApplicationStatus;
import com.aicareer.jobradar.module.tracker.model.JobApplication;
import com.aicareer.jobradar.module.tracker.repository.JobApplicationRepository;
import com.aicareer.jobradar.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationTrackerService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    @Transactional
    public JobApplication saveJob(User user, UUID jobId) {
        if (applicationRepository.existsByUserIdAndJobId(user.getId(), jobId)) {
            throw AppException.conflict("Job already saved");
        }
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> AppException.notFound("Job not found"));

        return applicationRepository.save(JobApplication.builder()
                .user(user).job(job).build());
    }

    @Transactional
    public JobApplication markApplied(User user, UUID jobId) {
        JobApplication app = applicationRepository.findByUserIdAndJobId(user.getId(), jobId)
                .orElseThrow(() -> AppException.notFound("Application not found"));
        app.setStatus(ApplicationStatus.APPLIED);
        app.setAppliedAt(Instant.now());
        return applicationRepository.save(app);
    }

    @Transactional
    public JobApplication updateStatus(User user, UUID applicationId, ApplicationStatus status) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> AppException.notFound("Application not found"));
        if (!app.getUser().getId().equals(user.getId())) {
            throw AppException.forbidden("Not your application");
        }
        app.setStatus(status);
        if (status == ApplicationStatus.APPLIED && app.getAppliedAt() == null) {
            app.setAppliedAt(Instant.now());
        }
        return applicationRepository.save(app);
    }

    public List<JobApplication> getApplications(UUID userId) {
        return applicationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Map<String, Long> getDashboardStats(UUID userId) {
        return Map.of(
                "total",     applicationRepository.countByUserIdAndStatus(userId, null),
                "saved",     applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.SAVED),
                "applied",   applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.APPLIED),
                "interview", applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.INTERVIEW),
                "offer",     applicationRepository.countByUserIdAndStatus(userId, ApplicationStatus.OFFER)
        );
    }
}
