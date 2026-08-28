package com.kittyp.nutrition.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.nutrition.enums.ItemType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pet_daily_plan", uniqueConstraints = @UniqueConstraint(columnNames = { "petUuid", "day", "time",
        "itemName", "active" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class PetDailyPlan extends BaseEntity {

    @Column(nullable = false)
    private String petUuid;

    @Column(nullable = false)
    private String userUuid;

    // Each 30-day plan has a unique parent UUID
    @Column(nullable = false)
    private String nutritionPlanUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType itemType; // FOOD / SUPPLEMENT

    @Column(nullable = false, length = 500)
    private String itemName;

    @Column(nullable = false)
    private LocalTime time;

    private Double quantityInGrams;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDate day;

    @Column(nullable = false)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(nullable = false)
    @Builder.Default
    private boolean notificationSent = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    private int planMonth; // e.g. 11 for November

    @Column(nullable = false)
    private int planYear; // e.g. 2025

}
