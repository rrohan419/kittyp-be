package com.kittyp.user.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.Mapper;
import com.kittyp.user.dao.AddressDao;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.dto.AddressSaveDto;
import com.kittyp.user.dto.AddressUpdateDto;
import com.kittyp.user.entity.Address;
import com.kittyp.user.entity.User;
import com.kittyp.user.models.AddressModel;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AddressServiceImpl implements AddressService {

    private static final Logger logger = LoggerFactory.getLogger(AddressServiceImpl.class);

    private final Mapper mapper;
    private final AddressDao addressDao;
    private final UserDao userDao;

    @Override
    public List<AddressModel> getAllAddresses(String userUuid) {
        logger.info("Fetching all addresses for user UUID: {}", userUuid);

        List<Address> addresses = addressDao.getAllAddresses(userUuid);

        if (addresses.isEmpty()) {
            logger.info("No addresses found for user UUID: {}", userUuid);
            return List.of();
        }

        logger.info("Found {} addresses for user UUID: {}", addresses.size(), userUuid);
        return addresses.stream().map(address -> mapper.convert(address, AddressModel.class)).toList();
    }

    @Override
    public AddressModel getAddressByUuid(String userUuid, String uuid) {
        logger.info("Fetching address by UUID: {} for user UUID: {}", uuid, userUuid);

        Address address = addressDao.getAddressByUuid(uuid);
        if (address == null) {
            logger.warn("Address not found for UUID: {}", uuid);
            return null;
        }

        if (address.getUser() != null && !address.getUser().getUuid().equals(userUuid)) {
            logger.error("User UUID: {} is not the owner of address UUID: {}", userUuid, uuid);
            throw new CustomException(String.format("User UUID : %s is not the owner of address uuid provided", userUuid), HttpStatus.FORBIDDEN);
        }

        logger.info("Address UUID: {} found and owned by user UUID: {}", uuid, userUuid);
        return mapper.convert(address, AddressModel.class);
    }

    @Transactional
    @Override
    public AddressModel saveAddress(String userUuid, AddressSaveDto addressSaveDto) {
        logger.info("Saving new address for user UUID: {}", userUuid);

        User user = userDao.userByUuid(userUuid);
        if (user == null) {
            logger.error("User not found for UUID: {}", userUuid);
            throw new CustomException("User with UUID : %s does not exist", HttpStatus.NOT_FOUND);
        }

        Address address = mapper.convert(addressSaveDto, Address.class);
        address.setUuid(UUID.randomUUID().toString());
        address.setUser(user);

        Address savedAddress = addressDao.saveUpdateAddress(address);
        logger.info("Address saved with UUID: {} for user UUID: {}", savedAddress.getUuid(), userUuid);

        // user.getAddresses().add(savedAddress);
        // userDao.saveUser(user);
        // logger.info("User updated with new address. User UUID: {}, Address UUID: {}", userUuid, savedAddress.getUuid());

        return mapper.convert(savedAddress, AddressModel.class);
    }

    @Transactional
    @Override
    public AddressModel updateAddress(String userUuid, AddressUpdateDto addressUpdateDto) {
        logger.info("Updating address UUID: {} for user UUID: {}", addressUpdateDto.getUuid(), userUuid);

        Address existingAddress = addressDao.getAddressByUuid(addressUpdateDto.getUuid());
        if (existingAddress == null) {
            logger.warn("Address with UUID: {} not found", addressUpdateDto.getUuid());
            throw new CustomException("Address with UUID : %s does not exist", HttpStatus.NOT_FOUND);
        }

        if (existingAddress.getUser() == null || !existingAddress.getUser().getUuid().equals(userUuid)) {
            logger.error("User UUID: {} is not the owner of address UUID: {}", userUuid, addressUpdateDto.getUuid());
            throw new CustomException("User UUID : %s is not the owner of the address", HttpStatus.FORBIDDEN);
        }

        if (addressUpdateDto.getName() != null && !addressUpdateDto.getName().isBlank()) {
            existingAddress.setName(addressUpdateDto.getName());
        }

        if (addressUpdateDto.getStreet() != null && !addressUpdateDto.getStreet().isBlank()) {
            existingAddress.setStreet(addressUpdateDto.getStreet());
        }

        if (addressUpdateDto.getCity() != null && !addressUpdateDto.getCity().isBlank()) {
            existingAddress.setCity(addressUpdateDto.getCity());
        }

        if (addressUpdateDto.getState() != null && !addressUpdateDto.getState().isBlank()) {
            existingAddress.setState(addressUpdateDto.getState());
        }

        if (addressUpdateDto.getPostalCode() != null && !addressUpdateDto.getPostalCode().isBlank()) {
            existingAddress.setPostalCode(addressUpdateDto.getPostalCode());
        }

        if (addressUpdateDto.getCountry() != null && !addressUpdateDto.getCountry().isBlank()) {
            existingAddress.setCountry(addressUpdateDto.getCountry());
        }

        if (addressUpdateDto.getAddressType() != null) {
            existingAddress.setAddressType(addressUpdateDto.getAddressType());
        }

        if (addressUpdateDto.getFormattedAddress() != null && !addressUpdateDto.getFormattedAddress().isBlank()) {
            existingAddress.setFormattedAddress(addressUpdateDto.getFormattedAddress());
        }

        if (addressUpdateDto.getPhoneNumber() != null && !addressUpdateDto.getPhoneNumber().isBlank()) {
            existingAddress.setPhoneNumber(addressUpdateDto.getPhoneNumber());
        }
        
        Address updated = addressDao.saveUpdateAddress(existingAddress);
        logger.info("Successfully updated address UUID: {} for user UUID: {}", updated.getUuid(), userUuid);

        return mapper.convert(updated, AddressModel.class);
    }

    @Transactional
    @Override
    public void deleteAddress(String userUuid, String uuid) {
        logger.info("Deleting address UUID: {} for user UUID: {}", uuid, userUuid);

        Address address = addressDao.getAddressByUuid(uuid);
        if (address == null) {
            logger.warn("Address with UUID: {} not found", uuid);
            throw new CustomException("Address with UUID : %s does not exist", HttpStatus.NOT_FOUND);
        }

        if (address.getUser() == null || !address.getUser().getUuid().equals(userUuid)) {
            logger.error("User UUID: {} is not the owner of address UUID: {}", userUuid, uuid);
            throw new CustomException("User UUID : %s is not authorized to delete this address", HttpStatus.FORBIDDEN);
        }

        addressDao.deleteAddress(address.getUuid());
        logger.info("Successfully deleted address UUID: {} for user UUID: {}", uuid, userUuid);
    }

}
