package com.example.EHR.service;

import com.example.EHR.model.MedicalReport;
import com.example.EHR.repository.MedicalReportRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class MedicalReportService {

    private final MedicalReportRepository repository;
    private final long maxFileSize;
    private final AadhaarEncryptionService aadhaarEncryptionService;

    public MedicalReportService(MedicalReportRepository repository,
                                AadhaarEncryptionService aadhaarEncryptionService,
                                @Value("${app.upload.max-size:52428800}") long maxFileSize) {
        this.repository = repository;
        this.aadhaarEncryptionService = aadhaarEncryptionService;
        this.maxFileSize = maxFileSize;
    }

    public MedicalReport upload(MultipartFile file, String uploadedBy, String patientId, String reportType, LocalDateTime reportDate) {
        // Basic validation
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File too large. Max size: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Encrypt patientId (which is Aadhaar) before storing
        String encryptedPatientId = null;
        String patientIdHash = null;
        if (patientId != null && !patientId.isBlank()) {
            encryptedPatientId = aadhaarEncryptionService.encrypt(patientId);
            patientIdHash = aadhaarEncryptionService.hashAadhaar(patientId);
        }


        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        String checksum = sha256(file);
        String extracted = "";
        int pageCount = 0;

        try {
            byte[] bytes = file.getBytes();
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                pageCount = doc.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                extracted = stripper.getText(doc);
            }
        } catch (Exception e) {
            // If PDF parsing fails, still save the file but with empty text
            extracted = "";
            pageCount = 0;
        }

        String normalized = normalizeText(extracted);

        MedicalReport report = new MedicalReport();
        report.setOriginalFileName(filename);
        report.setFileName(filename);
        report.setFileSize(file.getSize());
        report.setMimeType("application/pdf");
        report.setChecksum(checksum);
        report.setExtractedText(extracted);
        report.setNormalizedText(normalized);
        report.setUploadedBy(uploadedBy);
        report.setPageCount(pageCount);
        report.setPatientId(encryptedPatientId);
        report.setPatientIdHash(patientIdHash);
        report.setReportType(reportType);
        report.setReportDate(reportDate);

        return repository.save(report);
    }

    public MedicalReport getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<MedicalReport> listByPatientId(String aadhaarPlain) {
        String patientIdHash = aadhaarEncryptionService.hashAadhaar(aadhaarPlain);
        return repository.findByPatientIdHash(patientIdHash);
    }


    public List<MedicalReport> listAll() {
        return repository.findAll();
    }

    private String sha256(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        String s = text.replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2212', '-')
                .replace('\r', ' ')
                .replace('\t', ' ');
        s = s.replaceAll("[ ]+", " ").trim();
        return s;
    }
}
