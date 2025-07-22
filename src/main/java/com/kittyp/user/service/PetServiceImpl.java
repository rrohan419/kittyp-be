package com.kittyp.user.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.user.dao.PetDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dto.PetDetailDto;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.entity.User;
import com.kittyp.user.models.PetModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetDao petDao;
    private final Mapper mapper;
    private final UserDao userDao;

    @Transactional
    @Override
    public PetModel addPet(PetDetailDto petDetailDto, String email) {

        User petOwner = userDao.userByEmail(email);

        log.info("Adding new pet for email={}", petOwner.getEmail());

        Pet newPet = mapper.convert(petDetailDto, Pet.class);
        newPet.setUuid(UUID.randomUUID().toString());
        newPet = petDao.savePets(newPet);
        
        petOwner.addPet(newPet);
        userDao.saveUser(petOwner);
        log.info("Successfully added pet with uuid={} for email={}", newPet.getUuid(), petOwner.getEmail());

        return mapper.convert(newPet, PetModel.class);
    }

    @Override
    public List<PetModel> petsByUserUuid(String email) {
        log.info("Fetching pets for email={}", email);

        User petOwner = userDao.userByEmail(email);
        List<Pet> userPets = petOwner.getPets();

        if (userPets == null || userPets.isEmpty()) {
            log.warn("No pets found for email={}", email);
            return List.of(); // Return empty list instead of throwing exception
        }

        log.info("Found {} pet(s) for email={}", userPets.size(), email);

        return mapper.convertToList(userPets, PetModel.class);
    }

    @Transactional
    @Override
    public void deletePetByUuid(String uuid, String email) {

        User petOwner = userDao.userByEmail(email);

        Boolean isOwnerPet = petOwner.getPets().stream().anyMatch(pet -> pet.getUuid().equals(uuid));

        if(!isOwnerPet) {
            log.info("Pet not found with uuid={}, for owner email {}", uuid, petOwner.getEmail());
            throw new CustomException("pet not found by uuid : "+uuid, HttpStatus.NOT_FOUND);
        }

        // Pet pet = petDao.petByUuid(uuid);

        // if(pet == null) {
        //     throw new CustomException("pet not found by uuid : "+uuid, HttpStatus.NOT_FOUND);
        // }

        log.info("Deleting pet with uuid={}", uuid);
        petDao.deleteByUuid(uuid);
        log.info("Deleted pet with uuid={}", uuid);
    }

    @Transactional
    @Override
    public PetModel updatePet(PetDetailDto petDetailDto, String uuid, String email) {
        User petOwner = userDao.userByEmail(email);

        log.info("Updating pet with uuid={} for email={}", uuid, petOwner.getEmail());

        // Check if pet belongs to user
        Pet existingPet = petOwner.getPets().stream()
                .filter(pet -> pet.getUuid().equals(uuid))
                .findFirst()
                .orElseThrow(() -> new CustomException("Pet not found by uuid: " + uuid, HttpStatus.NOT_FOUND));

        // Update pet details
        existingPet.setName(petDetailDto.getName());
        existingPet.setProfilePicture(petDetailDto.getProfilePicture());
        existingPet.setBreed(petDetailDto.getBreed());
        existingPet.setAge(petDetailDto.getAge());
        existingPet.setWeight(petDetailDto.getWeight());
        existingPet.setActivityLevel(petDetailDto.getActivityLevel());
        existingPet.setGender(petDetailDto.getGender());
        existingPet.setNeutered(petDetailDto.isNeutered());
        existingPet.setCurrentFoodBrand(petDetailDto.getCurrentFoodBrand());
        existingPet.setHealthConditions(petDetailDto.getHealthConditions());
        existingPet.setAllergies(petDetailDto.getAllergies());

        Pet updatedPet = petDao.savePets(existingPet);
        
        log.info("Successfully updated pet with uuid={} for email={}", updatedPet.getUuid(), petOwner.getEmail());

        return mapper.convert(updatedPet, PetModel.class);
    }

    @Override
    public PetModel updatePetProfilePicture(String petUuid, String profilePictureUrl) {
        Pet pet = petDao.petByUuid(petUuid);
        if (pet == null) {
            throw new CustomException("Pet not found by uuid: " + petUuid, HttpStatus.NOT_FOUND);
        }

        log.info("Updating profile picture for pet with uuid={}", petUuid);
        pet.setProfilePicture(profilePictureUrl);
        Pet updatedPet = petDao.savePets(pet);
        
        log.info("Successfully updated profile picture for pet with uuid={}", updatedPet.getUuid());

        return mapper.convert(updatedPet, PetModel.class);
    }
}
