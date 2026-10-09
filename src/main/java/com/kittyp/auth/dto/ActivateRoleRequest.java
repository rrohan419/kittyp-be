package com.kittyp.auth.dto;

import com.kittyp.common.enums.DoctorSpecialization;
import com.kittyp.common.enums.SignupRole;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Authenticated role activation. Password is intentionally absent so this
 * endpoint cannot change the existing login secret.
 */
@Getter
@Setter
public class ActivateRoleRequest {

	@NotNull
	private SignupRole role;

	/** If present, must match the signed-in email. */
	private String email;

	/**
	 * Password typed for the role being created. Stored only as a bcrypt hash
	 * on that role. Never copied onto users.password. Ignored when the role
	 * is already on the account.
	 */
	private String rolePassword;

	private String phoneNumber;
	private String licenseNumber;
	private String registrationNumber;
	private DoctorSpecialization specialization;
	private Double experience;
	private String professionalSummary;
	private String degreeCertificateUrl;
	private String registrationCertificateUrl;
	private String governmentIdUrl;
	private String photoUrl;
	private String inviteToken;

	private String clinicName;
	private String address;
	private String phone;
	private String timezone;
}
