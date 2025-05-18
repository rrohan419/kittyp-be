/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReplyTo {

    @JsonProperty("address")
    private String address;

    @JsonProperty("name")
    private String name;
}

