package com.kittyp.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.user.dto.AddressSaveDto;
import com.kittyp.user.dto.AddressUpdateDto;
import com.kittyp.user.models.AddressModel;
import com.kittyp.user.service.AddressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AddressController {

    private final ApiResponse<?> responseBuilder;
    private final AddressService addressService;

    @GetMapping(ApiUrl.USER_ADDRESS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<AddressModel>>> getAllUserSavedAddress(@RequestParam String userUuid) {

        // String email =
        // SecurityContextHolder.getContext().getAuthentication().getName();

        List<AddressModel> response = addressService.getAllAddresses(userUuid);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.USER_ADDRESS_DETAIL)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<AddressModel>> getAddressByUuid(@RequestParam String userUuid,
            @RequestParam String addressUuid) {

        // String email =
        // SecurityContextHolder.getContext().getAuthentication().getName();

        AddressModel response = addressService.getAddressByUuid(userUuid, addressUuid);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @DeleteMapping(ApiUrl.USER_ADDRESS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<String>> deleteAddress(@RequestParam String userUuid,
            @RequestParam String addressUuid) {

        // String email =
        // SecurityContextHolder.getContext().getAuthentication().getName();

        addressService.deleteAddress(userUuid, addressUuid);
        return responseBuilder.buildSuccessResponse(ResponseMessage.SUCCESS, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PostMapping(ApiUrl.USER_ADDRESS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<AddressModel>> saveAddress(@RequestParam String userUuid,
            @Valid @RequestBody AddressSaveDto addressSaveDto) {

        // String email =
        // SecurityContextHolder.getContext().getAuthentication().getName();

        AddressModel response = addressService.saveAddress(userUuid, addressSaveDto);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PutMapping(ApiUrl.USER_ADDRESS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<AddressModel>> updateAddress(@RequestParam String userUuid,
            @RequestBody @Valid AddressUpdateDto addressUpdateDto) {

        // String email =
        // SecurityContextHolder.getContext().getAuthentication().getName();

        AddressModel response = addressService.updateAddress(userUuid, addressUpdateDto);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
