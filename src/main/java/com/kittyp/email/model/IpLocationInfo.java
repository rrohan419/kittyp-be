/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IpLocationInfo {

    @JsonProperty("zipcode")
    private String zipCode;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("city")
    private String city;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("country_name")
    private String countryName;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("time_zone")
    private String timeZone;

    @JsonProperty("region")
    private String region;

    @JsonProperty("longitude")
    private String longitude;
}

