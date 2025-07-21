package com.kittyp.order.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressInfo implements Serializable{
    private String name;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String formattedAddress;
    private String phoneNumber;
}

