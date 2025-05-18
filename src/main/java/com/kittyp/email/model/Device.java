/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Device {

    @JsonProperty("name")
    private String name;
}

