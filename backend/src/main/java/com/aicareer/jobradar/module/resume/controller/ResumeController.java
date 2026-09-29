package com.aicareer.jobradar.module.resume.controller;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.resume.model.Resume;
import com.aicareer.jobradar.module.resume.service.ResumeService;
import com.aicareer.jobradar.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Resume> upload(@RequestParam("file") MultipartFile file,
                                      @AuthenticationPrincipal User user) throws IOException {
        return ApiResponse.ok("Resume uploaded successfully", resumeService.upload(file, user));
    }

    @PostMapping("/{id}/extract-skills")
    public ApiResponse<Map<String, Object>> extractSkills(@PathVariable UUID id) {
        return ApiResponse.ok(resumeService.extractAndSaveSkills(id));
    }

    @GetMapping
    public ApiResponse<List<Resume>> list(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(resumeService.getResumesForUser(user.getId()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id,
                                             @AuthenticationPrincipal User user) {
        Resume resume = resumeService.getResumeForUser(id, user.getId());
        Resource resource = resumeService.serveFile(id, user.getId());
        String contentType = resume.getFileType() != null
                ? resume.getFileType() : "application/octet-stream";
        String filename = resume.getFileName() != null ? resume.getFileName() : "resume";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/{id}/profile-fields")
    public ApiResponse<Map<String, Object>> profileFields(@PathVariable UUID id,
                                                          @AuthenticationPrincipal User user) {
        return ApiResponse.ok(resumeService.getProfileFields(id, user.getId()));
    }
}
