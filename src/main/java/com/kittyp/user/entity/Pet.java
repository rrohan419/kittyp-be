package com.kittyp.user.entity;

import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private String age;
    private String weight;
    private String activityLevel;
    private String gender;
    private Set<String> photos;
    private boolean isNeutered;
    private String currentFoodBrand;
    @Column(columnDefinition = "TEXT")
    private String healthConditions;
    @Column(columnDefinition = "TEXT")
    private String allergies;

}
