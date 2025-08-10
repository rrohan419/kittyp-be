package com.kittyp.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationData {
    
    @NotBlank
    private String latitude;
    
    @NotBlank
    private String longitude;
    private Double accuracy;
    private long timestamp;

}
