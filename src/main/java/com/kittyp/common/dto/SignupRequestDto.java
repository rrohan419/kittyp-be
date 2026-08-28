package com.kittyp.common.dto;

import java.util.Set;

import com.kittyp.common.enums.SignupRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDto {

	@NotBlank
	@Size(min = 2, max = 50)
	private String firstName;

	@Size(max = 50)
	private String lastName;

	@NotBlank
	@Size(max = 50)
	@Email
	private String email;

	/**
	 * Allowlisted public signup role. Defaults to USER when omitted.
	 * Unknown values fail Jackson deserialization (400).
	 */
	private SignupRole role = SignupRole.USER;

	/** Ignored server-side. Kept for API compatibility — never used for provisioning. */
	private Set<String> roles;

	@NotBlank
	@Size(min = 8, max = 72)
	@Pattern(
			regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
			message = "Password must be 8-72 chars with upper, lower, digit, and special character")
	private String password;
}
