package com.kittyp.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GoogleUserInfo {
    private String id;
    private String email;
    
    @JsonProperty("verified_email")
    private Boolean verifiedEmail;
    
    private String name;
    
    @JsonProperty("given_name")
    private String givenName;
    
    @JsonProperty("family_name")
    private String familyName;
    
    private String picture;
    private String locale;
} 