package com.kittyp.user.entity;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class Pet extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String profilePicture;
    
    private String type;
    
    private String breed;
    
    private LocalDate dateOfBirth;
    
    private String weight;
    
    private String activityLevel;
    
    private String gender;

    @ElementCollection
    @CollectionTable(name = "pet_photos", joinColumns = @JoinColumn(name = "pet_id"))
    @Column(name = "photo_url")
    private Set<String> photos;
    
    private boolean isNeutered;
    
    private String currentFoodBrand;
    
    @Column(columnDefinition = "TEXT")
    private String healthConditions;
    
    @Column(columnDefinition = "TEXT")
    private String allergies;

    public int getAgeInMonths() {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getMonths() +
                (Period.between(dateOfBirth, LocalDate.now()).getYears() * 12);
    }

}
