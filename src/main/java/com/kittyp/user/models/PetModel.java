package com.kittyp.user.models;

import lombok.AllArgsConstructor;
import lombok.Data;
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
    private String breed;
    private String age;
    private String weight;
    private String activityLevel;
    private String gender;
    private boolean isNeutered;
    private String currentFoodBrand;
    private String healthConditions;
    private String allergies;
}
