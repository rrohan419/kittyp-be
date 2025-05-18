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
public class ZeptoWebhookEventRequest {

	@JsonProperty("event_name")
    private List<String> eventName;

    @JsonProperty("event_message")
    private List<EventMessage> eventMessage;

    @JsonProperty("mailagent_key")
    private String mailAgentKey;

    @JsonProperty("webhook_request_id")
    private String webhookRequestId;
}
