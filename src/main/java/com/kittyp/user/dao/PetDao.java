package com.kittyp.user.dao;

import com.kittyp.user.entity.Pet;

public interface PetDao {
    Pet savePets(Pet pet);
    void deleteByUuid(String uuid);
    Pet petByUuid(String uuid);
}
