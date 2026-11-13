package com.kittyp.doctor.smartphrase.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.smartphrase.dto.DoctorSmartPhraseDtos.SmartPhraseModel;
import com.kittyp.doctor.smartphrase.dto.DoctorSmartPhraseDtos.SmartPhraseRequest;
import com.kittyp.doctor.smartphrase.entity.DoctorSmartPhrase;
import com.kittyp.doctor.smartphrase.repository.DoctorSmartPhraseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorSmartPhraseServiceImpl implements DoctorSmartPhraseService {

    private final DoctorSmartPhraseRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<SmartPhraseModel> list(String doctorUserUuid) {
        return repository.findAll(DoctorSmartPhraseSpecification.forDoctor(doctorUserUuid)).stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional
    public SmartPhraseModel create(String doctorUserUuid, SmartPhraseRequest request) {
        DoctorSmartPhrase phrase = DoctorSmartPhrase.builder()
                .uuid(UUID.randomUUID().toString())
                .doctorUserUuid(doctorUserUuid)
                .phraseText(request.phraseText().trim())
                .build();
        return toModel(repository.save(phrase));
    }

    @Override
    @Transactional
    public SmartPhraseModel update(String doctorUserUuid, String phraseUuid, SmartPhraseRequest request) {
        DoctorSmartPhrase phrase = findOwnedPhrase(doctorUserUuid, phraseUuid);
        phrase.setPhraseText(request.phraseText().trim());
        return toModel(repository.save(phrase));
    }

    @Override
    @Transactional
    public void delete(String doctorUserUuid, String phraseUuid) {
        DoctorSmartPhrase phrase = findOwnedPhrase(doctorUserUuid, phraseUuid);
        phrase.setIsActive(false);
        repository.save(phrase);
    }

    private DoctorSmartPhrase findOwnedPhrase(String doctorUserUuid, String phraseUuid) {
        return repository.findOne(DoctorSmartPhraseSpecification.forDoctor(doctorUserUuid)
                        .and(DoctorSmartPhraseSpecification.withUuid(phraseUuid)))
                .orElseThrow(() -> new CustomException("Smart phrase not found", HttpStatus.NOT_FOUND));
    }

    private SmartPhraseModel toModel(DoctorSmartPhrase phrase) {
        return new SmartPhraseModel(phrase.getUuid(), phrase.getPhraseText(), phrase.getCreatedAt(),
                phrase.getUpdatedAt());
    }
}
