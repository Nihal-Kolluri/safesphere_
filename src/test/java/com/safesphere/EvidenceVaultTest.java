package com.safesphere;

import com.safesphere.crypto.EvidenceVault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EvidenceVaultTest {
    private EvidenceVault vault;

    @BeforeEach
    void setUp() {
        vault = new EvidenceVault();
    }

    @Test
    @DisplayName("AES-256-GCM encryption and decryption roundtrip preserves data integrity")
    void testEncryptionDecryptionRoundtrip() {
        String originalData = "{\"capsule_id\":\"CR-8924\",\"medical\":\"Blood: O+\",\"lat\":17.3850}";
        String encrypted = vault.encrypt(originalData);

        assertNotNull(encrypted);
        assertNotEquals(originalData, encrypted);

        String decrypted = vault.decrypt(encrypted);
        assertEquals(originalData, decrypted);
    }

    @Test
    @DisplayName("Tampering with ciphertext causes GCM authentication tag check to fail")
    void testTamperDetection() {
        String data = "CRITICAL_EVIDENCE_PAYLOAD";
        String encrypted = vault.encrypt(data);

        // Tamper with the encrypted base64 payload
        byte[] rawBytes = java.util.Base64.getDecoder().decode(encrypted);
        rawBytes[rawBytes.length - 2] ^= 0x5A; // flip bits in the auth tag or ciphertext
        String tampered = java.util.Base64.getEncoder().encodeToString(rawBytes);

        assertThrows(SecurityException.class, () -> {
            vault.decrypt(tampered);
        }, "Tampered ciphertext must be rejected by AES-256-GCM authentication verification");
    }

    @Test
    @DisplayName("Evidence packaging computes deterministic SHA-256 integrity checksum")
    void testSha256Checksum() {
        byte[] payload = "TELEMETRY_STREAM_DATA".getBytes();
        String hash1 = EvidenceVault.computeSha256(payload);
        String hash2 = EvidenceVault.computeSha256(payload);

        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }
}
