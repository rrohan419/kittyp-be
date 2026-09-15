package com.kittyp.email.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachment {

	private String content;

	@JsonProperty("mime_type")
	private String mimeType;

	private String name;
}
