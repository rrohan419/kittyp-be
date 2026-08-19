package com.kittyp.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class LoginRequestDto {

	/** Email, user public id, or doctor public id. */
	@NotBlank
	@Size(max = 255)
    private String email;
    
    @NotBlank
    private String password;
}
