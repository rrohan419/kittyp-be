package com.kittyp.vaccine.entity;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vaccine_master")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccineMaster extends BaseEntity {
    
    private String name;
    private String species;
    private Integer initialAgeWeeks;
    private Integer repeatIntervalMonths;
    private String description;
}
