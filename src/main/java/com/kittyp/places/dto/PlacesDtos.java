package com.kittyp.places.dto;

import java.util.List;

public final class PlacesDtos {

	private PlacesDtos() {
	}

	public record AutocompleteRequest(String query, String sessionToken) {
	}

	public record DetailsRequest(String placeId, String sessionToken) {
	}

	public record PlacePrediction(String placeId, String description) {
	}

	public record AutocompleteResponse(List<PlacePrediction> predictions) {
	}

	public record AddressComponent(String longName, String shortName, List<String> types) {
	}

	public record PlaceDetails(
			String name,
			String formattedAddress,
			List<AddressComponent> addressComponents,
			Double latitude,
			Double longitude) {
	}
}
