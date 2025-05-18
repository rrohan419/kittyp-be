/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.dto;

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
}
