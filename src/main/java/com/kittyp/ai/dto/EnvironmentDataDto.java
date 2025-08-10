package com.kittyp.ai.dto;

import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnvironmentDataDto {
    
    private double temperature;
    private String unit;
    private int humidity;
    private String weatherCondition;
    private double windSpeed;
    private String windUnit;
    private int uvIndex;
    private double precipitation;
}
