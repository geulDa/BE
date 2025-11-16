package com.CUK.geulDa.ai.service.util;

import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.course.service.CourseService;
import com.CUK.geulDa.domain.place.service.GooglePlacesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceImageResolver {

    private final GooglePlacesService googlePlacesService;
    private final CourseService courseService;

    @Cacheable(value = "place-images", key = "#course.id", unless = "#result == null")
    public String resolvePlaceImageUrl(Course course) {
        if (course == null) {
            return null;
        }

        // 1. DB에 저장된 이미지가 있으면 즉시 반환
        if (course.getPlaceImg() != null && !course.getPlaceImg().isBlank()) {
            log.debug("DB에 저장된 이미지 사용: {}", course.getName());
            return course.getPlaceImg();
        }

        try {
            // 2. Google Places API를 통해 이름/주소로 검색
            Optional<String> imageUrl = googlePlacesService.searchPlaceImageUrl(
                    course.getName(),
                    course.getAddress(),
                    course.getLatitude(),
                    course.getLongitude()
            );

            if (imageUrl.isPresent()) {
                // 찾은 이미지를 DB에 업데이트하고 반환
                courseService.updatePlaceImage(course.getId(), imageUrl.get());
                log.debug("최신 이미지 조회 및 DB 업데이트: {}", course.getName());
                return imageUrl.get();
            }

            // 3. 이름/주소 검색 실패 시 좌표로 재검색
            if (course.getLatitude() != null && course.getLongitude() != null) {
                imageUrl = googlePlacesService.searchPlaceImageUrlByCoordinates(
                        course.getName(),
                        course.getAddress(),
                        course.getLatitude(),
                        course.getLongitude()
                );

                if (imageUrl.isPresent()) {
                    // 찾은 이미지를 DB에 업데이트하고 반환
                    courseService.updatePlaceImage(course.getId(), imageUrl.get());
                    log.debug("좌표 기반 이미지 조회 및 DB 업데이트: {}", course.getName());
                    return imageUrl.get();
                }
            }

        } catch (Exception e) {
            // API 호출 중 예외 발생 시 경고만 기록하고 넘어감 (null 반환)
            log.warn("Google Places API 이미지 조회 중 오류 발생: {}", e.getMessage());
        }

        // 4. 모든 방법으로도 이미지를 찾지 못한 경우
        log.debug("이미지 조회 실패, null 반환: {}", course.getName());
        return null;
    }
}
