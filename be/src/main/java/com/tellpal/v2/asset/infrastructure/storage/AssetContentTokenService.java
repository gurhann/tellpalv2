package com.tellpal.v2.asset.infrastructure.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetContentAccessToken;
import com.tellpal.v2.asset.domain.StorageProvider;

/**
 * Stateless signer and verifier for short-lived backend content tokens.
 */
@Component
public class AssetContentTokenService {

    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final AssetStorageFirebaseProperties properties;

    public AssetContentTokenService(
            Clock clock,
            ObjectMapper objectMapper,
            PrivateKey privateKey,
            PublicKey publicKey,
            AssetStorageFirebaseProperties properties) {
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.properties = properties;
    }

    public AssetContentAccessToken issue(Long assetId, StorageProvider provider, String objectPath) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(properties.backendContentTtl());
        AssetContentTokenClaims claims = new AssetContentTokenClaims(
                assetId,
                provider,
                objectPath,
                issuedAt,
                expiresAt);
        String payload = base64UrlEncode(serialize(claims));
        String signature = base64UrlEncode(sign(payload));
        return new AssetContentAccessToken(payload + "." + signature, expiresAt);
    }

    public AssetContentTokenClaims verifyAndDecode(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Asset content token must not be blank");
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Asset content token format is invalid");
        }
        String payload = parts[0];
        String signature = parts[1];
        if (!verify(payload, decodeBase64Url(signature))) {
            throw new IllegalArgumentException("Asset content token signature is invalid");
        }
        AssetContentTokenClaims claims = deserialize(decodeBase64Url(payload));
        if (!claims.expiresAt().isAfter(Instant.now(clock))) {
            throw new IllegalArgumentException("Asset content token has expired");
        }
        return claims;
    }

    private byte[] serialize(AssetContentTokenClaims claims) {
        try {
            return objectMapper.writeValueAsBytes(new SerializedAssetContentToken(
                    claims.assetId(),
                    claims.provider().name(),
                    claims.objectPath(),
                    claims.issuedAt(),
                    claims.expiresAt()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Asset content token payload could not be serialized", exception);
        }
    }

    private AssetContentTokenClaims deserialize(byte[] bytes) {
        try {
            SerializedAssetContentToken payload = objectMapper.readValue(bytes, SerializedAssetContentToken.class);
            return new AssetContentTokenClaims(
                    payload.assetId(),
                    StorageProvider.valueOf(payload.provider()),
                    payload.objectPath(),
                    payload.issuedAt(),
                    payload.expiresAt());
        } catch (RuntimeException | IOException exception) {
            throw new IllegalArgumentException("Asset content token payload is invalid", exception);
        }
    }

    private byte[] sign(String payload) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Asset content token could not be signed", exception);
        }
    }

    private boolean verify(String payload, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Asset content token could not be verified", exception);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decodeBase64Url(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Asset content token uses invalid base64url encoding", exception);
        }
    }
}

record SerializedAssetContentToken(
        Long assetId,
        String provider,
        String objectPath,
        Instant issuedAt,
        Instant expiresAt) {
}
