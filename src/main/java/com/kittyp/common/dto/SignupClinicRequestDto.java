package com.kittyp.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupClinicRequestDto extends SignupRequestDto {

    @NotBlank
    private String clinicName;

    private String licenseNumber;
    private String address;
    private String phone;
    private String timezone;
}
