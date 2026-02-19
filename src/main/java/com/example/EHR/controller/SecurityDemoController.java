package com.example.EHR.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SecurityDemoController {

    @GetMapping("/public/ping")
    public ResponseEntity<?> publicPing() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/admin/ping")
    public ResponseEntity<?> adminPing() {
        return ResponseEntity.ok(Map.of("status", "admin ok"));
    }

    @GetMapping("/doctor/ping")
    public ResponseEntity<?> doctorPing() {
        return ResponseEntity.ok(Map.of("status", "doctor ok"));
    }

    @GetMapping("/diagnost/ping")
    public ResponseEntity<?> diagnostPing() {
        return ResponseEntity.ok(Map.of("status", "diagnost ok"));
    }

    @GetMapping("/patient/ping")
    public ResponseEntity<?> patientPing() {
        return ResponseEntity.ok(Map.of("status", "patient ok"));
    }

}
