package com.kittyp.clinic.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author rrohan419@gmail.com
 * ClinicContactOtpVerifyRequest
 * @param channel
 * @param email
 * @param phone
 * @param code
 * @param accessToken
 */
public record ClinicContactOtpVerifyRequest(
        @NotBlank String channel,
        String email,
        String phone,
        String code,
        String accessToken) {
}
