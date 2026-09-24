package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.PaginationSupport;

class ClinicRetentionPaginationTest {

    @Test
    void retentionAlertOrderingAndFiltersProduceStablePageShape() {
        List<RetentionAlertModel> alerts = List.of(
                new RetentionAlertModel("b", "pet-2", "Ziggy", "Owner", "LAPSED_VISIT", "late", 20, "LAPSED"),
                new RetentionAlertModel("a", "pet-1", "Milo", "Owner", "VACCINATION", "due", 5, "DUE"));

        List<RetentionAlertModel> filtered = ClinicServiceImpl.filterAndSortRetentionAlerts(alerts, "DUE", null);
        PaginationModel<RetentionAlertModel> page = PaginationSupport.slice(filtered, 1, 20);

        assertEquals(1L, page.getTotalElements());
        assertEquals(1, page.getPageNumber());
        assertEquals("a", page.getModels().get(0).id());
    }
}