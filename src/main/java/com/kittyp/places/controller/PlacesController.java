package com.kittyp.places.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.places.dto.PlacesDtos.AutocompleteRequest;
import com.kittyp.places.dto.PlacesDtos.AutocompleteResponse;
import com.kittyp.places.dto.PlacesDtos.DetailsRequest;
import com.kittyp.places.dto.PlacesDtos.PlaceDetails;
import com.kittyp.places.service.PlacesProxyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class PlacesController {

	private final PlacesProxyService placesProxyService;
	private final ApiResponse<?> responseBuilder;

	@PostMapping(ApiUrl.PLACES_AUTOCOMPLETE)
	public ResponseEntity<SuccessResponse<AutocompleteResponse>> autocomplete(
			@RequestBody AutocompleteRequest request) {
		return responseBuilder.buildSuccessResponse(
				placesProxyService.autocomplete(request == null ? null : request.query(),
						request == null ? null : request.sessionToken()),
				ResponseMessage.SUCCESS, HttpStatus.OK);
	}

	@PostMapping(ApiUrl.PLACES_DETAILS)
	public ResponseEntity<SuccessResponse<PlaceDetails>> details(@RequestBody DetailsRequest request) {
		return responseBuilder.buildSuccessResponse(
				placesProxyService.details(request == null ? null : request.placeId(),
						request == null ? null : request.sessionToken()),
				ResponseMessage.SUCCESS, HttpStatus.OK);
	}
}
