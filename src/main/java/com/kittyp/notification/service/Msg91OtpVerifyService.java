package com.kittyp.notification.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class Msg91OtpVerifyService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${msg91.auth.key}")
    private String authKey;

    @Value("${msg91.verify-access-token-url}")
    private String verifyAccessTokenUrl;

    public boolean verifyOtp(String accessToken){
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        if (authKey == null || authKey.isBlank()) {
            throw new CustomException("MSG91 auth key is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }

        try {
            String response = restClient.post()
                    .uri(verifyAccessTokenUrl)
                    .body(Map.of(
                            "authkey", authKey,
                            "access-token", accessToken.trim()))
                    .retrieve()
                    .body(String.class);

            JsonNode result = objectMapper.readTree(response == null ? "{}" : response);
            return isSuccessful(result);
        } catch (RestClientResponseException e) {
            throw new CustomException("MSG91 OTP verification failed", HttpStatus.BAD_GATEWAY, e);
        } catch (Exception e) {
            throw new CustomException("MSG91 OTP verification failed", HttpStatus.BAD_GATEWAY, e);
        }
    }

    private boolean isSuccessful(JsonNode result) {
        if (result.path("status").isBoolean()) {
            return result.path("status").asBoolean();
        }
        if (result.path("type").isTextual()) {
            return "success".equalsIgnoreCase(result.path("type").asText());
        }
        return false;
    }
}
