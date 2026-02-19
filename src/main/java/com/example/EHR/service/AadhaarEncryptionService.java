package com.example.EHR.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AadhaarEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_SIZE = 256;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AadhaarEncryptionService(@Value("${aadhaar.encryption.key:#{null}}") String base64Key) {
        try {
            if (base64Key != null && !base64Key.isEmpty()) {
                byte[] decodedKey = Base64.getDecoder().decode(base64Key);
                this.secretKey = new SecretKeySpec(decodedKey, "AES");
            } else {
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(AES_KEY_SIZE);
                this.secretKey = keyGenerator.generateKey();
                System.out.println("Generated AES Key (Base64): " +
                    Base64.getEncoder().encodeToString(secretKey.getEncoded()));
            }
            this.secureRandom = new SecureRandom();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption service", e);
        }
    }

    public String encrypt(String aadhaarNumber) {
        try {
            if (aadhaarNumber == null || !aadhaarNumber.matches("\\d{12}")) {
                throw new IllegalArgumentException("Invalid Aadhaar number format. Must be 12 digits.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = aadhaarNumber.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintext);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String hashAadhaar(String aadhaarNumber) {
        try {
            if (aadhaarNumber == null || !aadhaarNumber.matches("\\d{12}")) {
                throw new IllegalArgumentException("Invalid Aadhaar number format. Must be 12 digits.");
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(aadhaarNumber.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash Aadhaar", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            if (encryptedData == null || encryptedData.isEmpty()) {
                throw new IllegalArgumentException("Encrypted data cannot be null or empty");
            }

            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed. Data may be corrupted or tampered with.", e);
        }
    }

    public String maskAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null || !aadhaarNumber.matches("\\d{12}")) {
            throw new IllegalArgumentException("Invalid Aadhaar number format. Must be 12 digits.");
        }

        String lastFour = aadhaarNumber.substring(8);
        return "XXXX-XXXX-" + lastFour;
    }

    public String formatAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null || !aadhaarNumber.matches("\\d{12}")) {
            throw new IllegalArgumentException("Invalid Aadhaar number format. Must be 12 digits.");
        }

        return aadhaarNumber.substring(0, 4) + "-" +
               aadhaarNumber.substring(4, 8) + "-" +
               aadhaarNumber.substring(8);
    }

    public String getEncodedKey() {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}
