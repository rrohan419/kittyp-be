package com.kittyp.booking.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@Order(9)
@RequiredArgsConstructor
public class BookingUrgencySchemaRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS urgency VARCHAR(20)");
        jdbcTemplate.execute("UPDATE bookings SET urgency = 'ROUTINE' WHERE urgency IS NULL");
        jdbcTemplate.execute("ALTER TABLE bookings ALTER COLUMN urgency SET DEFAULT 'ROUTINE'");
        jdbcTemplate.execute("ALTER TABLE bookings ALTER COLUMN urgency SET NOT NULL");
    }
}
