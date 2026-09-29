package com.aicareer.jobradar.module.discovery.scheduler;

import com.aicareer.jobradar.module.discovery.service.JobDiscoveryService;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import com.aicareer.jobradar.module.profile.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScanScheduler {

    private final JobDiscoveryService discoveryService;
    private final CandidateProfileRepository profileRepository;

    @Value("${app.scanning.enabled}")
    private boolean scanEnabled;

    @Scheduled(fixedDelayString = "${app.scanning.interval-minutes}",
               timeUnit = TimeUnit.MINUTES,
               initialDelay = 2)
    public void runScheduledScan() {
        if (!scanEnabled) {
            log.debug("Job scanning is disabled — skipping");
            return;
        }

        log.info("Starting scheduled job scan");
        for (CandidateProfile profile : profileRepository.findAll()) {
            if (!profile.isProfileComplete()) continue;
            int found = discoveryService.discoverJobsForProfile(profile);
            log.info("Scheduled scan found {} new jobs for userId={}", found, profile.getUser().getId());
        }
        log.info("Scheduled job scan complete");
    }
}
