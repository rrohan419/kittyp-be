package com.kittyp.clinic.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(8)
@RequiredArgsConstructor
public class ClinicPracticeModelSchemaRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE clinics ADD COLUMN IF NOT EXISTS practice_model VARCHAR(32)");
        int backfilled = jdbcTemplate.update("""
                UPDATE clinics c
                SET practice_model = CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM clinic_doctors cd
                        JOIN doctor_profiles dp ON dp.id = cd.doctor_id
                        WHERE cd.clinic_id = c.id
                          AND cd.doctor_id IS NOT NULL
                          AND cd.role = 'owner'
                          AND cd.is_active = TRUE
                          AND dp.user_id = c.owner_user_id
                    ) THEN 'PERSONAL_PRACTICE'
                    ELSE 'ORGANIZATION'
                END,
                status = CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM clinic_doctors cd
                        JOIN doctor_profiles dp ON dp.id = cd.doctor_id
                        WHERE cd.clinic_id = c.id
                          AND cd.role = 'owner'
                          AND cd.is_active = TRUE
                          AND dp.user_id = c.owner_user_id
                    ) AND c.status <> 'SHUTDOWN' THEN 'NOT_REQUIRED'
                    ELSE c.status
                END
                WHERE c.practice_model IS NULL
                """);
        jdbcTemplate.execute("ALTER TABLE clinics ALTER COLUMN practice_model SET DEFAULT 'ORGANIZATION'");
        jdbcTemplate.execute("ALTER TABLE clinics ALTER COLUMN practice_model SET NOT NULL");
        log.info("Clinic practice model schema ready; {} existing clinics classified", backfilled);
    }
}
