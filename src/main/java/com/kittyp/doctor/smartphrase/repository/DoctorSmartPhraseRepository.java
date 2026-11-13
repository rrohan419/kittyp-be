package com.kittyp.doctor.smartphrase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.kittyp.doctor.smartphrase.entity.DoctorSmartPhrase;

@Repository
public interface DoctorSmartPhraseRepository
        extends JpaRepository<DoctorSmartPhrase, Long>, JpaSpecificationExecutor<DoctorSmartPhrase> {
}
