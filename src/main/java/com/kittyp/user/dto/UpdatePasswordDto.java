/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * @author rrohan419@gmail.com 
 */
@Getter
public class UpdatePasswordDto {

	@NotBlank
	@Email
	private String email;

	@NotBlank
	@Size(min = 6, max = 6)
	private String code;

	@NotBlank
	@Size(min = 8, max = 72)
	@Pattern(
			regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
			message = "Password must be 8-72 chars with upper, lower, digit, and special character")
	private String password;
}
