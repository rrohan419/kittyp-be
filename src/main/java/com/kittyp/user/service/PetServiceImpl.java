package com.kittyp.user.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.service.S3StorageService;
import com.kittyp.common.util.Mapper;
import com.kittyp.common.util.SafePhotoUrl;
import com.kittyp.user.dao.PetDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dto.PetDetailDto;
import com.kittyp.user.dto.PetPhotosDto;
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
    private final S3StorageService s3StorageService;

    @Transactional
    @Override
    public PetModel addPet(PetDetailDto petDetailDto, String email) {

        User petOwner = userDao.userByEmail(email);

        log.info("Adding new pet for email={}", petOwner.getEmail());

        Pet newPet = mapper.convert(petDetailDto, Pet.class);
        newPet.setProfilePicture(SafePhotoUrl.requireHttps(newPet.getProfilePicture()));
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
            return List.of();
        }

        log.info("Found {} pet(s) for email={}", userPets.size(), email);

        return mapper.convertToList(userPets, PetModel.class);
    }

    @Transactional
    @Override
    public void deletePetByUuid(String uuid, String email) {

        User petOwner = userDao.userByEmail(email);

        Pet petToDelete = petOwner.getPets().stream().filter(pet -> pet.getUuid().equals(uuid)).findFirst()
                .orElseThrow(() -> {
                    log.info("Pet not found with uuid={}, for owner email {}", uuid, petOwner.getEmail());
                    throw new CustomException("pet not found by uuid : " + uuid, HttpStatus.NOT_FOUND);
                });

        log.info("Hiding pet uuid={} from parent email={} (row kept for clinic/medical history)", uuid,
                petOwner.getEmail());
        // Detach from parent account only — never hard-delete (visits/clinic records stay).
        petToDelete.setHiddenFromParent(true);
        petOwner.getPets().remove(petToDelete);
        userDao.saveUser(petOwner);
        log.info("Pet uuid={} detached from parent; database row retained", uuid);
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
        existingPet.setProfilePicture(SafePhotoUrl.requireHttps(petDetailDto.getProfilePicture()));
        existingPet.setBreed(petDetailDto.getBreed());
        existingPet.setType(petDetailDto.getType());
        existingPet.setDateOfBirth(petDetailDto.getDateOfBirth());
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
    public PetModel updatePetProfilePicture(String petUuid, PetPhotosDto petPhotosDto) {
        Pet pet = petDao.petByUuid(petUuid);
        if (pet == null) {
            throw new CustomException("Pet not found by uuid: " + petUuid, HttpStatus.NOT_FOUND);
        }

        if (petPhotosDto == null || petPhotosDto.getPhotos() == null || petPhotosDto.getPhotos().isEmpty()) {
            throw new CustomException("At least one photo URL is required", HttpStatus.BAD_REQUEST);
        }
        log.info("Updating profile picture for pet with uuid={}", petUuid);
        String primary = SafePhotoUrl.requireHttps(petPhotosDto.getPhotos().get(0));
        pet.setProfilePicture(primary);

        Set<String> mutablePhotos = (pet.getPhotos() != null) ? pet.getPhotos() : new HashSet<>();
        for (String photo : petPhotosDto.getPhotos()) {
            mutablePhotos.add(SafePhotoUrl.requireHttps(photo));
        }
        pet.setPhotos(mutablePhotos);

        Pet updatedPet = petDao.savePets(pet);

        log.info("Successfully updated profile picture for pet with uuid={}", updatedPet.getUuid());

        return mapper.convert(updatedPet, PetModel.class);
    }
}
