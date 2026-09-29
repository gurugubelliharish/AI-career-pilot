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
public class CoverLetterService {

    private final OllamaChatModel chatModel;

    private static final String PROMPT = """
            You are an expert career coach writing a personalized cover letter.

            Candidate:
            Name: %s
            Key Skills: %s
            Career Goals: %s

            Job:
            Role: %s
            Company: %s
            Description: %s

            Write a compelling, personalized cover letter (3–4 paragraphs).
            - Opening: express genuine interest in the role and company
            - Middle: connect candidate's skills directly to job requirements
            - Closing: confident call to action

            Tone: professional, enthusiastic, authentic. No generic filler.
            """;

    public String generateCoverLetter(CandidateProfile profile, Job job) {
        String skills = profile.getSkills().stream()
                .filter(s -> s.isConfirmed())
                .map(s -> s.getSkillName())
                .limit(10)
                .reduce((a, b) -> a + ", " + b).orElse("");

        String goals = String.join(", ", profile.getCareerGoals());

        String prompt = PROMPT.formatted(
                profile.getName(),
                skills,
                goals,
                job.getTitle(),
                job.getCompany(),
                job.getDescription().substring(0, Math.min(job.getDescription().length(), 1500)));

        log.info("Generating cover letter for: {} at {}", job.getTitle(), job.getCompany());
        return chatModel.generate(prompt);
    }
}
