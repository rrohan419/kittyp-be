package com.kittyp.clinic.service;

import java.time.LocalDate;

import com.kittyp.clinic.dto.ClinicReportsDto.ReportModel;

public interface ClinicReportsService {

    ReportModel reports(String clinicUuid, String email, LocalDate from, LocalDate to);
}
