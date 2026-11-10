package com.kittyp.user.models;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetModel {
    
    private String uuid;
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
    private String microchipNumber;
}
