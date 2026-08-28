package com.kittyp.ai.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kittyp.ai.entity.NutritionPlan;
import com.kittyp.ai.enums.NutritionPlanStatus;

import jakarta.transaction.Transactional;

@Repository
public interface NutritionPlanRepository
        extends JpaRepository<NutritionPlan, Long>, JpaSpecificationExecutor<NutritionPlan> {

    Optional<NutritionPlan> findByUuid(String uuid);

    List<NutritionPlan> findByPetUuidAndIsActiveTrue(String petUuid);

    Page<NutritionPlan> findByPetUuidAndIsActive(String petUuid, boolean isActive, Pageable pageable);

    List<NutritionPlan> findByUserUuidAndIsActive(String userUuid, boolean isActive);

    List<NutritionPlan> findByUserUuidAndIsActiveTrue(String userUuid);

    List<NutritionPlan> findByPetUuidOrderByGenerationTimestampDesc(String petUuid);

    List<NutritionPlan> findByUserUuidOrderByGenerationTimestampDesc(String userUuid);

    @Query("SELECT np FROM NutritionPlan np WHERE np.petUuid = :petUuid AND np.isActivePlan = true AND np.isActive = true")
    Optional<NutritionPlan> findActivePlanByPetUuid(@Param("petUuid") String petUuid);

    Optional<NutritionPlan> findFirstByPetUuidAndParentUserUuidAndStatusAndIsActiveTrueOrderBySentAtDesc(
            String petUuid, String parentUserUuid, NutritionPlanStatus status);

    Optional<NutritionPlan> findFirstByPetUuidAndStatusAndIsActiveTrueOrderBySentAtDesc(
            String petUuid, NutritionPlanStatus status);

    @Query("SELECT np FROM NutritionPlan np WHERE np.userUuid = :userUuid AND np.generationTimestamp >= :startDate ORDER BY np.generationTimestamp DESC")
    List<NutritionPlan> findRecentPlansByUserUuid(@Param("userUuid") String userUuid,
            @Param("startDate") LocalDateTime startDate);

    @Query("SELECT np FROM NutritionPlan np WHERE np.petUuid = :petUuid AND np.generationTimestamp >= :startDate ORDER BY np.generationTimestamp DESC")
    List<NutritionPlan> findRecentPlansByPetUuid(@Param("petUuid") String petUuid,
            @Param("startDate") LocalDateTime startDate);

    long countByPetUuidAndIsActiveTrue(String petUuid);

    long countByUserUuidAndIsActiveTrue(String userUuid);

    @Transactional
    @Modifying
    @Query("""
                UPDATE NutritionPlan p
                SET p.isActivePlan = false
                WHERE p.petUuid = :petUuid
                  AND p.planMonth = :planMonth
                  AND p.planYear = :planYear
                  AND p.isActivePlan = true
            """)
    int deactivatePlansForMonth(@Param("petUuid") String petUuid,
            @Param("planMonth") int planMonth,
            @Param("planYear") int planYear);

}
