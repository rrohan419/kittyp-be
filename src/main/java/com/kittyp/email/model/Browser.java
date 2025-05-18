/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Browser {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;
}

