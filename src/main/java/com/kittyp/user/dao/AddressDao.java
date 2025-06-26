package com.kittyp.user.dao;

import java.util.List;

import com.kittyp.user.entity.Address;


public interface AddressDao {
    
    Address getAddressByUuid(String uuid);

    Address saveUpdateAddress(Address address);

    void deleteAddress(String uuid);

    List<Address> getAllAddresses(String userUuid);
}
