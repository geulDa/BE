package com.CUK.geulDa.ai.service;

import com.CUK.geulDa.ai.dto.CourseRecommendResponse;
import com.CUK.geulDa.ai.dto.RecommendRequest;
import com.CUK.geulDa.ai.dto.SessionData;
import com.CUK.geulDa.ai.service.ai.AIPlaceGenerator;
import com.CUK.geulDa.ai.service.ai.AIRecommendationEngine;
import com.CUK.geulDa.ai.service.parser.NaturalLanguageParser;
import com.CUK.geulDa.ai.service.search.CourseSearchService;
import com.CUK.geulDa.ai.service.session.RecommendationSessionManager;
import com.CUK.geulDa.ai.service.util.DataTransformService;
import com.CUK.geulDa.ai.service.util.DistanceCalculator;
import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRecommendService {

    private static final double DEFAULT_LATITUDE = 37.4974496;
    private static final double DEFAULT_LONGITUDE = 126.8007892;

    private final DataTransformService dataTransformService;
    private final CourseSearchService courseSearchService;
    private final AIPlaceGenerator aiPlaceGenerator;
    private final NaturalLanguageParser naturalLanguageParser;
    private final AIRecommendationEngine aiRecommendationEngine;
    private final RecommendationSessionManager sessionManager;
    private final DistanceCalculator distanceCalculator;

    public CourseRecommendResponse recommend(RecommendRequest request) {
        log.info("코스 추천 시작: 목적={}, 교통수단={}",
                request.travelPurpose(), request.transportation());

        try {
            double userLat = DEFAULT_LATITUDE;
            double userLon = DEFAULT_LONGITUDE;

            if (request.userLatitude() != null && request.userLatitude() != 0.0 &&
                    request.userLongitude() != null && request.userLongitude() != 0.0) {
                userLat = request.userLatitude();
                userLon = request.userLongitude();
                log.debug("사용자 위치: ({}, {})", userLat, userLon);
            } else {
                log.debug("사용자 위치 정보 없음, 기본 위치 사용: 가톨릭대학교 부천캠퍼스 ({}, {})", userLat, userLon);
            }

            String normalizedTransportation = dataTransformService.normalizeTransportation(request.transportation());
            String normalizedPurpose = dataTransformService.normalizePurpose(request.travelPurpose());
            double radius = dataTransformService.getRadius(normalizedTransportation);

            List<Course> searchedPlaces = courseSearchService.searchPlaces(
                    userLat, userLon, radius, normalizedPurpose);

            List<Course> candidates = searchedPlaces != null ? new ArrayList<>(searchedPlaces) : new ArrayList<>();

            if (candidates.isEmpty()) {
                log.debug("초기 검색 결과 없음. 조건 완화 시도");
                candidates = courseSearchService.searchWithFallback(userLat, userLon, radius, normalizedPurpose);
            }

            if (candidates.size() < 3) {
                log.debug("DB 검색 결과 부족 ({}개). AI로 장소 생성 시작", candidates.size());
                List<Course> aiGeneratedPlaces = aiPlaceGenerator.generatePlacesWithAI(
                        userLat, userLon, normalizedPurpose, normalizedTransportation,
                        5 - candidates.size()
                );
                candidates.addAll(aiGeneratedPlaces);
                log.debug("AI 생성 장소 {}개 추가. 총 {}개 장소",
                        aiGeneratedPlaces.size(), candidates.size());
            }

            if (candidates.isEmpty()) {
                log.warn("추천 가능한 장소 없음 (목적: {}, 위치: {}, {})",
                        request.travelPurpose(), userLat, userLon);
                String suggestion = String.format(
                        "현재 위치(%s) 주변에서 '%s' 목적에 맞는 장소를 찾을 수 없습니다.\n" +
                                "- 검색 범위를 넓혀보세요\n" +
                                "- 여행 목적을 변경해보세요\n" +
                                "- 위치 정보를 확인해주세요",
                        userLat != 0 && userLon != 0 ? String.format("%.4f, %.4f", userLat, userLon)
                                : "기본 위치",
                        dataTransformService.translatePurpose(request.travelPurpose())
                );
                throw new BusinessException(ErrorCode.AI_NO_PLACES_FOUND, suggestion);
            }

            NaturalLanguageParser.ParsedRequest parsed =
                    naturalLanguageParser.parseUserRequest(request.mustVisitPlace());
            log.debug("자연어 파싱 결과 - 필수장소: '{}', 제외: {}, 개수: {}",
                    parsed.cleanedMustVisitPlace(), parsed.excludeCategories(), parsed.placeCount());

            if (!parsed.excludeCategories().isEmpty()) {
                int beforeSize = candidates.size();
                candidates = candidates.stream()
                        .filter(place -> {
                            String category = place.getCategory() != null ? place.getCategory() : "";
                            return parsed.excludeCategories().stream()
                                    .noneMatch(excluded -> category.contains(excluded) ||
                                            excluded.contains(category));
                        })
                        .toList();
                log.debug("제외 카테고리 필터링: {} → {} 장소", beforeSize, candidates.size());

                if (candidates.isEmpty()) {
                    throw new BusinessException(ErrorCode.AI_NO_PLACES_FOUND,
                            "'음식점 제외' 조건으로 인해 추천 가능한 장소가 없습니다.");
                }
            }

            List<Course> mustVisitPlaces = new ArrayList<>();
            List<Course> finalPlaces = candidates;

            if (!parsed.cleanedMustVisitPlace().isBlank()) {
                CourseSearchService.MustVisitResult result =
                        aiRecommendationEngine.processMustVisitPlaceWithAI(candidates,
                                parsed.cleanedMustVisitPlace());
                mustVisitPlaces = result.mustVisitPlaces();
                finalPlaces = result.candidates();

                if (!mustVisitPlaces.isEmpty()) {
                    log.debug("필수 방문지 확정: {} 개", mustVisitPlaces.size());
                }
            }

            List<CourseRecommendResponse.PlaceDetail> recommended =
                    aiRecommendationEngine.selectBestPlaces(finalPlaces, request, mustVisitPlaces,
                            parsed.placeCount());

            String sessionId = UUID.randomUUID().toString();
            sessionManager.saveSession(sessionId, null, recommended,
                    request.travelPurpose(), request.stayDuration(), request.transportation());
            String routeSummary = String.format("%s로 %d곳을 방문하는 %s 코스",
                    dataTransformService.translateTransportation(request.transportation()),
                    recommended.size(),
                    dataTransformService.translatePurpose(request.travelPurpose())
            );

            double totalDistance = distanceCalculator.calculateTotalDistance(recommended, userLat, userLon);

            log.info("코스 추천 완료: {} 장소, {:.2f}km", recommended.size(), totalDistance);

            return new CourseRecommendResponse(sessionId, recommended, routeSummary, totalDistance);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("코스 추천 실패", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR,
                    "AI 코스 추천 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    public SessionData getSession(String sessionId) {
        return sessionManager.getSession(sessionId);
    }
}
