package com.kittyp.auth.dto;

import com.kittyp.user.enums.Provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialSso {
    
    private String code;
    private String scope;
    private Provider provider;
    private String token;
    private String redirectUri;
}
