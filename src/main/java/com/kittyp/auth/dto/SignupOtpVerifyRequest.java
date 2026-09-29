package com.kittyp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupOtpVerifyRequest {

    @NotBlank
    private String channel;

    private String email;
    private String phone;

    private String code;

    /** MSG91 widget JWT returned after the WhatsApp OTP is verified in the widget. */
    private String accessToken;
}
