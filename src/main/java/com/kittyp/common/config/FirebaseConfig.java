package com.kittyp.common.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service.account:}")
    private String firebaseServiceAccountInfoJson;

    @PostConstruct
    public void initialize() throws IOException {
        if (firebaseServiceAccountInfoJson == null || firebaseServiceAccountInfoJson.isBlank()) {
            log.warn("Firebase service account not configured; push notifications disabled");
            return;
        }

        // Replace escaped \n with real newlines
        firebaseServiceAccountInfoJson = firebaseServiceAccountInfoJson.replace("\\n", "\n");

        InputStream serviceAccount = new ByteArrayInputStream(
                firebaseServiceAccountInfoJson.getBytes(StandardCharsets.UTF_8));

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
