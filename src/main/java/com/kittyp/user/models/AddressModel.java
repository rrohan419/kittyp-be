package com.kittyp.user.models;

import lombok.Data;

@Data
public class AddressModel {
    
    private String uuid;

    private String street;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String formattedAddress;

    private boolean isDefault = false;

    private UserDetailsModel user;
}
