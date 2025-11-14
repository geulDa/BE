package com.CUK.geulDa.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GooglePlacesService {

    @Value("${geulda.google.places.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String PLACES_TEXT_SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String PLACES_NEARBY_SEARCH_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private static final String PLACES_DETAILS_URL = "https://places.googleapis.com/v1/";

    public GooglePlacesService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    public Optional<String> searchPlaceImageUrl(String placeName, String address, Double latitude, Double longitude) {
        try {
            String photoName = searchPlaceByText(placeName, address, latitude, longitude);
            if (photoName == null) {
                return Optional.empty();
            }

            String photoUrl = buildPhotoUrl(photoName, 800);
            return Optional.of(photoUrl);

        } catch (Exception e) {
            log.warn("이미지 조회 실패: placeName={}", placeName);
            return Optional.empty();
        }
    }

    private String searchPlaceByText(String placeName, String address, Double latitude, Double longitude) {
        try {
            String query = buildSearchQuery(placeName, address);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Goog-Api-Key", apiKey);
            headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.photos");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("textQuery", query);
            requestBody.put("languageCode", "ko");

            if (latitude != null && longitude != null) {
                Map<String, Object> locationBias = new HashMap<>();
                Map<String, Object> circle = new HashMap<>();
                Map<String, Double> center = new HashMap<>();
                center.put("latitude", latitude);
                center.put("longitude", longitude);
                circle.put("center", center);
                circle.put("radius", 1000.0);
                locationBias.put("circle", circle);
                requestBody.put("locationBias", locationBias);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    PLACES_TEXT_SEARCH_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode places = root.path("places");

            if (places.isEmpty() || places.isMissingNode()) {
                return null;
            }

            JsonNode photos = places.get(0).path("photos");
            if (!photos.isEmpty() && !photos.isMissingNode()) {
                return photos.get(0).path("name").asText();
            }

            return null;

        } catch (Exception e) {
            log.warn("Text Search 실패: {}", placeName);
            return null;
        }
    }

    private String buildSearchQuery(String placeName, String address) {
        if (address != null && !address.isBlank()) {
            return placeName + " " + address;
        }
        return placeName;
    }

    private String buildPhotoUrl(String photoName, int maxHeight) {
        String url = String.format("https://places.googleapis.com/v1/%s/media?maxHeightPx=%d&key=%s",
                photoName, maxHeight, apiKey);
        log.debug("Photo URL 생성: {}", url);
        return url;
    }

    public Optional<String> searchPlaceImageUrlByCoordinates(String placeName, String address, Double latitude, Double longitude) {
        try {
            String keyword = buildSearchQuery(placeName, address);
            log.debug("Nearby Search (New API) 시작: keyword='{}', lat={}, lng={}", keyword, latitude, longitude);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Goog-Api-Key", apiKey);
            headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.photos");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("languageCode", "ko");

            Map<String, Object> locationRestriction = new HashMap<>();
            Map<String, Object> circle = new HashMap<>();
            Map<String, Double> center = new HashMap<>();
            center.put("latitude", latitude);
            center.put("longitude", longitude);
            circle.put("center", center);
            circle.put("radius", 300.0);
            locationRestriction.put("circle", circle);
            requestBody.put("locationRestriction", locationRestriction);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    PLACES_NEARBY_SEARCH_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode places = root.path("places");

            if (places.isEmpty() || places.isMissingNode()) {
                log.debug("Nearby Search 결과 없음: keyword='{}'", keyword);
                return Optional.empty();
            }

            String placeId = places.get(0).path("id").asText();
            String foundName = places.get(0).path("displayName").path("text").asText();
            log.debug("Nearby Search 성공: keyword='{}' -> found='{}' (placeId={})", keyword, foundName, placeId);

            // 사진이 있으면 첫 번째 사진의 name 반환
            JsonNode photos = places.get(0).path("photos");
            if (!photos.isEmpty() && !photos.isMissingNode()) {
                String photoName = photos.get(0).path("name").asText();
                log.debug("Nearby Search: 사진 정보 발견 -> photoName={}", photoName);

                String photoUrl = buildPhotoUrl(photoName, 800);
                log.debug("Nearby Search: 이미지 URL 생성 완료 -> {}", photoUrl);
                return Optional.of(photoUrl);
            }

            log.debug("Nearby Search: 사진 없음 (placeId={})", placeId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Nearby Search 예외: placeName={}, error={}", placeName, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
