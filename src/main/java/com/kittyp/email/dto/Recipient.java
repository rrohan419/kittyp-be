/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
@AllArgsConstructor
public class Recipient {
	@JsonProperty(value = "email_address")
    private EmailAddress emailAddress;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("merge_info")
	private Map<String, Object> mergeInfo;

	public Recipient(EmailAddress emailAddress) {
		this.emailAddress = emailAddress;
	}
}
