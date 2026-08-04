package com.kittyp.doctor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorChecklistUpdateRequest {
    private Boolean checkMobileOtp;
    private Boolean checkEmailOtp;
    private Boolean checkGovernmentId;
    private Boolean checkDegree;
    private Boolean checkRegistrationCertificate;
    private Boolean checkClinicAddress;
    private Boolean checkRegistrationNumber;
    private Boolean checkGoogleMapsMatch;
    private Boolean checkClinicPhotos;
}
