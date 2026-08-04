package com.kittyp.booking.repository;

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
}
