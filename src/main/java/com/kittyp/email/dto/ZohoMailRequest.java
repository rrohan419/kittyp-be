/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class ZohoMailRequest implements IEmailDto {

    @JsonProperty("template_key")
    private String templateKey;

    @JsonProperty("bounce_address")
    private String bounceAddress;

    private EmailAddress from;

    private List<Recipient> to;
    private List<Recipient> cc;
    private List<Recipient> bcc;

    @JsonProperty("merge_info")
    private Map<String, Object> mergeInfo;

    @JsonProperty("reply_to")
    private List<EmailAddress> replyTo;

    @JsonProperty("client_reference")
    private String clientReference;

    @JsonProperty("mime_headers")
    private Map<String, String> mimeHeaders;
}
