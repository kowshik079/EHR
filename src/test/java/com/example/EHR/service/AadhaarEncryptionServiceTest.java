package com.example.EHR.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AadhaarEncryptionService.
 * Verifies encryption, decryption, and masking functionality.
 */
@SpringBootTest
class AadhaarEncryptionServiceTest {

    @Autowired
    private AadhaarEncryptionService encryptionService;

    private static final String VALID_AADHAAR = "123456789012";
    private static final String ANOTHER_AADHAAR = "987654321098";

    @Test
    void testEncryptAndDecrypt() {
        // Test basic encryption and decryption
        String encrypted = encryptionService.encrypt(VALID_AADHAAR);
        assertNotNull(encrypted);
        assertNotEquals(VALID_AADHAAR, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(VALID_AADHAAR, decrypted);
    }

    @Test
    void testEncryptionProducesDifferentCiphertexts() {
        // Each encryption should produce different ciphertext due to random IV
        String encrypted1 = encryptionService.encrypt(VALID_AADHAAR);
        String encrypted2 = encryptionService.encrypt(VALID_AADHAAR);

        assertNotEquals(encrypted1, encrypted2,
            "Same plaintext should produce different ciphertexts due to random IV");

        // But both should decrypt to the same value
        assertEquals(VALID_AADHAAR, encryptionService.decrypt(encrypted1));
        assertEquals(VALID_AADHAAR, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testDecryptDifferentAadhaarNumbers() {
        // Test that different Aadhaar numbers encrypt and decrypt correctly
        String encrypted1 = encryptionService.encrypt(VALID_AADHAAR);
        String encrypted2 = encryptionService.encrypt(ANOTHER_AADHAAR);

        assertEquals(VALID_AADHAAR, encryptionService.decrypt(encrypted1));
        assertEquals(ANOTHER_AADHAAR, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testMaskAadhaar() {
        String masked = encryptionService.maskAadhaar(VALID_AADHAAR);
        assertEquals("XXXX-XXXX-9012", masked);

        String masked2 = encryptionService.maskAadhaar(ANOTHER_AADHAAR);
        assertEquals("XXXX-XXXX-1098", masked2);
    }

    @Test
    void testFormatAadhaar() {
        String formatted = encryptionService.formatAadhaar(VALID_AADHAAR);
        assertEquals("1234-5678-9012", formatted);
    }

    @Test
    void testInvalidAadhaarFormat() {
        // Test with invalid formats
        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.encrypt("12345")); // Too short

        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.encrypt("12345678901234")); // Too long

        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.encrypt("12345678901A")); // Contains letter

        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.encrypt(null)); // Null
    }

    @Test
    void testInvalidEncryptedData() {
        // Test decryption with invalid data
        assertThrows(RuntimeException.class, () ->
            encryptionService.decrypt("invalid-base64-data"));

        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.decrypt(null));

        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.decrypt(""));
    }

    @Test
    void testDataIntegrity() {
        // Test that tampering with encrypted data is detected
        String encrypted = encryptionService.encrypt(VALID_AADHAAR);

        // Tamper with the encrypted data
        byte[] tamperedBytes = java.util.Base64.getDecoder().decode(encrypted);
        tamperedBytes[tamperedBytes.length - 1] ^= 0x01; // Flip one bit
        String tampered = java.util.Base64.getEncoder().encodeToString(tamperedBytes);

        // Decryption should fail due to authentication tag mismatch
        assertThrows(RuntimeException.class, () ->
            encryptionService.decrypt(tampered),
            "Tampered data should fail authentication");
    }

    @Test
    void testMaskInvalidAadhaar() {
        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.maskAadhaar("123"));

        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.maskAadhaar(null));
    }

    @Test
    void testFormatInvalidAadhaar() {
        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.formatAadhaar("123"));

        assertThrows(IllegalArgumentException.class, () ->
            encryptionService.formatAadhaar(null));
    }
}

