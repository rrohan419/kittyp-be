package com.kittyp.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequest {

	@NotBlank
	@Size(max = 120)
	private String name;

	@NotBlank
	@Email
	@Size(max = 254)
	private String email;

	@NotBlank
	@Size(max = 200)
	private String subject;

	@NotBlank
	@Size(max = 4000)
	private String message;
}
