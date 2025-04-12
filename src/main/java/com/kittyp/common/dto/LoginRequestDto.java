package com.kittyp.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDto {

	@NotBlank
	@Email
    private String email;
    
    @NotBlank
    private String password;
}
