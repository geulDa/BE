package com.CUK.geulDa.ai.service.ai;

import com.CUK.geulDa.ai.dto.CourseRecommendResponse;
import com.CUK.geulDa.ai.dto.RecommendRequest;
import com.CUK.geulDa.ai.service.search.CourseSearchService;
import com.CUK.geulDa.ai.service.util.DataTransformService;
import com.CUK.geulDa.ai.service.util.PlaceImageResolver;
import com.CUK.geulDa.domain.course.Course;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationEngine {

    private final ChatClient chatClient;
    private final DataTransformService dataTransformService;
    private final PlaceImageResolver placeImageResolver;
    private final CourseSearchService courseSearchService;
    private final AIPlaceGenerator aiPlaceGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<CourseRecommendResponse.PlaceDetail> selectBestPlaces(
            List<Course> candidates, RecommendRequest request, List<Course> mustVisitPlaces,
            int targetCount) {

        List<CourseRecommendResponse.PlaceDetail> result = new ArrayList<>();

        for (Course place : mustVisitPlaces) {
            result.add(new CourseRecommendResponse.PlaceDetail(
                    place.getId(),
                    place.getName(),
                    place.getAddress(),
                    place.getLatitude(),
                    place.getLongitude(),
                    place.getDescription(),
                    placeImageResolver.resolvePlaceImageUrl(place)
            ));
        }

        if (!mustVisitPlaces.isEmpty()) {
            log.debug("필수 방문지 {} 개 최종 포함: {}",
                    mustVisitPlaces.size(),
                    mustVisitPlaces.stream().map(Course::getName).collect(Collectors.joining(", ")));
        }

        int remainingCount = Math.max(0, targetCount - mustVisitPlaces.size());

        if (remainingCount == 0) {
            log.debug("필수 방문지만으로 추천 완료 ({}개)", result.size());
            return result;
        }

        if (mustVisitPlaces.size() > targetCount) {
            log.debug("필수 방문지({})가 목표 개수({})를 초과. 목표 개수만큼만 반환",
                    mustVisitPlaces.size(), targetCount);
            return result.subList(0, targetCount);
        }

        String prompt = String.format("""
                당신은 부천 지역 관광 전문가입니다. 다음 기준으로 최적의 장소 %d개를 선택하세요.

                [선택 기준]
                1. 여행 목적: %s
                2. 교통수단: %s (이동 거리/편의성 고려)
                3. 체류 시간: %s
                4. 장소 간 동선 효율성 (이동 거리 최소화)
                5. 장소의 인기도 및 품질 (popularityScore 참고)
                6. 카테고리 다양성 (음식점, 관광지, 문화시설 등 균형)

                [후보 장소 목록]
                %s

                [출력 형식]
                반드시 다음 JSON 형식으로만 응답하세요:
                {
                  "recommendations": [
                    {"placeId": 123},
                    {"placeId": 456}
                  ]
                }

                [필수 지침]
                - 동선을 고려해 가까운 장소들을 묶어서 선택
                - 인기도가 높은 장소 우선 (70점 이상 최우선)
                - 목적에 맞는 카테고리 우선 (데이트→카페/공원, 가족→공원/문화시설, 맛집→음식점)
                - 정확히 %d개 선택
                - placeId만 포함, 추가 설명 불필요
                """,
                remainingCount,
                dataTransformService.translatePurpose(request.travelPurpose()),
                dataTransformService.translateTransportation(request.transportation()),
                request.stayDuration() != null ? request.stayDuration() : "당일치기",
                candidates.stream()
                        .map(place -> String.format(
                                "ID: %d | %s | 카테고리: %s | 인기도: %d | 설명: %s | 좌표: (%.4f, %.4f)",
                                place.getId(),
                                place.getName(),
                                place.getCategory() != null ? place.getCategory() : "기타",
                                place.getPopularityScore() != null ? place.getPopularityScore() : 50,
                                place.getDescription() != null ? place.getDescription() : "정보 없음",
                                place.getLatitude(),
                                place.getLongitude()))
                        .collect(Collectors.joining("\n")),
                remainingCount
        );

        try {
            ChatResponse response = chatClient
                    .prompt(prompt)
                    .options(org.springframework.ai.chat.prompt.ChatOptions.builder()
                            .temperature(0.8)
                            .build())
                    .call()
                    .chatResponse();

            String aiResponse = response.getResult().getOutput().getText();
            List<CourseRecommendResponse.PlaceDetail> aiSelected =
                    parseAIRecommendation(aiResponse, candidates);
            result.addAll(aiSelected);

            return result;
        } catch (Exception e) {
            log.error("AI 모델 호출 실패, 폴백 사용", e);
            candidates.stream()
                    .sorted((p1, p2) -> {
                        Integer score1 = p1.getPopularityScore() != null ? p1.getPopularityScore() : 50;
                        Integer score2 = p2.getPopularityScore() != null ? p2.getPopularityScore() : 50;
                        return Integer.compare(score2, score1);
                    })
                    .limit(remainingCount)
                    .forEach(place -> result.add(new CourseRecommendResponse.PlaceDetail(
                            place.getId(),
                            place.getName(),
                            place.getAddress(),
                            place.getLatitude(),
                            place.getLongitude(),
                            place.getDescription(),
                            placeImageResolver.resolvePlaceImageUrl(place)
                    )));

            return result;
        }
    }

    private List<CourseRecommendResponse.PlaceDetail> parseAIRecommendation(
            String aiResponse, List<Course> candidates) {
        try {
            String jsonPart = extractJsonFromResponse(aiResponse);
            JsonNode root = objectMapper.readTree(jsonPart);

            List<CourseRecommendResponse.PlaceDetail> details = new ArrayList<>();
            JsonNode recommendations = root.get("recommendations");

            if (recommendations != null && recommendations.isArray()) {
                for (JsonNode rec : recommendations) {
                    Long placeId = rec.get("placeId").asLong();

                    candidates.stream()
                            .filter(place -> place.getId() != null && place.getId().equals(placeId))
                            .findFirst()
                            .ifPresent(place -> details.add(
                                    new CourseRecommendResponse.PlaceDetail(
                                            place.getId(),
                                            place.getName(),
                                            place.getAddress(),
                                            place.getLatitude(),
                                            place.getLongitude(),
                                            place.getDescription(),
                                            placeImageResolver.resolvePlaceImageUrl(place)
                                    )
                            ));
                }
            }

            if (!details.isEmpty()) {
                log.debug("AI 추천 성공: {} 장소 선택됨", details.size());
                return details;
            }

        } catch (Exception e) {
            log.warn("AI 추천 파싱 실패", e);
        }

        return List.of();
    }

    public Optional<Course> selectPlaceByContext(List<Course> candidates, String userRequest) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        String prompt = String.format("""
                사용자가 필수로 방문하고 싶은 장소를 요청했습니다: "%s"

                아래 후보 장소 목록에서 사용자의 요청에 가장 적합한 장소 1개를 선택하세요.

                [후보 장소 목록]
                %s

                [분석 가이드]
                - 사용자 요청의 맥락과 의도 파악
                - 장소의 이름, 카테고리, 설명을 종합적으로 분석
                - 가장 적합한 장소 1개만 선택

                [예시]
                요청: "카페 같은 곳" → 카테고리가 '카페'이거나 분위기 좋은 곳 선택
                요청: "조용한 곳" → '자연', '공원', '사찰' 등 힐링 장소 선택
                요청: "사진 찍기 좋은 곳" → 건축미/경관이 특별한 곳 선택
                요청: "밥 먹을 곳" → '음식점' 카테고리 선택
                요청: "아이들이랑" → 가족 단위 방문에 적합한 곳 선택

                [출력 형식]
                반드시 JSON 형식으로만 응답하세요:
                {
                  "placeId": 123,
                  "reason": "선택 이유 (1문장)"
                }

                만약 적합한 장소가 없다면:
                {
                  "placeId": null,
                  "reason": "적합한 장소 없음"
                }
                """,
                userRequest,
                candidates.stream()
                        .map(place -> String.format("ID: %d | 이름: %s | 카테고리: %s | 설명: %s",
                                place.getId(),
                                place.getName(),
                                place.getCategory() != null ? place.getCategory() : "없음",
                                place.getDescription() != null ? place.getDescription() : "없음"))
                        .collect(Collectors.joining("\n"))
        );

        try {
            ChatResponse response = chatClient
                    .prompt(prompt)
                    .call()
                    .chatResponse();

            String aiResponse = response.getResult().getOutput().getText();
            log.debug("AI 맥락 파악 응답: {}", aiResponse);

            String jsonPart = extractJsonFromResponse(aiResponse);
            JsonNode root = objectMapper.readTree(jsonPart);

            JsonNode placeIdNode = root.get("placeId");
            if (placeIdNode != null && !placeIdNode.isNull()) {
                Long selectedId = placeIdNode.asLong();
                String reason = root.has("reason") ? root.get("reason").asText() : "AI 선택";

                log.debug("AI 선택 결과: placeId={}, 이유={}", selectedId, reason);

                return candidates.stream()
                        .filter(place -> place.getId().equals(selectedId))
                        .findFirst();
            }

        } catch (Exception e) {
            log.error("AI 맥락 파악 실패", e);
        }

        return Optional.empty();
    }

    public boolean verifyPlaceRelevance(Course place, String userRequest) {
        String prompt = String.format("""
                사용자 요청: "%s"
                장소: %s (카테고리: %s, 설명: %s)

                이 장소가 사용자 요청과 관련이 있나요?

                [판단 기준]
                - 요청: "학교", "교육시설" → 장소 카테고리가 "교육시설"이어야 함
                - 요청: "카페" → 카테고리가 "카페"이거나 설명에 카페 관련 내용
                - 요청: "공원", "자연" → 카테고리가 "자연"
                - 요청: "쇼핑" → 카테고리가 "쇼핑" 또는 백화점/마트

                [출력]
                관련 있으면 true, 없으면 false만 출력하세요.
                """,
                userRequest,
                place.getName(),
                place.getCategory() != null ? place.getCategory() : "없음",
                place.getDescription() != null ? place.getDescription() : "없음"
        );

        try {
            ChatResponse response = chatClient
                    .prompt(prompt)
                    .call()
                    .chatResponse();

            String aiResponse = response.getResult().getOutput().getText().trim().toLowerCase();
            boolean isRelevant = aiResponse.contains("true");

            log.debug("AI 관련성 검증: {} - {} = {}",
                    userRequest, place.getName(), isRelevant);

            return isRelevant;

        } catch (Exception e) {
            log.error("AI 관련성 검증 실패, 안전하게 false 반환", e);
            return false;
        }
    }

    public CourseSearchService.MustVisitResult processMustVisitPlaceWithAI(
            List<Course> candidates, String mustVisitPlace) {

        CourseSearchService.MustVisitResult basicResult =
                courseSearchService.findMustVisitPlaces(candidates, mustVisitPlace);

        if (!basicResult.mustVisitPlaces().isEmpty()) {
            return basicResult;
        }

        log.debug("AI 맥락 파악 시작: '{}'", mustVisitPlace);
        List<Course> allVisiblePlaces = courseSearchService.findMustVisitPlaces(candidates, "").candidates();
        Optional<Course> aiSelectedPlace = selectPlaceByContext(allVisiblePlaces, mustVisitPlace);

        if (aiSelectedPlace.isPresent()) {
            Course mustVisit = aiSelectedPlace.get();
            log.debug("AI 맥락 파악 성공: {} (요청: '{}')", mustVisit.getName(), mustVisitPlace);

            List<Course> others = candidates.stream()
                    .filter(p -> !p.getId().equals(mustVisit.getId()))
                    .toList();
            return new CourseSearchService.MustVisitResult(List.of(mustVisit), others);
        }

        log.debug("벡터 스토어 의미론적 검색: '{}'", mustVisitPlace);
        Optional<Course> semanticMatch = courseSearchService.searchBySemanticSimilarity(mustVisitPlace);

        if (semanticMatch.isPresent()) {
            Course candidate = semanticMatch.get();

            if (verifyPlaceRelevance(candidate, mustVisitPlace)) {
                log.debug("의미론적 검색 성공: {} (요청: '{}')", candidate.getName(), mustVisitPlace);

                List<Course> others = candidates.stream()
                        .filter(p -> !p.getId().equals(candidate.getId()))
                        .toList();
                return new CourseSearchService.MustVisitResult(List.of(candidate), others);
            } else {
                log.debug("벡터 검색 결과가 요청과 관련 없음: {} (요청: '{}')",
                        candidate.getName(), mustVisitPlace);
            }
        }

        log.debug("AI로 주변 지역에서 '{}' 검색 시작", mustVisitPlace);
        List<Course> aiGeneratedPlaces = aiPlaceGenerator.generatePlacesByUserRequest(mustVisitPlace);

        if (!aiGeneratedPlaces.isEmpty()) {
            log.debug("AI가 주변 지역에서 {}개 장소 생성: {}",
                    aiGeneratedPlaces.size(),
                    aiGeneratedPlaces.stream()
                            .map(Course::getName)
                            .collect(Collectors.joining(", ")));

            Course mustVisit = aiGeneratedPlaces.get(0);
            return new CourseSearchService.MustVisitResult(List.of(mustVisit), candidates);
        }

        log.debug("모든 전략 실패: '{}'", mustVisitPlace);
        return new CourseSearchService.MustVisitResult(List.of(), candidates);
    }

    private String extractJsonFromResponse(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
