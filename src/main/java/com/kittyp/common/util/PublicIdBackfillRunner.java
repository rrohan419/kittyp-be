package com.kittyp.common.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Rewrites legacy UUID / 5-char public ids to 6-char alphanumeric on startup.
 * Numeric PKs and FKs are untouched; only {@code uuid} string columns (and
 * denormalized copies) are remapped.
 */
@Component
@Order(20)
@RequiredArgsConstructor
public class PublicIdBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PublicIdBackfillRunner.class);
    private static final String CANONICAL = "^[0-9A-Z]{6}$";

    private static final String[][] USER_REFS = {
            { "address", "user_uuid" },
            { "carts", "user_uuid" },
            { "orders", "user_uuid" },
            { "pets", "user_uuid" },
            { "article_comment_likes", "user_uuid" },
            { "authors", "user_uuid" },
            { "file_records", "user_uuid" },
            { "nutrition_plans", "user_uuid" },
            { "nutrition_plans", "parent_user_uuid" },
            { "nutrition_plans", "doctor_user_uuid" },
            { "pet_daily_plan", "user_uuid" },
            { "pet_feeding_log", "user_uuid" },
            { "user_favourite_products", "user_uuid" },
            { "article_comments", "commenter_uuid" },
            { "article_likes", "liker_uuid" }
    };

    private static final String[][] PET_REFS = {
            { "nutrition_plans", "pet_uuid" },
            { "pet_daily_plan", "pet_uuid" },
            { "pet_feeding_log", "pet_uuid" },
            { "consultation_invoices", "pet_uuid" },
            { "clinic_patient_pets", "global_pet_id" }
    };

    private static final String[][] USER_FKS = {
            { "address", "user_uuid", "users" },
            { "carts", "user_uuid", "users" },
            { "orders", "user_uuid", "users" }
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int users = remap("users", USER_REFS, USER_FKS);
        int pets = remap("pets", PET_REFS, new String[0][]);
        int doctors = remap("doctor_profiles", new String[0][], new String[0][]);
        int clinics = remap("clinics", new String[0][], new String[0][]);
        int owners = remap("clinic_pet_owners", new String[0][], new String[0][]);
        int bookings = remap("bookings", new String[0][], new String[0][]);
        log.info("Public id backfill: users={}, pets={}, doctors={}, clinics={}, owners={}, bookings={}",
                users, pets, doctors, clinics, owners, bookings);
    }

    private int remap(String table, String[][] refs, String[][] fks) {
        List<String> oldIds = jdbcTemplate.queryForList(
                "SELECT uuid FROM " + table + " WHERE uuid IS NOT NULL AND uuid !~ '" + CANONICAL + "'",
                String.class);
        if (oldIds.isEmpty()) {
            return 0;
        }

        Set<String> used = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT uuid FROM " + table + " WHERE uuid ~ '" + CANONICAL + "'",
                String.class));

        for (String[] fk : fks) {
            dropFk(fk[0], fk[1]);
        }

        int n = 0;
        for (String oldId : oldIds) {
            String neu = nextId(used);
            jdbcTemplate.update("UPDATE " + table + " SET uuid = ? WHERE uuid = ?", neu, oldId);
            for (String[] ref : refs) {
                if (columnExists(ref[0], ref[1])) {
                    jdbcTemplate.update(
                            "UPDATE " + ref[0] + " SET " + ref[1] + " = ? WHERE " + ref[1] + " = ?",
                            neu, oldId);
                }
            }
            n++;
        }

        for (String[] fk : fks) {
            addFk(fk[0], fk[1], fk[2]);
        }
        return n;
    }

    private String nextId(Set<String> used) {
        for (int i = 0; i < AlphanumericIdService.MAX_ATTEMPTS * 4; i++) {
            String id = AlphanumericIdService.generate(AlphanumericIdService.LENGTH);
            if (used.add(id)) {
                return id;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique 6-char public id");
    }

    private void dropFk(String table, String column) {
        List<String> names = jdbcTemplate.queryForList("""
                SELECT con.conname
                FROM pg_constraint con
                JOIN pg_class rel ON rel.oid = con.conrelid
                JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                WHERE nsp.nspname = 'public'
                  AND rel.relname = ?
                  AND con.contype = 'f'
                  AND pg_get_constraintdef(con.oid) ILIKE ?
                """, String.class, table, "%(" + column + ")%");
        for (String name : names) {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + name);
        }
    }

    private void addFk(String table, String column, String refTable) {
        String constraint = "fk_" + table + "_" + column;
        jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraint
                + " FOREIGN KEY (" + column + ") REFERENCES " + refTable + "(uuid)");
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }
}
