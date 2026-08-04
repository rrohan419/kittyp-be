package com.kittyp.nutrition.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.nutrition.enums.FeedingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pet_feeding_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class PetFeedingLog extends BaseEntity{
    
    @Column(nullable = false)
    private String petUuid;

    @Column(nullable = false)
    private String userUuid;

    @Column
    private Long dailyPlanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedingStatus status;

    private Double consumedQuantityInGrams;

    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime loggedAt = LocalDateTime.now();

}
