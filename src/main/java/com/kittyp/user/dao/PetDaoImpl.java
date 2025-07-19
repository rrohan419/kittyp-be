package com.kittyp.user.dao;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.entity.Pet;
import com.kittyp.user.repository.PetsRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PetDaoImpl implements PetDao {

  private final PetsRepository petsRepository;
  private final Environment env;

  @Override
  public Pet savePets(Pet pet) {
    try {
      return petsRepository.save(pet);
    } catch (Exception e) {
      throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
          HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
  }

  @Override
  public void deleteByUuid(String uuid) {
    try {
      petsRepository.deleteByUuid(uuid);
    } catch (Exception e) {
      throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
          HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
  }

  @Override
  public Pet petByUuid(String uuid) {
    try {
      return petsRepository.findByUuid(uuid);
    } catch (Exception e) {
      throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
          HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
  }

}
