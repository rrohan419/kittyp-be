package com.kittyp.common.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignupRequestDto {

//	@NotBlank
//	@Size(min = 3, max = 20)
//	private String username;

	@NotBlank
	@Size(min = 2)
	private String firstName;
	
	private String lastName;
	
	@NotBlank
	@Size(max = 50)
	@Email
	private String email;

	private Set<String> roles;

	@NotBlank
	@Size(min = 6, max = 40)
	private String password;
}
