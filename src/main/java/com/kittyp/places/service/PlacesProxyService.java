package com.kittyp.places.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	private static final String AUTOCOMPLETE = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
	private static final String DETAILS = "https://maps.googleapis.com/maps/api/place/details/json";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;

	public PlacesProxyService(
			RestClient restClient,
			ObjectMapper objectMapper,
			@Value("${google.maps.api.key:}") String apiKey) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
		this.apiKey = apiKey == null ? "" : apiKey.trim();
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
		if (apiKey.isBlank()) {
			log.warn("Places autocomplete skipped: google.maps.api.key is blank");
			return new AutocompleteResponse(List.of());
		}
		log.info("Places autocomplete query={}", PiiMasker.mask(input));
		String uri = UriComponentsBuilder.fromUriString(AUTOCOMPLETE)
				.queryParam("input", input)
				.queryParam("components", "country:in")
				.queryParam("key", apiKey)
				.queryParam("sessiontoken", blankToNull(sessionToken))
				.build(true)
				.toUriString();
		JsonNode root = getJson(uri);
		List<PlacePrediction> predictions = new ArrayList<>();
		JsonNode array = root.path("predictions");
		if (array.isArray()) {
			for (JsonNode n : array) {
				predictions.add(new PlacePrediction(n.path("place_id").asText(""), n.path("description").asText("")));
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
		if (apiKey.isBlank()) {
			throw new CustomException("Places is not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}
		String uri = UriComponentsBuilder.fromUriString(DETAILS)
				.queryParam("place_id", placeId.trim())
				.queryParam("fields", "name,formatted_address,address_component,geometry")
				.queryParam("key", apiKey)
				.queryParam("sessiontoken", blankToNull(sessionToken))
				.build(true)
				.toUriString();
		JsonNode result = getJson(uri).path("result");
		JsonNode loc = result.path("geometry").path("location");
		Double lat = loc.path("lat").isNumber() ? loc.path("lat").asDouble() : null;
		Double lng = loc.path("lng").isNumber() ? loc.path("lng").asDouble() : null;
		List<AddressComponent> components = new ArrayList<>();
		JsonNode comps = result.path("address_components");
		if (comps.isArray()) {
			for (JsonNode c : comps) {
				List<String> types = new ArrayList<>();
				c.path("types").forEach(t -> types.add(t.asText()));
				components.add(new AddressComponent(c.path("long_name").asText(""), c.path("short_name").asText(""),
						types));
			}
		}
		return new PlaceDetails(
				result.path("name").asText(""),
				result.path("formatted_address").asText(""),
				components,
				lat,
				lng);
	}

	private JsonNode getJson(String uri) {
		try {
			String body = restClient.get().uri(uri).retrieve().body(String.class);
			return objectMapper.readTree(body == null ? "{}" : body);
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException("Places lookup failed", HttpStatus.BAD_GATEWAY, e);
		}
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
