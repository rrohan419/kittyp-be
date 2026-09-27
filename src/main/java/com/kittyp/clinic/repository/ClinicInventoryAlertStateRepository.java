package com.kittyp.clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicInventoryAlertState;
import com.kittyp.clinic.enums.InventoryAlertType;

public interface ClinicInventoryAlertStateRepository extends JpaRepository<ClinicInventoryAlertState, Long> {

    List<ClinicInventoryAlertState> findByClinic_IdAndResolvedAtIsNullAndIsActiveTrueOrderByLastNotifiedAtDesc(
            Long clinicId);

    Optional<ClinicInventoryAlertState> findByClinic_IdAndAlertTypeAndInventoryItem_IdAndStateKey(
            Long clinicId, InventoryAlertType alertType, Long itemId, String stateKey);

    Optional<ClinicInventoryAlertState> findByClinic_IdAndAlertTypeAndInventoryItemIsNullAndStateKey(
            Long clinicId, InventoryAlertType alertType, String stateKey);
}
