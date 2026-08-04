package com.kittyp.clinic.service;

import java.util.List;

import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicRequest;
import com.kittyp.clinic.dto.ClinicDtos.DoctorModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventRequest;
import com.kittyp.clinic.dto.ClinicDtos.PatientDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientModel;
import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.common.model.PaginationModel;

public interface ClinicService {

    List<ClinicModel> mine(String email);

    ClinicModel create(ClinicRequest request, String email);

    ClinicModel get(String clinicUuid, String email);

    ClinicModel update(String clinicUuid, ClinicRequest request, String email);

    List<DoctorModel> doctors(String clinicUuid, String email);

    List<PatientModel> patients(String clinicUuid, String email);

    PatientDetailModel patientDetail(String clinicUuid, String petUuid, String email);

    PaginationModel<BookingModel> bookings(String clinicUuid, String status, int page, int size, String email);

    List<RetentionAlertModel> retentionAlerts(String clinicUuid, String email);

    void notifyAlert(String clinicUuid, String alertId, String email);

    List<HealthEventModel> healthEvents(String clinicUuid, String petUuid, String email);

    HealthEventModel createHealthEvent(String clinicUuid, String petUuid, HealthEventRequest request, String email);
}
