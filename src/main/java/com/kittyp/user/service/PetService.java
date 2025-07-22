package com.kittyp.user.service;

import java.util.List;

import com.kittyp.user.dto.PetDetailDto;
import com.kittyp.user.models.PetModel;

public interface PetService {
    
    PetModel addPet(PetDetailDto petDetailDto, String email);

    List<PetModel> petsByUserUuid(String userUuid);

    PetModel updatePet(PetDetailDto petDetailDto, String uuid, String email);

    void deletePetByUuid(String uuid, String email);

    PetModel updatePetProfilePicture(String petUuid, String profilePictureUrl);
}
