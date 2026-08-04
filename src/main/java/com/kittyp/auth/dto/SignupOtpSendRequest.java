package com.kittyp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupOtpSendRequest {

    /** EMAIL or PHONE */
    @NotBlank
    private String channel;

    private String email;
    private String phone;
}
