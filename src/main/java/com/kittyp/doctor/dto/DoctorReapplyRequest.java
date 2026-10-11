package com.kittyp.doctor.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorReapplyRequest {
    @Size(max = 100)
    private String registrationNumber;
    @Size(max = 2048)
    private String degreeCertificateUrl;
    @Size(max = 2048)
    private String registrationCertificateUrl;
    @Size(max = 2048)
    private String governmentIdUrl;
}
