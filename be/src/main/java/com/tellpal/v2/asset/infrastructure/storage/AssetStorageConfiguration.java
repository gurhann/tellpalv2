package com.tellpal.v2.asset.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires asset-module-owned Firebase storage beans and test doubles.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AssetStorageFirebaseProperties.class)
public class AssetStorageConfiguration {

    @Bean
    AssetStorageClient firebaseAssetStorageClient(AssetStorageFirebaseProperties properties) {
        if (properties.fakeClientEnabled()) {
            return new FakeFirebaseStorageAssetClient(properties);
        }
        ServiceAccountCredentials credentials = loadServiceAccountCredentials(properties);
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(requireText(properties.projectId(), "Firebase Storage project ID must not be blank"))
                .setCredentials(credentials)
                .build()
                .getService();
        return new FirebaseStorageAssetClient(storage, credentials, properties);
    }

    @Bean
    PrivateKey assetUploadTokenPrivateKey(AssetStorageFirebaseProperties properties) {
        if (properties.fakeClientEnabled()) {
            return generateTestKeyPair().getPrivate();
        }
        return loadServiceAccountCredentials(properties).getPrivateKey();
    }

    @Bean
    PublicKey assetUploadTokenPublicKey(AssetStorageFirebaseProperties properties, PrivateKey assetUploadTokenPrivateKey) {
        if (properties.fakeClientEnabled()) {
            return generatePublicKey(assetUploadTokenPrivateKey);
        }
        return generatePublicKey(assetUploadTokenPrivateKey);
    }

    private static ServiceAccountCredentials loadServiceAccountCredentials(AssetStorageFirebaseProperties properties) {
        Path credentialsPath = Path.of(requireText(
                properties.credentialsPath(),
                "Firebase Storage credentials path must not be blank"));
        try (InputStream inputStream = Files.newInputStream(credentialsPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            if (credentials instanceof ServiceAccountCredentials serviceAccountCredentials) {
                return serviceAccountCredentials;
            }
            throw new IllegalStateException("Firebase Storage credentials must be a service account JSON file");
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase Storage credentials could not be loaded", exception);
        }
    }

    private static PublicKey generatePublicKey(PrivateKey privateKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey rsaPrivateKey)) {
            throw new IllegalStateException("Asset upload token signing requires an RSA private key");
        }
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPublicExponent()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Asset upload token public key could not be derived", exception);
        }
    }

    private static KeyPair generateTestKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA key pair generation is not available", exception);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
