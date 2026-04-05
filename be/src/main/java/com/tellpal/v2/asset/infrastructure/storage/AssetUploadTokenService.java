package com.tellpal.v2.asset.infrastructure.storage;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.domain.MediaKind;
import com.tellpal.v2.asset.domain.StorageProvider;

/**
 * Stateless signer and verifier for asset upload tokens.
 */
@Component
public class AssetUploadTokenService {

    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public AssetUploadTokenService(
            Clock clock,
            ObjectMapper objectMapper,
            PrivateKey privateKey,
            PublicKey publicKey) {
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String issue(
            StorageProvider provider,
            String objectPath,
            MediaKind kind,
            String mimeType,
            long byteSize,
            Instant issuedAt,
            Instant expiresAt) {
        AssetUploadTokenClaims claims = new AssetUploadTokenClaims(
                provider,
                objectPath,
                kind,
                mimeType,
                byteSize,
                issuedAt,
                expiresAt);
        String payload = base64UrlEncode(serialize(claims));
        String signature = base64UrlEncode(sign(payload));
        return payload + "." + signature;
    }

    public AssetUploadTokenClaims verifyAndDecode(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Asset upload token must not be blank");
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Asset upload token format is invalid");
        }
        String payload = parts[0];
        String signature = parts[1];
        if (!verify(payload, decodeBase64Url(signature))) {
            throw new IllegalArgumentException("Asset upload token signature is invalid");
        }
        AssetUploadTokenClaims claims = deserialize(decodeBase64Url(payload));
        if (!claims.expiresAt().isAfter(Instant.now(clock))) {
            throw new IllegalArgumentException("Asset upload token has expired");
        }
        return claims;
    }

    private byte[] serialize(AssetUploadTokenClaims claims) {
        try {
            return objectMapper.writeValueAsBytes(new SerializedAssetUploadToken(
                    claims.provider().name(),
                    claims.objectPath(),
                    claims.kind().name(),
                    claims.mimeType(),
                    claims.byteSize(),
                    claims.issuedAt(),
                    claims.expiresAt()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Asset upload token payload could not be serialized", exception);
        }
    }

    private AssetUploadTokenClaims deserialize(byte[] bytes) {
        try {
            SerializedAssetUploadToken payload = objectMapper.readValue(bytes, SerializedAssetUploadToken.class);
            return new AssetUploadTokenClaims(
                    StorageProvider.valueOf(payload.provider()),
                    payload.objectPath(),
                    MediaKind.valueOf(payload.kind()),
                    payload.mimeType(),
                    payload.byteSize(),
                    payload.issuedAt(),
                    payload.expiresAt());
        } catch (RuntimeException | IOException exception) {
            throw new IllegalArgumentException("Asset upload token payload is invalid", exception);
        }
    }

    private byte[] sign(String payload) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Asset upload token could not be signed", exception);
        }
    }

    private boolean verify(String payload, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Asset upload token could not be verified", exception);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decodeBase64Url(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Asset upload token uses invalid base64url encoding", exception);
        }
    }
}

record SerializedAssetUploadToken(
        String provider,
        String objectPath,
        String kind,
        String mimeType,
        long byteSize,
        Instant issuedAt,
        Instant expiresAt) {
}
