package com.kittyp.user.dto;

import com.kittyp.user.enums.AddressType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AddressUpdateDto {
    
    @NotBlank
    private String uuid;

    private String name;
    
    private String street;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private AddressType addressType;

    private String formattedAddress;

    private String phoneNumber;
}
