package com.aicareer.jobradar.module.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ResumeParserService {

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if ("application/pdf".equals(contentType)) {
            return extractFromPdf(file.getInputStream());
        }

        return extractWithTika(file.getInputStream());
    }

    private String extractFromPdf(InputStream inputStream) throws IOException {
        // PDFBox 3.x: Loader.loadPDF(byte[]) replaces the old PDDocument.load(InputStream)
        try (PDDocument doc = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String extractWithTika(InputStream inputStream) throws IOException {
        try {
            return tika.parseToString(inputStream);
        } catch (org.apache.tika.exception.TikaException e) {
            throw new IOException("Tika failed to parse document: " + e.getMessage(), e);
        }
    }
}
