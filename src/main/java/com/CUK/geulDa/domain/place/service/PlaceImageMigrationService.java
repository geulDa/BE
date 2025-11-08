package com.CUK.geulDa.domain.place.service;

import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.domain.place.dto.PlaceMigrationResult;
import com.CUK.geulDa.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceImageMigrationService {

    private final PlaceRepository placeRepository;
    private final GooglePlacesService googlePlacesService;

    public PlaceMigrationResult migrateAllPlaceImages() {
        log.info("Place 이미지 마이그레이션 시작");

        List<Place> places = placeRepository.findAll();
        int totalCount = places.size();
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        List<String> failedPlaces = new ArrayList<>();
        Map<Long, String> updatedImages = new HashMap<>();

        for (int i = 0; i < places.size(); i++) {
            Place place = places.get(i);

            try {
                if (place.getLatitude() == null || place.getLongitude() == null) {
                    skippedCount++;
                    failedPlaces.add(place.getName() + " (좌표 없음)");
                    continue;
                }

                var imageUrlOpt = googlePlacesService.searchPlaceImageUrl(
                        place.getName(),
                        place.getAddress(),
                        place.getLatitude(),
                        place.getLongitude()
                );

                if (imageUrlOpt.isPresent()) {
                    String newImageUrl = imageUrlOpt.get();
                    migrateSinglePlaceTransaction(place.getId(), newImageUrl);
                    updatedImages.put(place.getId(), newImageUrl);
                    successCount++;
                } else {
                    var retryImageUrlOpt = googlePlacesService.searchPlaceImageUrlByCoordinates(
                            place.getName(),
                            place.getAddress(),
                            place.getLatitude(),
                            place.getLongitude()
                    );

                    if (retryImageUrlOpt.isPresent()) {
                        String newImageUrl = retryImageUrlOpt.get();
                        migrateSinglePlaceTransaction(place.getId(), newImageUrl);
                        updatedImages.put(place.getId(), newImageUrl);
                        successCount++;
                    } else {
                        failCount++;
                        failedPlaces.add(place.getName());
                        log.warn("이미지 검색 실패: {}", place.getName());
                    }
                }

                TimeUnit.MILLISECONDS.sleep(200);

            } catch (Exception e) {
                failCount++;
                failedPlaces.add(place.getName());
                log.error("마이그레이션 오류 - {}: {}", place.getName(), e.getMessage());
            }
        }

        log.info("Place 이미지 마이그레이션 완료 - 전체: {}, 성공: {}, 실패: {}, 스킵: {}",
                totalCount, successCount, failCount, skippedCount);

        return new PlaceMigrationResult(totalCount, successCount, failCount, skippedCount, failedPlaces, updatedImages);
    }

    @Transactional
    private void migrateSinglePlaceTransaction(Long placeId, String newImageUrl) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소 ID: " + placeId));
        updatePlaceImage(place, newImageUrl);
        placeRepository.save(place);
    }

    @Transactional
    public PlaceMigrationResult migrateSinglePlace(Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소 ID: " + placeId));

        List<String> failedPlaces = new ArrayList<>();
        Map<Long, String> updatedImages = new HashMap<>();

        if (place.getLatitude() == null || place.getLongitude() == null) {
            failedPlaces.add(place.getName() + " (좌표 없음)");
            return new PlaceMigrationResult(1, 0, 0, 1, failedPlaces, updatedImages);
        }

        var imageUrlOpt = googlePlacesService.searchPlaceImageUrl(
                place.getName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude()
        );

        if (imageUrlOpt.isPresent()) {
            String newImageUrl = imageUrlOpt.get();
            updatePlaceImage(place, newImageUrl);
            placeRepository.save(place);

            updatedImages.put(place.getId(), newImageUrl);
            log.info("단일 마이그레이션 성공: {}", place.getName());
            return new PlaceMigrationResult(1, 1, 0, 0, failedPlaces, updatedImages);
        } else {
            failedPlaces.add(place.getName());
            log.warn("단일 마이그레이션 실패: {}", place.getName());
            return new PlaceMigrationResult(1, 0, 1, 0, failedPlaces, updatedImages);
        }
    }

    private void updatePlaceImage(Place place, String newImageUrl) {
        try {
            var field = Place.class.getDeclaredField("placeImg");
            field.setAccessible(true);
            field.set(place, newImageUrl);
        } catch (Exception e) {
            log.error("placeImg 필드 업데이트 실패", e);
            throw new RuntimeException("이미지 URL 업데이트 실패", e);
        }
    }
}
