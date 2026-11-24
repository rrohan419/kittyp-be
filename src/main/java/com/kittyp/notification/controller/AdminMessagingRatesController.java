package com.kittyp.notification.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.notification.model.MessagingDtos.RateCardModel;
import com.kittyp.notification.model.MessagingDtos.RateCardsRequest;
import com.kittyp.notification.service.MessagingRateCardService;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping(ApiUrl.BASE_URL + ApiUrl.ADMIN_MESSAGING_RATES)
@RequiredArgsConstructor
@PreAuthorize(KeyConstant.IS_ROLE_ADMIN)
public class AdminMessagingRatesController {
    private final MessagingRateCardService service;
    private final ApiResponse<?> response;
    @GetMapping public ResponseEntity<SuccessResponse<List<RateCardModel>>> get() {
        return response.buildSuccessResponse(service.all(),ResponseMessage.SUCCESS,HttpStatus.OK);
    }
    @PutMapping public ResponseEntity<SuccessResponse<List<RateCardModel>>> put(
            @jakarta.validation.Valid @RequestBody RateCardsRequest request) {
        return response.buildSuccessResponse(service.replace(request.rates()),ResponseMessage.SUCCESS,HttpStatus.OK);
    }
}
