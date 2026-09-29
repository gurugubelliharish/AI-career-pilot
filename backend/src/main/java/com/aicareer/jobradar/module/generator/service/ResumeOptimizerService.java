package com.aicareer.jobradar.module.generator.service;

import com.aicareer.jobradar.module.discovery.model.Job;
import com.aicareer.jobradar.module.profile.model.CandidateProfile;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeOptimizerService {

    private final OllamaChatModel chatModel;

    private static final String PROMPT = """
            You are an expert ATS resume writer.

            Candidate Profile:
            Name: %s
            Skills: %s
            Experience: %s

            Target Job Description:
            %s

            Task:
            1. Rewrite the resume to be ATS-optimized for this job.
            2. Naturally incorporate keywords from the job description.
            3. Highlight relevant experience and skills.
            4. Keep it concise (max 1 page equivalent).

            Return the optimized resume in clean plain text format.
            """;

    public String generateOptimizedResume(CandidateProfile profile, Job job, String baseResumeText) {
        String skills = profile.getSkills().stream()
                .filter(s -> s.isConfirmed())
                .map(s -> s.getSkillName())
                .reduce((a, b) -> a + ", " + b).orElse("");

        String prompt = PROMPT.formatted(
                profile.getName(),
                skills,
                baseResumeText.substring(0, Math.min(baseResumeText.length(), 2000)),
                job.getDescription().substring(0, Math.min(job.getDescription().length(), 2000)));

        log.info("Generating optimized resume for job: {} at {}", job.getTitle(), job.getCompany());
        return chatModel.generate(prompt);
    }
}
