package com.kittyp.clinic.service;

import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.common.model.PaginationModel;

public interface ClinicInventoryService {

    PaginationModel<InventoryItemModel> list(String clinicUuid, Integer pageNumber, Integer pageSize, String email);

    InventoryItemModel create(String clinicUuid, InventoryItemRequest request, String email);

    InventoryItemModel update(String clinicUuid, String itemUuid, InventoryItemRequest request, String email);

    void delete(String clinicUuid, String itemUuid, String email);
}