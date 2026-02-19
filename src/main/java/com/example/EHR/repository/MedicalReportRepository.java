package com.example.EHR.repository;

import com.example.EHR.model.MedicalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalReportRepository extends JpaRepository<MedicalReport, Long> {

    List<MedicalReport> findByPatientId(String patientId);

    Optional<MedicalReport> findByChecksum(String checksum);

    List<MedicalReport> findByReportType(String reportType);

    List<MedicalReport> findByUploadedBy(String uploadedBy);
    List<MedicalReport> findByPatientIdHash(String patientIdHash);

}
