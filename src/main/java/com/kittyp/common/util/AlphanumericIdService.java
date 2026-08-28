package com.kittyp.common.util;

import java.security.SecureRandom;
import java.util.function.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.repository.ConsultationInvoiceRepository;
import com.kittyp.doctor.repository.DoctorProfileRepository;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.PetsRepository;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Collision-resistant alphanumeric public IDs (A–Z, 0–9).
 * Default length is 6 (users, pets, clinics, doctors, invoices).
 * {@link #generate()} is used at persist time. Uniqueness is enforced by the
 * column UNIQUE constraint. Columns stay VARCHAR(255) so a one-time backfill
 * can replace legacy UUID strings without shrinking the column.
 */
@Service
@RequiredArgsConstructor
public class AlphanumericIdService {

    public static final int LENGTH = 6;
    public static final int INVOICE_LENGTH = 6;
    public static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final int MAX_ATTEMPTS = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PetsRepository petsRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ConsultationInvoiceRepository consultationInvoiceRepository;

    public static String generate() {
        return generate(LENGTH);
    }

    public static String generate(int length) {
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
        }
        return new String(buf);
    }

    /**
     * Retrying allocator for callers that run <em>before</em> {@code persist}.
     * Do not call from {@code @PrePersist} — a repository exists-check can flush
     * the current entity with a null uuid.
     */
    public String allocateUnique(Class<?> type) {
        int length = ConsultationInvoice.class.isAssignableFrom(type) ? INVOICE_LENGTH : LENGTH;
        return allocateUnique(existsCheck(type), length);
    }

    public String allocateUnique(Predicate<String> exists) {
        return allocateUnique(exists, LENGTH);
    }

    public String allocateUnique(Predicate<String> exists, int length) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String id = generate(length);
            if (!exists.test(id)) {
                return id;
            }
        }
        throw new CustomException("Unable to allocate a unique public id", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Predicate<String> existsCheck(Class<?> type) {
        if (User.class.isAssignableFrom(type)) {
            return userRepository::existsByUuid;
        }
        if (Pet.class.isAssignableFrom(type)) {
            return petsRepository::existsByUuid;
        }
        if (Clinic.class.isAssignableFrom(type)) {
            return clinicRepository::existsByUuid;
        }
        if (DoctorProfile.class.isAssignableFrom(type)) {
            return doctorProfileRepository::existsByUuid;
        }
        if (ConsultationInvoice.class.isAssignableFrom(type)) {
            return consultationInvoiceRepository::existsByUuid;
        }
        return id -> false;
    }
}
