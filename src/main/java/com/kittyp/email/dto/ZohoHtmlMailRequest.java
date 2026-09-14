package com.kittyp.email.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ZohoHtmlMailRequest implements IEmailDto {

	private EmailAddress from;
	private List<Recipient> to;
	private String subject;

	@JsonProperty("htmlbody")
	private String htmlBody;
}
