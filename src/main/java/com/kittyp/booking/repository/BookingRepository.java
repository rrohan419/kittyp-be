package com.kittyp.booking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByClinic_Id(Long clinicId, Pageable pageable);

    Page<Booking> findByClinic_IdAndStatus(Long clinicId, BookingStatus status, Pageable pageable);

    List<Booking> findByClinic_Id(Long clinicId);

    List<Booking> findByDoctor_IdAndSlotStartBetweenOrderBySlotStartAsc(Long doctorId, LocalDateTime from,
            LocalDateTime to);

    List<Booking> findByOwner_IdOrderBySlotStartDesc(Long ownerId);
}
