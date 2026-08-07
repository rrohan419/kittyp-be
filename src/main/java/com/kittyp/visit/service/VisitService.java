package com.kittyp.visit.service;

import java.time.LocalDate;
import java.util.List;

import com.kittyp.visit.dto.VisitDtos.AttendedPatientModel;
import com.kittyp.visit.dto.VisitDtos.VisitChartRequest;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.dto.VisitDtos.VisitPatchRequest;
import com.kittyp.visit.dto.VisitDtos.WalkInCreateRequest;
import com.kittyp.visit.enums.VisitStatus;

public interface VisitService {

    VisitModel createWalkIn(String clinicUuid, WalkInCreateRequest request, String email);

    List<VisitModel> listClinicVisits(String clinicUuid, LocalDate date, VisitStatus status, String doctorUuid,
            String email);

    VisitModel patchVisit(String clinicUuid, String visitUuid, VisitPatchRequest request, String email);

    List<VisitModel> listPetVisitsForClinic(String clinicUuid, String petUuid, String email);

    List<VisitModel> listMyDoctorVisits(LocalDate date, String email);

    VisitModel startVisit(String visitUuid, String email);

    VisitModel saveChart(String visitUuid, VisitChartRequest request, String email);

    VisitModel completeVisit(String visitUuid, String email);

    /** Doctor sends patient back to reception (CHECKED_IN) without completing. */
    VisitModel returnToReception(String visitUuid, String email);

    List<VisitModel> listParentPetVisits(String petUuid, String email);

    /** All clinic visits across pets owned/linked to this parent. */
    List<VisitModel> listMyParentVisits(String email);

    List<AttendedPatientModel> listMyAttendedPatients(String email);
}
