package com.kittyp.user.dao;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.user.entity.Address;
import com.kittyp.user.repository.AddressRepository;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class AddressDaoImpl implements AddressDao {

    private final Environment env;
    private final AddressRepository addressRepository;

    @Override
    public Address getAddressByUuid(String uuid) {
        try {
			return addressRepository.findByUuid(uuid);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public Address saveUpdateAddress(Address address) {
        try {
			return addressRepository.save(address);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public void deleteAddress(String uuid) {
        try {
			addressRepository.deleteByUuid(uuid);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    @Override
    public List<Address> getAllAddresses(String userUuid) {
        Sort sort = Sort.by(Direction.DESC, "createdAt");
		try {
			return addressRepository.findByUserUuid(userUuid, sort);
		} catch (Exception e) {
			throw new CustomException(env.getProperty(ExceptionConstant.ERROR_DATABASE_OPERATION),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }
    
}
