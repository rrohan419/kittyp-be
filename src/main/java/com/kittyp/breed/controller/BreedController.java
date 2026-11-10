package com.kittyp.breed.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.breed.dto.BreedModel;
import com.kittyp.breed.service.BreedService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class BreedController {
    private final BreedService service;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.PUBLIC_BREEDS)
    public ResponseEntity<SuccessResponse<PaginationModel<BreedModel>>> search(
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize) {
        return responseBuilder.buildSuccessResponse(service.search(species, search, pageNumber, pageSize),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
