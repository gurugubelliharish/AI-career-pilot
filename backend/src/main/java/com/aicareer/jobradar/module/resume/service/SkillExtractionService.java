package com.aicareer.jobradar.module.resume.service;

import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillExtractionService {

    private final OllamaChatModel chatModel;

    private static final String SKILL_EXTRACTION_PROMPT = """
            You are an expert resume analyst. Extract all technical and professional skills from the resume text below.
            Include both explicitly mentioned skills AND skills that can be inferred from project descriptions and work experience.

            Return ONLY a JSON array of skill names. Example: ["Java", "Spring Boot", "Docker", "PostgreSQL"]

            Resume Text:
            %s
            """;

    /**
     * Extracts skills from resume text using the local Ollama model.
     * Returns raw JSON string — parse with ObjectMapper in the caller.
     */
    public String extractSkillsAsJson(String resumeText) {
        String prompt = SKILL_EXTRACTION_PROMPT.formatted(resumeText);
        log.debug("Extracting skills from resume text ({} chars)", resumeText.length());
        return chatModel.generate(prompt);
    }

    /**
     * Infers skills from a single work experience or project description.
     */
    public String inferSkillsFromDescription(String description) {
        String prompt = """
                Extract technical skills from this work experience or project description.
                Return ONLY a JSON array: ["skill1", "skill2"]

                Description: %s
                """.formatted(description);
        return chatModel.generate(prompt);
    }
}
