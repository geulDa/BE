package com.CUK.geulDa.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
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
    private final Bucket rateLimitBucket;

    private static final String PLACES_TEXT_SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String PLACES_NEARBY_SEARCH_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private static final String PLACES_DETAILS_URL = "https://places.googleapis.com/v1/";

    public GooglePlacesService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        // Rate Limiting: 분당 최대 50회 API 호출
        Bandwidth limit = Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1)));
        this.rateLimitBucket = Bucket.builder()
                .addLimit(limit)
                .build();

        log.info("Google Places API Rate Limiter 초기화: 분당 50회");
    }
    public Optional<String> searchPlaceImageUrl(String placeName, String address, Double latitude, Double longitude) {
        try {
            // API 호출
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
            // Rate Limiting 체크
            if (!rateLimitBucket.tryConsume(1)) {
                log.warn("Google Places API Rate Limit 초과: placeName={}", placeName);
                return null;
            }

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

        } catch (HttpClientErrorException e) {
            return handleHttpError(e, placeName, "Text Search");
        } catch (HttpServerErrorException e) {
            return handleHttpError(e, placeName, "Text Search");
        } catch (Exception e) {
            log.warn("Text Search 실패: {}, error={}", placeName, e.getMessage());
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
            // Rate Limiting 체크
            if (!rateLimitBucket.tryConsume(1)) {
                log.warn("Google Places API Rate Limit 초과 (Nearby): placeName={}", placeName);
                return Optional.empty();
            }

            // API 호출
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

        } catch (HttpClientErrorException e) {
            handleHttpErrorForOptional(e, placeName, "Nearby Search");
            return Optional.empty();
        } catch (HttpServerErrorException e) {
            handleHttpErrorForOptional(e, placeName, "Nearby Search");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Nearby Search 예외: placeName={}, error={}", placeName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private String handleHttpError(RuntimeException e, String placeName, String apiType) {
        HttpStatus statusCode = null;
        String responseBody = null;

        if (e instanceof HttpClientErrorException clientError) {
            statusCode = HttpStatus.valueOf(clientError.getStatusCode().value());
            responseBody = clientError.getResponseBodyAsString();
        } else if (e instanceof HttpServerErrorException serverError) {
            statusCode = HttpStatus.valueOf(serverError.getStatusCode().value());
            responseBody = serverError.getResponseBodyAsString();
        }

        checkQuotaExceeded(statusCode, responseBody, placeName, apiType);
        return null;
    }

    private void handleHttpErrorForOptional(RuntimeException e, String placeName, String apiType) {
        HttpStatus statusCode = null;
        String responseBody = null;

        if (e instanceof HttpClientErrorException clientError) {
            statusCode = HttpStatus.valueOf(clientError.getStatusCode().value());
            responseBody = clientError.getResponseBodyAsString();
        } else if (e instanceof HttpServerErrorException serverError) {
            statusCode = HttpStatus.valueOf(serverError.getStatusCode().value());
            responseBody = serverError.getResponseBodyAsString();
        }

        checkQuotaExceeded(statusCode, responseBody, placeName, apiType);
    }

    private void checkQuotaExceeded(HttpStatus statusCode, String responseBody, String placeName, String apiType) {
        // HTTP 429 Too Many Requests 감지
        if (statusCode == HttpStatus.TOO_MANY_REQUESTS) {
            log.error("[Google Places API 할당량 초과] API={}, placeName={}, statusCode={}",
                    apiType, placeName, statusCode);
            log.error("응답 내용: {}", responseBody);
            return;
        }

        // 응답 본문에서 RESOURCE_EXHAUSTED 감지
        if (responseBody != null && responseBody.contains("RESOURCE_EXHAUSTED")) {
            log.error("[Google Places API 할당량 초과] API={}, placeName={}, error=RESOURCE_EXHAUSTED",
                    apiType, placeName);
            log.error("응답 내용: {}", responseBody);
            return;
        }

        // 기타 에러
        log.warn("{} 실패: placeName={}, statusCode={}, error={}",
                apiType, placeName, statusCode, responseBody);
    }
}
