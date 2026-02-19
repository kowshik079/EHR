package com.example.EHR.controller;

import com.example.EHR.service.AadhaarEncryptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/aadhaar")
public class AadhaarController {

    private final AadhaarEncryptionService aadhaarEncryptionService;

    public AadhaarController(AadhaarEncryptionService aadhaarEncryptionService) {
        this.aadhaarEncryptionService = aadhaarEncryptionService;
    }

    @PostMapping("/encrypt")
    public ResponseEntity<?> encrypt(@RequestBody Map<String, String> body) {
        String aadhaar = body.get("aadhaar");
        if (aadhaar == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "aadhaar field is required"));
        }
        try {
            String encrypted = aadhaarEncryptionService.encrypt(aadhaar);
            String masked = aadhaarEncryptionService.maskAadhaar(aadhaar);
            return ResponseEntity.ok(Map.of(
                    "encrypted", encrypted,
                    "masked", masked
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Encryption failed: " + e.getMessage()));
        }
    }

    @PostMapping("/decrypt")
    public ResponseEntity<?> decrypt(@RequestBody Map<String, String> body) {
        String encrypted = body.get("encrypted");
        if (encrypted == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "encrypted field is required"));
        }
        try {
            String aadhaar = aadhaarEncryptionService.decrypt(encrypted);
            String formatted = aadhaarEncryptionService.formatAadhaar(aadhaar);
            return ResponseEntity.ok(Map.of(
                    "aadhaar", aadhaar,
                    "formatted", formatted
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Decryption failed: " + e.getMessage()));
        }
    }

    @PostMapping("/mask")
    public ResponseEntity<?> mask(@RequestBody Map<String, String> body) {
        String aadhaar = body.get("aadhaar");
        if (aadhaar == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "aadhaar field is required"));
        }
        try {
            String masked = aadhaarEncryptionService.maskAadhaar(aadhaar);
            return ResponseEntity.ok(Map.of("masked", masked));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
