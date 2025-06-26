package com.kittyp.user.service;

import java.util.List;

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

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class AddressServiceImpl implements AddressService {

    private final Mapper mapper;
    private final AddressDao addressDao;
    private final UserDao userDao;

    @Override
    public List<AddressModel> getAllAddresses(String userUuid) {
        List<Address> addresses = addressDao.getAllAddresses(userUuid);

        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }

        return addresses.stream().map(address -> mapper.convert(address, AddressModel.class)).toList();
    }

    @Override
    public AddressModel getAddressByUuid(String userUuid, String uuid) {
        Address address = addressDao.getAddressByUuid(uuid);
        if (address == null) {
            return null;
        }

        if(address.getUser() !=null && !address.getUser().getUuid().equals(userUuid)) {
            throw new CustomException("User UUID : %s is not the owner of address uuid provided", null);
        }

        return mapper.convert(address, AddressModel.class);
    }

    @Transactional()
    @Override
    public AddressModel saveAddress(String userUuid, AddressSaveDto addressSaveDto) {

        User  user = userDao.userByUuid(userUuid);
        if (user == null) {
            throw new CustomException("User with UUID : %s does not exist", HttpStatus.NOT_FOUND);
        }

        
        Address address = mapper.convert(addressSaveDto, Address.class);
        address.setUser(user);
        Address savedAddress = addressDao.saveUpdateAddress(address);
        
        user.getAddresses().add(savedAddress);
        userDao.saveUser(user);

        return mapper.convert(savedAddress, AddressModel.class);
    }

    @Override
    public AddressModel updateAddress(String userUuid, AddressUpdateDto addressUpdateDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateAddress'");
    }

    @Override
    public void deleteAddress(String userUuid, String uuid) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAddress'");
    }

}
