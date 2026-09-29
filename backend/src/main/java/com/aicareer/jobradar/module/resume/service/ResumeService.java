package com.aicareer.jobradar.module.resume.service;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.resume.model.Resume;
import com.aicareer.jobradar.module.resume.repository.ResumeRepository;
import com.aicareer.jobradar.shared.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParserService parserService;
    private final SkillExtractionService skillExtractionService;
    private final ObjectMapper objectMapper;

    @Value("${app.resume.upload-dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES =
            List.of("application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    @Transactional
    public Resume upload(MultipartFile file, User user) throws IOException {
        validateFile(file);

        String savedPath = saveFileToDisk(file, user.getId());
        String rawText   = parserService.extractText(file);

        Resume resume = Resume.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .filePath(savedPath)
                .fileType(file.getContentType())
                .rawText(rawText)
                .build();

        resumeRepository.save(resume);
        return resume;
    }

    @Transactional
    public Map<String, Object> extractAndSaveSkills(UUID resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> AppException.notFound("Resume not found"));

        String skillsJson = skillExtractionService.extractSkillsAsJson(resume.getRawText());

        try {
            Map<String, Object> parsedData = Map.of("skills", objectMapper.readValue(skillsJson, List.class));
            resume.setParsedData(parsedData);
            resume.setProcessed(true);
            resumeRepository.save(resume);
            return parsedData;
        } catch (Exception e) {
            log.error("Failed to parse skills JSON for resume {}", resumeId, e);
            throw AppException.badRequest("Failed to extract skills from resume");
        }
    }

    public List<Resume> getResumesForUser(UUID userId) {
        return resumeRepository.findByUserIdAndActiveTrue(userId);
    }

    public Resume getResumeForUser(UUID resumeId, UUID userId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> AppException.notFound("Resume not found"));
    }

    public Resource serveFile(UUID resumeId, UUID userId) {
        Resume resume = getResumeForUser(resumeId, userId);
        Path filePath = Paths.get(resume.getFilePath());
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            throw AppException.notFound("Resume file not found on disk");
        }
        return resource;
    }

    public Map<String, Object> getProfileFields(UUID resumeId, UUID userId) {
        Resume resume = getResumeForUser(resumeId, userId);
        Map<String, Object> result = new HashMap<>();
        String text = resume.getRawText();
        if (text == null || text.isBlank()) return result;

        extractEmail(text).ifPresent(v -> result.put("email", v));
        extractPhone(text).ifPresent(v -> result.put("phone", v));
        extractLinkedIn(text).ifPresent(v -> result.put("linkedinUrl", v));
        extractGitHub(text).ifPresent(v -> result.put("githubUrl", v));
        extractName(text).ifPresent(v -> result.put("name", v));

        // Include skills from parsedData
        if (resume.getParsedData() != null && resume.getParsedData().containsKey("skills")) {
            result.put("skills", resume.getParsedData().get("skills"));
        }

        return result;
    }

    private java.util.Optional<String> extractEmail(String text) {
        Matcher m = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
                .matcher(text);
        return m.find() ? java.util.Optional.of(m.group().trim()) : java.util.Optional.empty();
    }

    private java.util.Optional<String> extractPhone(String text) {
        Matcher m = Pattern.compile("(?:\\+?\\d{1,3}[\\s\\-]?)?(?:\\(?\\d{3}\\)?[\\s\\-]?)?\\d{3}[\\s\\-]?\\d{4}")
                .matcher(text);
        return m.find() ? java.util.Optional.of(m.group().trim()) : java.util.Optional.empty();
    }

    private java.util.Optional<String> extractLinkedIn(String text) {
        Matcher m = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?linkedin\\.com/in/[\\w\\-]+")
                .matcher(text);
        if (!m.find()) return java.util.Optional.empty();
        String url = m.group().trim();
        if (!url.startsWith("http")) url = "https://" + url;
        return java.util.Optional.of(url);
    }

    private java.util.Optional<String> extractGitHub(String text) {
        Matcher m = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?github\\.com/[\\w\\-]+")
                .matcher(text);
        if (!m.find()) return java.util.Optional.empty();
        String url = m.group().trim();
        if (!url.startsWith("http")) url = "https://" + url;
        return java.util.Optional.of(url);
    }

    private java.util.Optional<String> extractName(String text) {
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && t.length() >= 3 && t.length() <= 60
                    && !t.contains("@") && !t.contains("http")
                    && !t.matches(".*\\d{4}.*")) {  // skip lines with years
                return java.util.Optional.of(t);
            }
        }
        return java.util.Optional.empty();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw AppException.badRequest("File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw AppException.badRequest("Only PDF and DOCX files are supported");
        }
    }

    private String saveFileToDisk(MultipartFile file, UUID userId) throws IOException {
        Path dir = Paths.get(uploadDir, userId.toString());
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dest = dir.resolve(filename);
        file.transferTo(dest);
        return dest.toString();
    }
}
