package com.kittyp.places.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.logging.PiiMasker;
import com.kittyp.notification.service.NotificationInputSanitizer;
import com.kittyp.places.dto.PlacesDtos.AddressComponent;
import com.kittyp.places.dto.PlacesDtos.AutocompleteResponse;
import com.kittyp.places.dto.PlacesDtos.PlaceDetails;
import com.kittyp.places.dto.PlacesDtos.PlacePrediction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PlacesProxyService {

	private static final String AUTOCOMPLETE = "https://places.googleapis.com/v1/places:autocomplete";
	private static final String DETAILS = "https://places.googleapis.com/v1/places/{placeId}";
	private static final String DETAILS_FIELD_MASK = "displayName,formattedAddress,addressComponents,location";
	private static final String API_KEY_HEADER = "X-Goog-Api-Key";
	private static final String FIELD_MASK_HEADER = "X-Goog-FieldMask";
	private static final String UNAVAILABLE_MESSAGE = "Address search is temporarily unavailable";
	private static final String FAILED_MESSAGE = "Places lookup failed";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String referer;

	public PlacesProxyService(
			RestClient restClient,
			ObjectMapper objectMapper,
			@Value("${google.maps.api.key:}") String apiKey,
			@Value("${google.maps.api.referer:}") String referer) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.referer = referer == null ? "" : referer.trim();
	}

	public AutocompleteResponse autocomplete(String query, String sessionToken) {
		NotificationInputSanitizer.rejectCrLf(query, "query");
		NotificationInputSanitizer.rejectCrLf(sessionToken, "sessionToken");
		if (query == null) {
			throw new IllegalArgumentException("query is required");
		}
		String input = query.trim();
		if (input.length() < 2 || input.length() > 80) {
			throw new IllegalArgumentException("query must be 2–80 characters");
		}
		requireApiKey("autocomplete");
		log.info("Places autocomplete query={}", PiiMasker.mask(input));

		ObjectNode body = objectMapper.createObjectNode();
		body.put("input", input);
		ArrayNode regions = body.putArray("includedRegionCodes");
		regions.add("in");
		String token = blankToNull(sessionToken);
		if (token != null) {
			body.put("sessionToken", token);
		}

		ResponseEntity<String> response = restClient.post()
				.uri(AUTOCOMPLETE)
				.headers(this::applyGoogleHeaders)
				.body(body.toString())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, res) -> {
					// handled below so the error body can be inspected without leaking the key
				})
				.toEntity(String.class);

		JsonNode root = readResponse(response, "autocomplete");
		List<PlacePrediction> predictions = new ArrayList<>();
		JsonNode suggestions = root.path("suggestions");
		if (suggestions.isArray()) {
			for (JsonNode suggestion : suggestions) {
				JsonNode prediction = suggestion.path("placePrediction");
				String placeId = prediction.path("placeId").asText("");
				String description = prediction.path("text").path("text").asText("");
				if (!placeId.isBlank()) {
					predictions.add(new PlacePrediction(placeId, description));
				}
			}
		}
		return new AutocompleteResponse(predictions);
	}

	public PlaceDetails details(String placeId, String sessionToken) {
		NotificationInputSanitizer.rejectCrLf(placeId, "placeId");
		NotificationInputSanitizer.rejectCrLf(sessionToken, "sessionToken");
		if (placeId == null || placeId.isBlank()) {
			throw new IllegalArgumentException("placeId is required");
		}
		requireApiKey("details");

		String uri = UriComponentsBuilder.fromUriString(DETAILS)
				.queryParam("sessionToken", blankToNull(sessionToken))
				.buildAndExpand(placeId.trim())
				.encode()
				.toUriString();

		ResponseEntity<String> response = restClient.get()
				.uri(uri)
				.headers(headers -> {
					applyGoogleHeaders(headers);
					headers.set(FIELD_MASK_HEADER, DETAILS_FIELD_MASK);
				})
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, res) -> {
					// handled below so the error body can be inspected without leaking the key
				})
				.toEntity(String.class);

		JsonNode result = readResponse(response, "details");
		JsonNode location = result.path("location");
		Double lat = location.path("latitude").isNumber() ? location.path("latitude").asDouble() : null;
		Double lng = location.path("longitude").isNumber() ? location.path("longitude").asDouble() : null;
		List<AddressComponent> components = new ArrayList<>();
		JsonNode comps = result.path("addressComponents");
		if (comps.isArray()) {
			for (JsonNode c : comps) {
				List<String> types = new ArrayList<>();
				c.path("types").forEach(t -> types.add(t.asText()));
				components.add(new AddressComponent(c.path("longText").asText(""), c.path("shortText").asText(""),
						types));
			}
		}
		return new PlaceDetails(
				result.path("displayName").path("text").asText(""),
				result.path("formattedAddress").asText(""),
				components,
				lat,
				lng);
	}

	private void applyGoogleHeaders(HttpHeaders headers) {
		headers.set(API_KEY_HEADER, apiKey);
		if (!referer.isBlank()) {
			headers.set(HttpHeaders.REFERER, referer);
		}
	}

	private void requireApiKey(String operation) {
		if (apiKey.isBlank()) {
			log.error("Places {} unavailable: google.maps.api.key is blank", operation);
			throw new CustomException(UNAVAILABLE_MESSAGE, HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	private JsonNode readResponse(ResponseEntity<String> response, String operation) {
		JsonNode root;
		try {
			String body = response.getBody();
			root = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
		} catch (Exception e) {
			log.error("Places {} returned an unreadable response: httpStatus={}", operation, response.getStatusCode());
			throw new CustomException(FAILED_MESSAGE, HttpStatus.BAD_GATEWAY, e);
		}
		if (response.getStatusCode().isError()) {
			throw googleFailure(root, operation, response.getStatusCode());
		}
		return root;
	}

	private CustomException googleFailure(JsonNode root, String operation, HttpStatusCode httpStatus) {
		JsonNode error = root.path("error");
		String status = error.path("status").asText("");
		log.error("Places {} rejected by Google: httpStatus={} status={} error={}", operation, httpStatus.value(),
				status.isBlank() ? "MISSING" : status, withoutApiKey(error.path("message").asText("")));
		if (httpStatus.value() == HttpStatus.FORBIDDEN.value()
				|| httpStatus.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
			return new CustomException(UNAVAILABLE_MESSAGE, HttpStatus.SERVICE_UNAVAILABLE);
		}
		return new CustomException(FAILED_MESSAGE, HttpStatus.BAD_GATEWAY);
	}

	private String withoutApiKey(String message) {
		if (message == null || message.isBlank() || apiKey.isBlank()) {
			return message == null ? "" : message;
		}
		return message.replace(apiKey, "***");
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
