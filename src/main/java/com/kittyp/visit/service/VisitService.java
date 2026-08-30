package com.kittyp.visit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.visit.dto.VisitDtos.AttendedPatientModel;
import com.kittyp.visit.dto.VisitDtos.ParentBookingCreateRequest;
import com.kittyp.visit.dto.VisitDtos.ScheduleBookingCreateRequest;
import com.kittyp.visit.dto.VisitDtos.ScheduleBookingPatchRequest;
import com.kittyp.visit.dto.VisitDtos.VisitChartRequest;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.dto.VisitDtos.VisitPatchRequest;
import com.kittyp.visit.dto.VisitDtos.VisitRatingModel;
import com.kittyp.visit.dto.VisitDtos.VisitRatingRequest;
import com.kittyp.visit.dto.VisitDtos.WalkInCreateRequest;
import com.kittyp.visit.enums.VisitStatus;

public interface VisitService {

    VisitModel createWalkIn(String clinicUuid, WalkInCreateRequest request, String email);

    /** Schedule a future appointment (Booking) for a clinic patient. */
    BookingModel createScheduledBooking(String clinicUuid, ScheduleBookingCreateRequest request, String email);

    /** Clinic front-desk: reschedule, reassign, notes, or cancel a booking at this clinic. */
    BookingModel updateScheduledBooking(String clinicUuid, String bookingUuid, ScheduleBookingPatchRequest request,
            String email);

    /** Parent self-serve booking for an owned pet at a discoverable clinic. */
    BookingModel createParentBooking(ParentBookingCreateRequest request, String email);

    /** Free half-hour slots for a clinic doctor on a given date (availability minus busy). */
    List<LocalDateTime> listParentDoctorSlots(String clinicUuid, String doctorUuid, LocalDate date, String email);

    /** Busy intervals for a clinic doctor across all clinics (active bookings overlapping the range). */
    List<BookingModel> listDoctorBusySlots(String clinicUuid, String doctorUuid, LocalDateTime from,
            LocalDateTime to, String email);

    /** Doctor starts treatment from a scheduled booking — creates an IN_PROGRESS visit. */
    VisitModel startTreatmentFromBooking(String bookingUuid, String email);

    List<VisitModel> listClinicVisits(String clinicUuid, LocalDate date, LocalDate from, LocalDate to,
            VisitStatus status, String doctorUuid, String email);

    VisitModel patchVisit(String clinicUuid, String visitUuid, VisitPatchRequest request, String email);

    List<VisitModel> listPetVisitsForClinic(String clinicUuid, String petUuid, String email);

    List<VisitModel> listMyDoctorVisits(LocalDate date, String clinicUuid, String email);

    /** Visits for this doctor in [from, to] inclusive days, optionally scoped to a clinic. */
    List<VisitModel> listMyDoctorVisitsRange(LocalDate from, LocalDate to, String clinicUuid, String email);

    VisitModel startVisit(String visitUuid, String email);

    VisitModel saveChart(String visitUuid, VisitChartRequest request, String email);

    VisitModel completeVisit(String visitUuid, String email);

    /** Doctor sends patient back to reception (CHECKED_IN) without completing. */
    VisitModel returnToReception(String visitUuid, String email);

    List<VisitModel> listParentPetVisits(String petUuid, String email);

    /** All clinic visits across pets owned/linked to this parent. */
    List<VisitModel> listMyParentVisits(String email);

    /** Scheduled appointments for this parent (by account, linked pets, or clinic-owner email). */
    List<BookingModel> listMyParentBookings(String email);

    VisitRatingModel rateVisit(String visitUuid, VisitRatingRequest request, String email);

    /** Pets this doctor attended across clinics (or one clinic when clinicUuid is set). */
    List<AttendedPatientModel> listMyAttendedPatients(String email, String clinicUuid);

    PaginationModel<AttendedPatientModel> pageMyAttendedPatients(String email, String clinicUuid, String q,
            Integer pageNumber, Integer pageSize);
}
