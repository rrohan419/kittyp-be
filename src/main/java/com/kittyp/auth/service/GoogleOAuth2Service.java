package com.kittyp.auth.service;

import java.net.URI;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.kittyp.auth.dto.GoogleUserInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2Service {

    private final RestClient restClient;

    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    public GoogleUserInfo getUserInfo(String accessToken) {
        try {
            // Using restClient.get() for GET request and setting Authorization header
            GoogleUserInfo userInfo = restClient.get()
                .uri(URI.create(GOOGLE_USER_INFO_URL))
                .headers(headers -> headers.setBearerAuth(accessToken)) // Set bearer token via headers consumer
                .retrieve() // Execute the request and retrieve the response
                .body(GoogleUserInfo.class); // Convert to GoogleUserInfo

            return userInfo;
        } catch (Exception e) {
            log.error("Error fetching user info from Google: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch user info from Google", e);
        }
    }
} 