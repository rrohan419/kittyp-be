/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class EventMessage {

	@JsonProperty("email_info")
    private EmailInfo emailInfo;

    @JsonProperty("event_data")
    private List<EventData> eventData;

    @JsonProperty("request_id")
    private String requestId;
}
