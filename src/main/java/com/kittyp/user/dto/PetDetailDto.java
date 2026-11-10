package com.kittyp.user.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PetDetailDto {
    @NotBlank
    private String name;
    private String profilePicture;
    private String type;
    private String breed;
    private LocalDate dateOfBirth;
    private String weight;
    private String activityLevel;
    private String gender;
    @JsonProperty("isNeutered")
    @JsonAlias("neutered")
    private boolean isNeutered;
    private String currentFoodBrand;
    private String healthConditions;
    private String allergies;
}
