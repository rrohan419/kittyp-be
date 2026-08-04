package com.kittyp.booking.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.kittyp.booking.entity.Booking;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.booking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BookingDaoImpl implements BookingDao {

    private final BookingRepository bookingRepository;

    @Override
    public Page<Booking> findByClinic(Long clinicId, BookingStatus status, Pageable pageable) {
        return status == null ? bookingRepository.findByClinic_Id(clinicId, pageable)
                : bookingRepository.findByClinic_IdAndStatus(clinicId, status, pageable);
    }

    @Override
    public List<Booking> findByClinic(Long clinicId) {
        return bookingRepository.findByClinic_Id(clinicId);
    }
}
