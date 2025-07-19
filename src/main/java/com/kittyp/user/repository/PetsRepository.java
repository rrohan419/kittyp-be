package com.kittyp.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.user.entity.Pet;

public interface  PetsRepository extends JpaRepository<Pet, Long>{
    
    void deleteByUuid(String uuid);

    Pet findByUuid(String uuid);
}
