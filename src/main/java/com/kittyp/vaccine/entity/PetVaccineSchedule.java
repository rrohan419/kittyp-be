package com.kittyp.vaccine.entity;

import java.time.LocalDate;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.user.entity.Pet;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pet_vaccine_schedule")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetVaccineSchedule extends BaseEntity{
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vaccine_id")
    private VaccineMaster vaccine;

    private LocalDate dueDate;

    @Builder.Default
    private Boolean completed = false;
 
    private LocalDate completedDate;

    @Builder.Default
    private Boolean reminderSent = false;
}
