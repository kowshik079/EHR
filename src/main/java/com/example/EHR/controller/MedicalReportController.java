package com.example.EHR.controller;

import com.example.EHR.controller.dto.MedicalReportResponse;
import com.example.EHR.model.MedicalReport;
import com.example.EHR.service.AadhaarEncryptionService;
import com.example.EHR.service.MedicalReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class MedicalReportController {

    private final MedicalReportService service;
    private final AadhaarEncryptionService aadhaarEncryptionService;

    public MedicalReportController(MedicalReportService service,
                                   AadhaarEncryptionService aadhaarEncryptionService) {
        this.service = service;
        this.aadhaarEncryptionService = aadhaarEncryptionService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<MedicalReportResponse> upload(@RequestPart("file") MultipartFile file,
                                                        @RequestParam("patientId") String patientId,
                                                        @RequestParam(value = "reportType", required = false) String reportType,
                                                        @RequestParam(value = "reportDate", required = false) String reportDate,
                                                        @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
                                                        Authentication auth) {
        String uploader = auth != null ? auth.getName()
                : (uploadedBy != null && !uploadedBy.isBlank() ? uploadedBy : "anonymous");

        LocalDateTime date = null;
        if (reportDate != null && !reportDate.isBlank()) {
            try {
                date = LocalDateTime.parse(reportDate);
            } catch (DateTimeParseException ignored) {
            }
        }

        MedicalReport saved = service.upload(file, uploader, patientId, reportType, date);
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<MedicalReportResponse>> listAll() {
        List<MedicalReportResponse> responses = service.listAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search-by-aadhaar/{aadhaar}")
    public ResponseEntity<List<MedicalReportResponse>> listByAadhaar(@PathVariable("aadhaar") String aadhaar,
                                                                     Authentication auth) {
        List<MedicalReportResponse> responses = service.listByPatientId(aadhaar).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        if (responses.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<List<MedicalReportResponse>> listByPatient(@PathVariable String patientId,
                                                                     Authentication auth) {
        boolean isPatient = hasRole(auth, "ROLE_PATIENT");
        if (isPatient && !auth.getName().equals(patientId)) {
            return ResponseEntity.status(403).build();
        }
        List<MedicalReportResponse> responses = service.listByPatientId(patientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        MedicalReport report = service.getById(id);
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        boolean isPatientOwner = hasRole(auth, "ROLE_PATIENT") && auth.getName().equals(report.getPatientId());

        if (!(isAdmin || isPatientOwner)) {
            return ResponseEntity.status(403).build();
        }

        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private MedicalReportResponse toResponse(MedicalReport report) {
        MedicalReportResponse resp = new MedicalReportResponse();
        resp.setId(report.getId());
        resp.setFileName(report.getFileName());
        resp.setOriginalFileName(report.getOriginalFileName());
        resp.setFileSize(report.getFileSize());
        resp.setMimeType(report.getMimeType());
        resp.setChecksum(report.getChecksum());
        resp.setExtractedText(report.getExtractedText());
        resp.setNormalizedText(report.getNormalizedText());
        resp.setUploadedAt(report.getUploadedAt());
        resp.setUploadedBy(report.getUploadedBy());
        resp.setPageCount(report.getPageCount());
        resp.setPatientId(report.getPatientId());
        resp.setReportType(report.getReportType());
        resp.setReportDate(report.getReportDate());

        if (report.getPatientId() != null) {
            try {
                String aadhaarPlain = aadhaarEncryptionService.decrypt(report.getPatientId());
                String masked = aadhaarEncryptionService.maskAadhaar(aadhaarPlain);
                resp.setMaskedAadhaar(masked);
            } catch (RuntimeException e) {
                resp.setMaskedAadhaar(null);
            }
        }

        return resp;
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
