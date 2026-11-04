package com.ojt.ecommerce.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacSignatureUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private HmacSignatureUtil() {
        // Utility class
    }

    /**
     * Computes the HMAC-SHA256 hexadecimal signature of the given payload using the secret key.
     */
    public static String generateSignature(String payload, String secret) {
        if (payload == null || secret == null) {
            throw new IllegalArgumentException("Payload and secret must not be null");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    public static String generateSignatureBase64(String payload, String secret) {
        if (payload == null || secret == null) {
            throw new IllegalArgumentException("Payload and secret must not be null");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", e);
        }
    }

    /**
     * Verifies whether the received signature matches the expected HMAC-SHA256 signature (Hex or Base64).
     */
    public static boolean verifySignature(String payload, String secret, String receivedSignature) {
        if (payload == null || secret == null || receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }
        try {
            String expectedHex = generateSignature(payload, secret);
            if (MessageDigest.isEqual(
                    expectedHex.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.trim().getBytes(StandardCharsets.UTF_8)
            )) {
                return true;
            }

            String expectedBase64 = generateSignatureBase64(payload, secret);
            return MessageDigest.isEqual(
                    expectedBase64.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
