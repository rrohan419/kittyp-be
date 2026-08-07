package com.kittyp.visit.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.service.VisitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ParentVisitController {

    private final VisitService visitService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.PET_VISITS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<VisitModel>>> petVisits(@PathVariable String uuid) {
        return success(visitService.listParentPetVisits(uuid, email()));
    }

    @GetMapping(ApiUrl.USER_VISITS_MINE)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<VisitModel>>> myVisits() {
        return success(visitService.listMyParentVisits(email()));
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return responseBuilder.buildSuccessResponse(data, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
