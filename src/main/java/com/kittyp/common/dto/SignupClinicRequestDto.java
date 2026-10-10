package com.kittyp.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupClinicRequestDto extends SignupRequestDto {

    @NotBlank
    private String clinicName;

    private String licenseNumber;
    private String address;
    private String city;
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;
    @Size(max = 512)
    private String googlePlaceId;
    private String phone;
    private String timezone;
}
