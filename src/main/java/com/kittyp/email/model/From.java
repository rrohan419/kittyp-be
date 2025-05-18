/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class From {

    @JsonProperty("address")
    private String address;

    @JsonProperty("name")
    private String name;
}

