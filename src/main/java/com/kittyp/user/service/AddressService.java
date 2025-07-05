package com.kittyp.user.service;

import java.util.List;

import com.kittyp.user.dto.AddressSaveDto;
import com.kittyp.user.dto.AddressUpdateDto;
import com.kittyp.user.models.AddressModel;

public interface AddressService {
    
    List<AddressModel> getAllAddresses(String userUuid);

    AddressModel getAddressByUuid(String userUuid, String uuid);

    AddressModel saveAddress(String userUuid, AddressSaveDto addressSaveDto);

    AddressModel updateAddress(String userUuid, AddressUpdateDto addressUpdateDto);

    void deleteAddress(String userUuid, String uuid);
}
