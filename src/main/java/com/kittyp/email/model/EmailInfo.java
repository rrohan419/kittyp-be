/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class EmailInfo {

    @JsonProperty("cc")
    private List<EmailContact> cc;

    @JsonProperty("client_reference")
    private String clientReference;

    @JsonProperty("bcc")
    private List<EmailContact> bcc;

    @JsonProperty("is_smtp_trigger")
    private boolean smtpTrigger;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("bounce_address")
    private String bounceAddress;

    @JsonProperty("email_reference")
    private String emailReference;

    @JsonProperty("reply_to")
    private List<ReplyTo> replyTo;

    @JsonProperty("from")
    private From from;

    @JsonProperty("to")
    private List<EmailContact> to;

    @JsonProperty("tag")
    private String tag;

    @JsonProperty("processed_time")
    private String processedTime;

    @JsonProperty("object")
    private String object;
}

