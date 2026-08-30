package com.kittyp.ai.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.ai.enums.NutritionPlanStatus;
import com.kittyp.common.entity.BaseEntity;

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
@Table(name = "nutrition_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class NutritionPlan extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "pet_uuid", nullable = false)
    private String petUuid;

    @Column(name = "user_uuid", nullable = false)
    private String userUuid;

    @Column(name = "parent_user_uuid")
    private String parentUserUuid;

    @Column(name = "doctor_user_uuid")
    private String doctorUserUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "varchar(20) not null default 'DRAFT'")
    @Builder.Default
    private NutritionPlanStatus status = NutritionPlanStatus.DRAFT;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "pet_profile_summary", columnDefinition = "TEXT", nullable = false)
    private String petProfileSummary;

     @Column(name = "environmental_impact", columnDefinition = "TEXT", nullable = false)
    private String environmentalImpact;

    @Column(name = "daily_feeding_plan", columnDefinition = "TEXT", nullable = false)
    private String dailyFeedingPlan;

    @Column(name = "special_consideration", columnDefinition = "TEXT", nullable = false)
    private String specialConsiderations;

    @Column(name = "recommended_products", columnDefinition = "TEXT", nullable = false)
    private String recommendedProducts;

    @Column(name = "wellness_tips", columnDefinition = "TEXT", nullable = false)
    private String longTermWellnessTips;

    @Column(name = "environment", columnDefinition = "TEXT", nullable = false)
    private String environment;

    @Column(name = "generation_timestamp", nullable = false)
    private LocalDateTime generationTimestamp;

    @Column(name = "is_active_plan")
    @Builder.Default
    private Boolean isActivePlan = false; 

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column
    private int planMonth; // e.g. 11 for November

    @Column
    private int planYear;

    /** Days the parent should follow the plan, set when the doctor sends it. */
    @Column(name = "duration_days")
    private Integer durationDays;

}
