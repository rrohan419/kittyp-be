package com.kittyp.common.dto;

import com.kittyp.common.enums.DoctorSpecialization;

import lombok.Getter;
import lombok.Setter;

/**
 * Unified POST /auth/signup payload. Extra doctor/clinic fields are optional at
 * bean-validation time; the matching register* path enforces them when role requires it.
 */
@Getter
@Setter
public class PublicSignupRequestDto extends SignupRequestDto {

	private String phoneNumber;
	private String licenseNumber;
	private String registrationNumber;
	private DoctorSpecialization specialization;
	private Double experience;
	private String clinicName;
	private String clinicAddress;
	private String professionalSummary;
	private String degreeCertificateUrl;
	private String registrationCertificateUrl;
	private String governmentIdUrl;
	private String clinicPhotosUrls;
	private String photoUrl;
	private String inviteToken;
	private String address;
	private String phone;
	private String timezone;

	public SignupDoctorRequestDto toDoctorRequest() {
		SignupDoctorRequestDto dto = new SignupDoctorRequestDto();
		copyAccount(dto);
		dto.setPhoneNumber(phoneNumber);
		dto.setLicenseNumber(licenseNumber);
		dto.setRegistrationNumber(registrationNumber);
		dto.setSpecialization(specialization);
		dto.setExperience(experience);
		dto.setClinicName(clinicName);
		dto.setClinicAddress(clinicAddress);
		dto.setProfessionalSummary(professionalSummary);
		dto.setDegreeCertificateUrl(degreeCertificateUrl);
		dto.setRegistrationCertificateUrl(registrationCertificateUrl);
		dto.setGovernmentIdUrl(governmentIdUrl);
		dto.setClinicPhotosUrls(clinicPhotosUrls);
		dto.setPhotoUrl(photoUrl);
		dto.setInviteToken(inviteToken);
		return dto;
	}

	public SignupClinicRequestDto toClinicRequest() {
		SignupClinicRequestDto dto = new SignupClinicRequestDto();
		copyAccount(dto);
		dto.setClinicName(clinicName);
		dto.setLicenseNumber(licenseNumber);
		dto.setAddress(address);
		dto.setPhone(phone);
		dto.setTimezone(timezone);
		return dto;
	}

	private void copyAccount(SignupRequestDto target) {
		target.setFirstName(getFirstName());
		target.setLastName(getLastName());
		target.setEmail(getEmail());
		target.setPassword(getPassword());
		target.setRole(getRole());
		target.setRoles(getRoles());
	}
}
