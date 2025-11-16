package com.CUK.geulDa.ai.service.util;

import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.course.service.CourseService;
import com.CUK.geulDa.domain.place.service.GooglePlacesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceImageResolver {

    private final GooglePlacesService googlePlacesService;
    private final CourseService courseService;

    public String resolvePlaceImageUrl(Course course) {
        if (course == null) {
            return null;
        }

        try {
            Optional<String> imageUrl = googlePlacesService.searchPlaceImageUrl(
                    course.getName(),
                    course.getAddress(),
                    course.getLatitude(),
                    course.getLongitude()
            );

            if (imageUrl.isPresent()) {
                courseService.updatePlaceImage(course.getId(), imageUrl.get());
                log.debug("최신 이미지 조회 및 DB 업데이트: {}", course.getName());
                return imageUrl.get();
            }

            if (course.getLatitude() != null && course.getLongitude() != null) {
                imageUrl = googlePlacesService.searchPlaceImageUrlByCoordinates(
                        course.getName(),
                        course.getAddress(),
                        course.getLatitude(),
                        course.getLongitude()
                );

                if (imageUrl.isPresent()) {
                    courseService.updatePlaceImage(course.getId(), imageUrl.get());
                    log.debug("좌표 기반 이미지 조회 및 DB 업데이트: {}", course.getName());
                    return imageUrl.get();
                }
            }

            if (course.getPlaceImg() != null && !course.getPlaceImg().isEmpty()) {
                log.debug("API 실패/Rate Limit, DB 이미지 사용: {}", course.getName());
                return course.getPlaceImg();
            }

        } catch (Exception e) {
            log.warn("이미지 조회 중 오류, DB 조회 시도: {}", course.getName());
            if (course.getPlaceImg() != null && !course.getPlaceImg().isEmpty()) {
                return course.getPlaceImg();
            }
        }

        log.debug("이미지 조회 실패, null 반환: {}", course.getName());
        return null;
    }
}
