package com.kittyp.ai.dto;


import com.kittyp.user.entity.Pet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetNutritionRecommendationDto {
    
    private String uuid;
    private String name;
    private String profilePicture;
    private String type;
    private String breed;
    private String dateOfBirth;
    private String weight;
    private String activityLevel;
    private String gender; 
    private String currentFoodBrand;
    private String healthConditions;
    private String allergies;
    private Boolean isNeutered;
    private Boolean isSpayedOrNeutered;

    /** Prefer stored pet fields so the model sees the clinic record, not a thin picker DTO. */
    public void overlayFromStoredPet(Pet pet) {
        if (pet == null) {
            return;
        }
        uuid = first(uuid, pet.getUuid());
        name = first(name, pet.getName());
        profilePicture = first(profilePicture, firstNonBlank(pet.getProfilePicture()));
        if (profilePicture == null && pet.getPhotos() != null) {
            for (String url : pet.getPhotos()) {
                profilePicture = firstNonBlank(url);
                if (profilePicture != null) {
                    break;
                }
            }
        }
        type = first(type, pet.getType());
        breed = first(breed, pet.getBreed());
        dateOfBirth = first(dateOfBirth, pet.getDateOfBirth() != null ? pet.getDateOfBirth().toString() : null);
        weight = first(weight, pet.getWeight());
        activityLevel = first(activityLevel, pet.getActivityLevel());
        gender = first(gender, pet.getGender());
        currentFoodBrand = first(currentFoodBrand, pet.getCurrentFoodBrand());
        healthConditions = first(healthConditions, pet.getHealthConditions());
        allergies = first(allergies, pet.getAllergies());
        if (isNeutered == null) {
            isNeutered = pet.isNeutered();
        }
        if (isSpayedOrNeutered == null) {
            isSpayedOrNeutered = isNeutered != null ? isNeutered : pet.isNeutered();
        }
    }

    private static String first(String preferred, String fallback) {
        String clean = firstNonBlank(preferred);
        return clean != null ? clean : firstNonBlank(fallback);
    }

    private static String firstNonBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
