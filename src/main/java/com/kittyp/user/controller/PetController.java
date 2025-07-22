package com.kittyp.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.kittyp.user.dto.PetDetailDto;
import com.kittyp.user.dto.PetPhotosDto;
import com.kittyp.user.models.PetModel;
import com.kittyp.user.service.PetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PetController {
    
    private final PetService petService;
    private final ApiResponse<?> responseBuilder;

    @PostMapping(ApiUrl.PET_BASE_URL)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PetModel>> savePet(@RequestBody @Valid PetDetailDto detailDto) {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        PetModel response = petService.addPet(detailDto, email);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.PET_BASE_URL + "/user")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<PetModel>>> getUserPets() {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        List<PetModel> response = petService.petsByUserUuid(email);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PutMapping(ApiUrl.PET_BASE_URL)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PetModel>> updatePet(@RequestBody @Valid PetDetailDto detailDto, @RequestParam String uuid) {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        PetModel response = petService.updatePet(detailDto, uuid, email);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @DeleteMapping(ApiUrl.PET_BASE_URL + ApiUrl.PET_BY_UUID)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<String>> removePet(@PathVariable String uuid) {
        
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		
        petService.deletePetByUuid(uuid, email);
        return responseBuilder.buildSuccessResponse(null, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PatchMapping(ApiUrl.PET_BASE_URL + "/photos")
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<PetModel>> updatePetProfilePicture(@RequestParam String uuid,@RequestBody PetPhotosDto petPhotosDto) {
        
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        PetModel response = petService.updatePetProfilePicture(uuid, petPhotosDto);
        return responseBuilder.buildSuccessResponse(response, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
