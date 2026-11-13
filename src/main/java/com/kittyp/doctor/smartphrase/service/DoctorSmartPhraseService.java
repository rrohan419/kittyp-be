package com.kittyp.doctor.smartphrase.service;

import java.util.List;

import com.kittyp.doctor.smartphrase.dto.DoctorSmartPhraseDtos.SmartPhraseModel;
import com.kittyp.doctor.smartphrase.dto.DoctorSmartPhraseDtos.SmartPhraseRequest;

public interface DoctorSmartPhraseService {

    List<SmartPhraseModel> list(String doctorUserUuid);

    SmartPhraseModel create(String doctorUserUuid, SmartPhraseRequest request);

    SmartPhraseModel update(String doctorUserUuid, String phraseUuid, SmartPhraseRequest request);

    void delete(String doctorUserUuid, String phraseUuid);
}
