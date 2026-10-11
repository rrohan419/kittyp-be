package com.kittyp.timezone.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.timezone.dto.TimeZoneModel;
import com.kittyp.timezone.service.TimeZoneService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PublicTimeZoneController {

	private final TimeZoneService timeZoneService;
	private final ApiResponse<?> responseBuilder;

	@GetMapping(ApiUrl.PUBLIC_TIMEZONES)
	public ResponseEntity<SuccessResponse<PaginationModel<TimeZoneModel>>> searchTimeZones(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Integer pageNumber,
			@RequestParam(required = false) Integer pageSize) {
		return responseBuilder.buildSuccessResponse(
				timeZoneService.search(search, pageNumber, pageSize),
				ResponseMessage.SUCCESS,
				HttpStatus.OK);
	}
}
