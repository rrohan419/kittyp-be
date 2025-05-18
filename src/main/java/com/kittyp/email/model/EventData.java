/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EventData {

    @JsonProperty("details")
    private List<Detail> details;

    @JsonProperty("object")
    private String object;
}

