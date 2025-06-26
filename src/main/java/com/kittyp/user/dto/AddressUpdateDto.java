package com.kittyp.user.dto;

import com.kittyp.user.enums.AddressType;

import lombok.Getter;

@Getter
public class AddressUpdateDto {
    
    private String uuid;
    
    private String street;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private AddressType addressType;

    private boolean isDefault;
}
