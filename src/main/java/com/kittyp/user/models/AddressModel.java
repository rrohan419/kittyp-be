package com.kittyp.user.models;

import lombok.Data;

@Data
public class AddressModel {
    
    private String uuid;

    private String name;

    private String street;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String formattedAddress;

    private String phoneNumber;

    private UserDetailsModel user;
}
