package com.kittyp.clinic.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author rrohan419@gmail.com
 * ClinicContactOtpSendRequest
 * @param channel
 * @param email
 * @param phone
 */
public record ClinicContactOtpSendRequest(
        @NotBlank String channel,
        String email,
        String phone) {
}
