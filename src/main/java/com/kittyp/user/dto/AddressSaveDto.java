package com.kittyp.user.dto;

import com.kittyp.user.enums.AddressType;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AddressSaveDto {
    
    @NotBlank
    private String street;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String country;

    @NotBlank
    private AddressType addressType;

    private boolean isDefault;
}
