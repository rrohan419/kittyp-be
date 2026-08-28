package com.kittyp.booking.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;

public interface BookingDao {

    Page<Booking> findByClinic(Long clinicId, BookingStatus status, Pageable pageable);

    List<Booking> findByClinic(Long clinicId);
}
