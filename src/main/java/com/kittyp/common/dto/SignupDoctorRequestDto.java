package com.kittyp.common.dto;

import com.kittyp.common.enums.DoctorSpecialization;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupDoctorRequestDto extends SignupRequestDto {

    private String phoneNumber;

    private String licenseNumber;

    @NotBlank
    private String registrationNumber;

    private DoctorSpecialization specialization;
    private Double experience;
    /** Ignored on doctor signup — clinics register separately. Kept for backward-compatible payloads. */
    private String clinicName;
    /** Ignored on doctor signup — clinics register separately. */
    private String clinicAddress;
    private String professionalSummary;

    @NotBlank
    private String degreeCertificateUrl;

    @NotBlank
    private String registrationCertificateUrl;

    private String governmentIdUrl;
    /** Ignored on doctor signup — clinic photos belong to clinic verification. */
    private String clinicPhotosUrls;
    private String photoUrl;

    /** Optional clinic invite token — joins that clinic instead of creating a new one. */
    private String inviteToken;
}
