package com.tellpal.v2.shared.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final String credentialsPath;
    private final String storageBucket;

    public FirebaseConfig(
            @Value("${app.firebase.credentials-path:}") String credentialsPath,
            @Value("${app.firebase.storage-bucket:tellpal-dev.appspot.com}") String storageBucket) {
        this.credentialsPath = credentialsPath;
        this.storageBucket = storageBucket;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options;

        if (credentialsPath != null && !credentialsPath.isBlank()
                && Files.exists(Paths.get(credentialsPath))) {
            log.info("Initializing Firebase with service account credentials from: {}", credentialsPath);
            try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(storageBucket)
                        .build();
            }
        } else {
            log.warn("Firebase credentials path not set or file not found. "
                    + "Attempting default credentials (dev/test mode).");
            try {
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setStorageBucket(storageBucket)
                        .build();
            } catch (IOException e) {
                log.warn("No Firebase credentials available. Firebase Storage will be unavailable. "
                        + "This is expected in dev/test environments.");
                // Return a minimal no-op FirebaseApp so the context starts
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                getClass().getResourceAsStream("/firebase-noop.json") != null
                                        ? getClass().getResourceAsStream("/firebase-noop.json")
                                        : InputStream.nullInputStream()))
                        .setStorageBucket(storageBucket)
                        .build();
                // We intentionally do NOT initialize FirebaseApp here to avoid errors;
                // FirebaseStorageService will handle the missing app gracefully.
                return null;
            }
        }

        return FirebaseApp.initializeApp(options);
    }
}
