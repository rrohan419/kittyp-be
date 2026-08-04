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

    @NotBlank
    private String code;
}
