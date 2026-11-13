package com.kittyp.doctor.smartphrase.service;

import org.springframework.data.jpa.domain.Specification;

import com.kittyp.doctor.smartphrase.entity.DoctorSmartPhrase;

public final class DoctorSmartPhraseSpecification {

    private DoctorSmartPhraseSpecification() {
    }

    public static Specification<DoctorSmartPhrase> forDoctor(String doctorUserUuid) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("doctorUserUuid"), doctorUserUuid),
                criteriaBuilder.isTrue(root.get("isActive")));
    }

    public static Specification<DoctorSmartPhrase> withUuid(String phraseUuid) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("uuid"), phraseUuid);
    }
}
