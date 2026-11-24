package com.kittyp.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileOtpVerifyRequest {

    /** EMAIL, PHONE or WHATSAPP */
    @NotBlank
    private String channel;

    private String email;
    private String phone;
    private String accessToken;

    private String code;
}
