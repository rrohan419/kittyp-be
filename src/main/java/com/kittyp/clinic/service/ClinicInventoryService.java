package com.kittyp.clinic.service;

import java.util.List;

import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryEditAccess;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryMovementModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockAdjustmentRequest;
import com.kittyp.common.model.PaginationModel;

public interface ClinicInventoryService {

    PaginationModel<InventoryItemModel> list(String clinicUuid, String category, Integer pageNumber, Integer pageSize,
            String email);

    List<InventoryItemModel> options(String clinicUuid, String category, String email);

    PaginationModel<InventoryMovementModel> movements(String clinicUuid, Integer pageNumber, Integer pageSize,
            String email);

    InventoryItemModel create(String clinicUuid, InventoryItemRequest request, String email);

    InventoryItemModel update(String clinicUuid, String itemUuid, InventoryItemRequest request, String email);

    InventoryItemModel adjustStock(String clinicUuid, String itemUuid, StockAdjustmentRequest request, String email);

    void delete(String clinicUuid, String itemUuid, String email);

    InventoryEditAccess editAccess(String clinicUuid, String email);
}