/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

/**
 * @author rrohan419@gmail.com 
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EmailContact {

    @JsonProperty("email_address")
    private EmailAddress emailAddress;
}

