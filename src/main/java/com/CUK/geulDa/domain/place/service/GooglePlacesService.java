package com.CUK.geulDa.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Service
@Slf4j
public class GooglePlacesService {

    @Value("${geulda.google.places.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String PLACES_TEXT_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json";
    private static final String PLACES_NEARBY_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    private static final String PLACES_PHOTO_URL = "https://maps.googleapis.com/maps/api/place/photo";

    public GooglePlacesService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    public Optional<String> searchPlaceImageUrl(String placeName, String address, Double latitude, Double longitude) {
        try {
            String placeId = searchPlaceByText(placeName, address, latitude, longitude);
            if (placeId == null) {
                return Optional.empty();
            }

            String photoReference = getPhotoReference(placeId);
            if (photoReference == null) {
                return Optional.empty();
            }

            String photoUrl = buildPhotoUrl(photoReference, 800);
            return Optional.of(photoUrl);

        } catch (Exception e) {
            log.error("Google Places API 호출 실패: {}", placeName, e);
            return Optional.empty();
        }
    }

    private String searchPlaceByText(String placeName, String address, Double latitude, Double longitude) {
        try {
            String query = buildSearchQuery(placeName, address);

            URI uri = UriComponentsBuilder
                    .fromUriString(PLACES_TEXT_SEARCH_URL)
                    .queryParam("query", query)
                    .queryParam("location", latitude + "," + longitude)
                    .queryParam("radius", 1000)
                    .queryParam("language", "ko")
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                return null;
            }

            JsonNode results = root.path("results");
            if (results.isEmpty()) {
                return null;
            }

            return results.get(0).path("place_id").asText();

        } catch (Exception e) {
            log.error("Text Search 실패: {}", placeName, e);
            return null;
        }
    }

    private String buildSearchQuery(String placeName, String address) {
        if (address != null && !address.isBlank()) {
            return placeName + " " + address;
        }
        return placeName;
    }

    private String getPhotoReference(String placeId) {
        try {
            String detailsUrl = "https://maps.googleapis.com/maps/api/place/details/json";

            URI uri = UriComponentsBuilder
                    .fromUriString(detailsUrl)
                    .queryParam("place_id", placeId)
                    .queryParam("fields", "photos")
                    .queryParam("language", "ko")
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                return null;
            }

            JsonNode photos = root.path("result").path("photos");
            if (photos.isEmpty()) {
                return null;
            }

            return photos.get(0).path("photo_reference").asText();

        } catch (Exception e) {
            log.error("Place Details 조회 실패: {}", placeId, e);
            return null;
        }
    }

    private String buildPhotoUrl(String photoReference, int maxWidth) {
        return UriComponentsBuilder
                .fromUriString(PLACES_PHOTO_URL)
                .queryParam("photoreference", photoReference)
                .queryParam("maxwidth", maxWidth)
                .queryParam("key", apiKey)
                .build()
                .toUriString();
    }

    public Optional<String> searchPlaceImageUrlByCoordinates(String placeName, String address, Double latitude, Double longitude) {
        try {
            String keyword = buildSearchQuery(placeName, address);

            URI uri = UriComponentsBuilder
                    .fromUriString(PLACES_NEARBY_SEARCH_URL)
                    .queryParam("location", latitude + "," + longitude)
                    .queryParam("radius", 300)
                    .queryParam("keyword", keyword)
                    .queryParam("language", "ko")
                    .queryParam("key", apiKey)
                    .build()
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);

            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                return Optional.empty();
            }

            JsonNode results = root.path("results");
            if (results.isEmpty()) {
                return Optional.empty();
            }

            String placeId = results.get(0).path("place_id").asText();
            String photoReference = getPhotoReference(placeId);

            if (photoReference == null) {
                return Optional.empty();
            }

            return Optional.of(buildPhotoUrl(photoReference, 800));

        } catch (Exception e) {
            log.error("Nearby Search 실패: {}", placeName, e);
            return Optional.empty();
        }
    }
}
