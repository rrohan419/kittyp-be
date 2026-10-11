package com.kittyp.clinic.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Order(9)
@RequiredArgsConstructor
public class ClinicPetParentSignupInviteSchemaRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS clinic_pet_parent_signup_invites (
                    id BIGSERIAL PRIMARY KEY,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    token_hash VARCHAR(64) NOT NULL UNIQUE,
                    email VARCHAR(255) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    consumed_at TIMESTAMP
                )
                """);
    }
}
