package com.tellpal.v2.purchase.infrastructure.revenuecat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class RevenueCatSignatureValidator {

    private final String webhookSecret;

    public RevenueCatSignatureValidator(
            @Value("${revenuecat.webhook.secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean validate(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            // Signature validation disabled in dev/test environments
            return true;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hmacBytes);
            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signatureHeader != null
                            ? signatureHeader.getBytes(StandardCharsets.UTF_8)
                            : new byte[0]);
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
