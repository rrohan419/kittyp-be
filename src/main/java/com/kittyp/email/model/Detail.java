/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Detail {

    @JsonProperty("email_client")
    private EmailClient emailClient;

    @JsonProperty("modified_time")
    private String modifiedTime;

    @JsonProperty("ip_location_info")
    private IpLocationInfo ipLocationInfo;

    @JsonProperty("browser")
    private Browser browser;

    @JsonProperty("operating_system")
    private OperatingSystem operatingSystem;

    @JsonProperty("time")
    private String time;

    @JsonProperty("device")
    private Device device;

    @JsonProperty("user_agent")
    private String userAgent;
}

