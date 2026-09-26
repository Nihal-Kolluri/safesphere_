package com.safesphere.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cryptographic Evidence Vault (M4).
 * Enforces tamper-evident chain of custody using AES-256-GCM and SHA-256 digital digests.
 */
public class EvidenceVault {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12; // 96 bits recommended for GCM
    private static final int KEY_LENGTH_BITS = 256;

    private final SecretKey masterKey;
    private final SecureRandom secureRandom;

    /**
     * Initializes EvidenceVault with a newly generated cryptographically secure AES-256 key.
     */
    public EvidenceVault() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_LENGTH_BITS, new SecureRandom());
            this.masterKey = keyGen.generateKey();
            this.secureRandom = new SecureRandom();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EvidenceVault AES-256 engine", e);
        }
    }

    /**
     * Initializes EvidenceVault with an existing raw key or passphrase derivative.
     */
    public EvidenceVault(byte[] keyBytes) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 key must be exactly 32 bytes (256 bits)");
        }
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Output format: Base64([ 12-byte IV ] + [ Ciphertext + 16-byte Auth Tag ])
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Evidence encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts ciphertext using AES-256-GCM and verifies authenticity tag.
     * Throws an exception if the data has been altered or tampered with.
     */
    public String decrypt(String base64Encrypted) {
        if (base64Encrypted == null || base64Encrypted.isEmpty()) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Encrypted);
            if (decoded.length < IV_LENGTH_BYTES + 16) {
                throw new IllegalArgumentException("Corrupted payload: insufficient length for IV + Tag");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("Cryptographic verification failed: data is tampered or invalid key", e);
        }
    }

    /**
     * Computes a SHA-256 checksum for digital evidence immutability.
     */
    public static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    /**
     * Packages a complete digital evidence payload (simulated audio clip, camera snapshot, sensor telemetry).
     */
    public String packageEvidence(String capsuleId, String sensorData, String medicalInfo, String audioSample) {
        long timestamp = System.currentTimeMillis();
        String rawEvidence = String.format(
                "{\"capsule_id\":\"%s\",\"timestamp\":%d,\"sensors\":\"%s\",\"medical\":\"%s\",\"audio_proof\":\"%s\",\"sha256\":\"%s\"}",
                capsuleId, timestamp, sensorData, medicalInfo, audioSample,
                computeSha256((capsuleId + timestamp + sensorData).getBytes(StandardCharsets.UTF_8))
        );
        return encrypt(rawEvidence);
    }

    public byte[] getRawKeyBytes() {
        return masterKey.getEncoded();
    }
}
