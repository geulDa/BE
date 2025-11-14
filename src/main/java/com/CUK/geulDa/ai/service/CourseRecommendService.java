package com.CUK.geulDa.ai.service;

import com.CUK.geulDa.ai.dto.CourseRecommendResponse;
import com.CUK.geulDa.ai.dto.RecommendRequest;
import com.CUK.geulDa.ai.dto.SessionData;
import com.CUK.geulDa.ai.mcp.BucheonTourMcpServer;
import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.course.service.CourseService;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRecommendService {

    // 가톨릭대학교 부천캠퍼스 기본 위치 (김수환관 기준)
    private static final double DEFAULT_LATITUDE = 37.4974496;
    private static final double DEFAULT_LONGITUDE = 126.8007892;

    private final CourseService courseService;
    private final ChatClient chatClient;
    private final BucheonTourMcpServer mcpServer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CourseRecommendResponse recommend(Member member, RecommendRequest request) {
        log.info("코스 추천 시작: 사용자={}, 목적={}, 교통수단={}",
            member.getId(), request.travelPurpose(), request.transportation());

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

            String normalizedTransportation = normalizeTransportation(request.transportation());
            String normalizedPurpose = normalizePurpose(request.travelPurpose());
            double radius = getRadius(normalizedTransportation);
            Map<String, Object> searchParams = Map.of(
                "latitude", userLat,
                "longitude", userLon,
                "radius", radius,
                "purpose", normalizedPurpose
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> searchResult = (Map<String, Object>)
                mcpServer.executeTool("search_places", searchParams);

            @SuppressWarnings("unchecked")
            List<Course> searchedPlaces = (List<Course>) searchResult.get("places");

            // 가변 리스트로 변환 (addAll을 위해 필요)
            List<Course> candidates = searchedPlaces != null ? new ArrayList<>(searchedPlaces) : new ArrayList<>();

            if (candidates.isEmpty()) {
                log.debug("초기 검색 결과 없음. 조건 완화 시도");
                candidates = searchWithFallback(userLat, userLon, radius, normalizedPurpose);
            }

            if (candidates.size() < 3) {
                log.debug("DB 검색 결과 부족 ({}개). AI로 장소 생성 시작", candidates.size());
                List<Course> aiGeneratedPlaces = generatePlacesWithAI(
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
                    translatePurpose(request.travelPurpose())
                );
                throw new BusinessException(ErrorCode.AI_NO_PLACES_FOUND, suggestion);
            }

            ParsedRequest parsed = parseNaturalLanguage(request.mustVisitPlace());
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
                MustVisitResult result = processMustVisitPlace(candidates,
                    parsed.cleanedMustVisitPlace());
                mustVisitPlaces = result.mustVisitPlaces();
                finalPlaces = result.candidates();

                if (!mustVisitPlaces.isEmpty()) {
                    log.debug("필수 방문지 확정: {} 개", mustVisitPlaces.size());
                }
            }

            List<CourseRecommendResponse.PlaceDetail> recommended =
                generateRecommendationWithAI(finalPlaces, request, mustVisitPlaces,
                    parsed.placeCount());

            String sessionId = UUID.randomUUID().toString();
            saveSession(sessionId, member.getId(), recommended,
                request.travelPurpose(), request.stayDuration(), request.transportation());
            String routeSummary = String.format("%s로 %d곳을 방문하는 %s 코스",
                translateTransportation(request.transportation()),
                recommended.size(),
                translatePurpose(request.travelPurpose())
            );

            double totalDistance = calculateTotalDistance(recommended, userLat, userLon);

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

    /**
     * 필수 방문지를 포함하여 AI 기반 장소 추천
     */
    private List<CourseRecommendResponse.PlaceDetail> generateRecommendationWithAI(
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
                place.getPlaceImg()
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
            translatePurpose(request.travelPurpose()),
            translateTransportation(request.transportation()),
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
                    .temperature(0.8)  // 다양성 증가 (기본 0.3 → 0.8)
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
                    place.getPlaceImg()
                )));

            return result;
        }
    }

    /**
     * AI 응답 파싱
     */
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
                                place.getPlaceImg()
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

    private String extractJsonFromResponse(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private record MustVisitResult(List<Course> mustVisitPlaces, List<Course> candidates) {

    }

    private record ParsedRequest(
        String cleanedMustVisitPlace,
        List<String> excludeCategories,
        int placeCount
    ) {

    }

    private ParsedRequest parseNaturalLanguage(String input) {
        if (input == null || input.isBlank()) {
            return new ParsedRequest("", List.of(), 4);
        }

        String cleaned = input;
        List<String> excludeCategories = new ArrayList<>();
        int placeCount = 4;

        // 제외 카테고리 파싱
        String[] excludePatterns = {"제외하고", "빼고", "제외", "뺴고", "말고"};
        for (String pattern : excludePatterns) {
            if (cleaned.contains(pattern)) {
                int idx = cleaned.indexOf(pattern);
                String before = cleaned.substring(0, idx).trim();

                String[] words = before.split("\\s+");
                if (words.length > 0) {
                    String category = words[words.length - 1]
                        .replace("은", "").replace("는", "")
                        .replace("을", "").replace("를", "").trim();
                    if (!category.isEmpty()) {
                        excludeCategories.add(category);
                    }
                }
                cleaned = cleaned.substring(idx + pattern.length()).trim();
            }
        }

        // 개수 파싱
        if (cleaned.contains("많이")) {
            placeCount = 10;
            cleaned = cleaned.replace("많이", "").trim();
        } else if (cleaned.matches(".*\\d+개.*")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)개");
            java.util.regex.Matcher matcher = pattern.matcher(cleaned);
            if (matcher.find()) {
                placeCount = Integer.parseInt(matcher.group(1));
                cleaned = cleaned.replaceAll("\\d+개(만)?", "").trim();
            }
        }

        placeCount = Math.max(1, Math.min(20, placeCount));

        cleaned = cleaned.replace("추천해줘", "")
            .replace("알려줘", "")
            .replace("보여줘", "")
            .replace("찾아줘", "").trim();

        return new ParsedRequest(cleaned, excludeCategories, placeCount);
    }

    private MustVisitResult processMustVisitPlace(List<Course> candidates, String mustVisitPlace) {
        if (mustVisitPlace == null || mustVisitPlace.isBlank()) {
            return new MustVisitResult(List.of(), candidates);
        }

        log.debug("필수 방문지 요청: '{}'", mustVisitPlace);

        // 복수 검색 키워드 감지
        boolean isMultipleRequest = mustVisitPlace.contains("모두") ||
            mustVisitPlace.contains("전부") ||
            mustVisitPlace.contains("모든") ||
            mustVisitPlace.contains("전체") ||
            mustVisitPlace.endsWith("들");

        if (isMultipleRequest) {
            log.debug("복수 장소 검색 모드 활성화");
            return findMultiplePlaces(candidates, mustVisitPlace);
        }

        return findSinglePlace(candidates, mustVisitPlace);
    }

    /**
     * 복수 장소 검색 (키워드로 관련된 모든 장소 찾기)
     */
    private MustVisitResult findMultiplePlaces(List<Course> candidates, String request) {
        String keyword = request
            .replace("만", "")
            .replace("모두", "")
            .replace("전부", "")
            .replace("모든", "")
            .replace("전체", "")
            .replace("들", "")
            .replace("알려줘", "")
            .replace("보여줘", "")
            .replace("찾아줘", "")
            .replace("관련", "")
            .replace("시설", "")
            .trim();

        log.debug("추출된 키워드: '{}'", keyword);

        List<Course> allPlaces = courseService.getAllVisibleCourses();
        List<Course> matchedPlaces = allPlaces.stream()
            .filter(place -> place.getName().contains(keyword))
            .toList();

        if (!matchedPlaces.isEmpty()) {
            log.debug("키워드 '{}' 매칭 성공: {}개 장소 발견", keyword, matchedPlaces.size());
            Set<Long> matchedIds = matchedPlaces.stream()
                .map(Course::getId)
                .collect(Collectors.toSet());
            List<Course> others = candidates.stream()
                .filter(p -> !matchedIds.contains(p.getId()))
                .toList();
            return new MustVisitResult(matchedPlaces, others);
        }

        log.debug("키워드 '{}'로 장소를 찾지 못함", keyword);
        return new MustVisitResult(List.of(), candidates);
    }

    /**
     * 단일 장소 검색
     */
    private MustVisitResult findSinglePlace(List<Course> candidates, String mustVisitPlace) {
        // 1단계: 정확한 이름 매칭 시도
        Optional<Course> exactMatch = candidates.stream()
            .filter(place -> place.getName().equals(mustVisitPlace))
            .findFirst();

        if (exactMatch.isPresent()) {
            Course mustVisit = exactMatch.get();
            log.debug("정확한 이름 매칭 성공: {}", mustVisit.getName());
            List<Course> others = candidates.stream()
                .filter(p -> !p.getId().equals(mustVisit.getId()))
                .toList();
            return new MustVisitResult(List.of(mustVisit), others);
        }

        // 2단계: 부분 이름 매칭 시도
        Optional<Course> partialMatch = candidates.stream()
            .filter(place -> place.getName().contains(mustVisitPlace) ||
                mustVisitPlace.contains(place.getName()))
            .findFirst();

        if (partialMatch.isPresent()) {
            Course mustVisit = partialMatch.get();
            log.debug("부분 이름 매칭 성공: {}", mustVisit.getName());
            List<Course> others = candidates.stream()
                .filter(p -> !p.getId().equals(mustVisit.getId()))
                .toList();
            return new MustVisitResult(List.of(mustVisit), others);
        }

        // 3단계: 전체 DB에서 정확한 이름 매칭
        List<Course> allPlaces = courseService.getAllVisibleCourses();
        Optional<Course> exactMatchInDb = allPlaces.stream()
            .filter(place -> place.getName().equals(mustVisitPlace))
            .findFirst();

        if (exactMatchInDb.isPresent()) {
            Course mustVisit = exactMatchInDb.get();
            log.debug("전체 DB에서 정확한 매칭 성공: {}", mustVisit.getName());
            return new MustVisitResult(List.of(mustVisit), candidates);
        }

        // 4단계: AI 맥락 파악
        log.debug("AI 맥락 파악 시작: '{}'", mustVisitPlace);
        List<Course> allVisiblePlaces = courseService.getAllVisibleCourses();
        Optional<Course> aiSelectedPlace = selectPlaceByAI(allVisiblePlaces, mustVisitPlace);

        if (aiSelectedPlace.isPresent()) {
            Course mustVisit = aiSelectedPlace.get();
            log.debug("AI 맥락 파악 성공: {} (요청: '{}')", mustVisit.getName(), mustVisitPlace);

            List<Course> others = candidates.stream()
                .filter(p -> !p.getId().equals(mustVisit.getId()))
                .toList();
            return new MustVisitResult(List.of(mustVisit), others);
        }

        // 5단계: 벡터 스토어 의미론적 검색
        log.debug("벡터 스토어 의미론적 검색: '{}'", mustVisitPlace);
        Optional<Course> semanticMatch = searchBySemanticSimilarity(mustVisitPlace);

        if (semanticMatch.isPresent()) {
            Course candidate = semanticMatch.get();

            if (isRelevantPlace(candidate, mustVisitPlace)) {
                log.debug("의미론적 검색 성공: {} (요청: '{}')", candidate.getName(), mustVisitPlace);

                List<Course> others = candidates.stream()
                    .filter(p -> !p.getId().equals(candidate.getId()))
                    .toList();
                return new MustVisitResult(List.of(candidate), others);
            } else {
                log.debug("벡터 검색 결과가 요청과 관련 없음: {} (요청: '{}')",
                    candidate.getName(), mustVisitPlace);
            }
        }

        // 6단계: AI로 주변 지역 실제 장소 생성
        log.debug("AI로 주변 지역에서 '{}' 검색 시작", mustVisitPlace);
        List<Course> aiGeneratedPlaces = generatePlacesByUserRequest(mustVisitPlace);

        if (!aiGeneratedPlaces.isEmpty()) {
            log.debug("AI가 주변 지역에서 {}개 장소 생성: {}",
                aiGeneratedPlaces.size(),
                aiGeneratedPlaces.stream()
                    .map(Course::getName)
                    .collect(Collectors.joining(", ")));

            Course mustVisit = aiGeneratedPlaces.get(0);
            return new MustVisitResult(List.of(mustVisit), candidates);
        }

        log.debug("모든 전략 실패: '{}'", mustVisitPlace);
        return new MustVisitResult(List.of(), candidates);
    }


    /**
     * AI로 애매모호한 요청 해석 및 장소 선택
     */
    private Optional<Course> selectPlaceByAI(List<Course> candidates, String userRequest) {
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

    /**
     * AI로 장소 관련성 검증
     */
    private boolean isRelevantPlace(Course place, String userRequest) {
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

    /**
     * 벡터 스토어 의미론적 검색
     */
    private Optional<Course> searchBySemanticSimilarity(String query) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> searchResult = (Map<String, Object>)
                mcpServer.executeTool("semantic_search", Map.of("query", query));

            if (searchResult.containsKey("error")) {
                log.warn("벡터 검색 실패: {}", searchResult.get("error"));
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<Course> places = (List<Course>) searchResult.get("places");

            if (places != null && !places.isEmpty()) {
                return Optional.of(places.get(0));
            }

        } catch (Exception e) {
            log.error("벡터 스토어 검색 실패", e);
        }

        return Optional.empty();
    }

    /**
     * AI로 사용자 요청에 맞는 실제 장소 생성
     */
    private List<Course> generatePlacesByUserRequest(String userRequest) {
        String prompt = String.format("""
            사용자가 필수로 방문하고 싶은 장소: "%s"
            
            부천시 또는 인근 지역(인천 계양/부평, 서울 구로/영등포, 경기 광명)에서
            사용자 요청에 맞는 **실제 존재하는 장소** 2-3곳을 찾아주세요.
            
            [요구사항]
            1. 반드시 실제로 존재하는 장소여야 합니다
            2. 정확한 주소와 좌표 필수
            3. 부천 중심(37.4985, 126.7822)에서 15km 이내
            4. 대중교통으로 1시간 이내 도달 가능
            5. 사용자 요청과 직접적으로 관련된 장소
            
            [우선순위]
            1순위: 부천시 내부
            2순위: 인천 계양구/부평구
            3순위: 서울 구로구/영등포구
            4순위: 경기 광명시
            
            [예시 해석]
            요청: "카페 같은 곳" → 부천의 유명 카페 (스타벅스, 투썸플레이스 등)
            요청: "놀이공원" → 인천 월미도 놀이공원
            요청: "박물관" → 부천로보파크, 부천교육박물관 등
            요청: "쇼핑" → 부천역 현대백화점, 뉴코아아울렛 등
            
            [출력 형식]
            반드시 다음 JSON 형식으로만 응답하세요:
            {
              "places": [
                {
                  "name": "장소명 (구체적으로)",
                  "address": "전체 주소",
                  "latitude": 위도,
                  "longitude": 경도,
                  "description": "장소 설명 (50자 이내)",
                  "category": "카테고리 (음식점/카페/문화시설/자연/쇼핑/교육시설 중 하나)"
                }
              ]
            }
            
            중요: 가짜 장소 금지! 실제 존재하며 영업 중인 장소만 추천하세요.
            """, userRequest);

        try {
            ChatResponse response = chatClient
                .prompt(prompt)
                .call()
                .chatResponse();

            String aiResponse = response.getResult().getOutput().getText();
            log.debug("AI 장소 생성 응답: {}", aiResponse);

            String jsonPart = extractJsonFromResponse(aiResponse);
            JsonNode root = objectMapper.readTree(jsonPart);
            JsonNode placesNode = root.get("places");

            if (placesNode == null || !placesNode.isArray()) {
                log.warn("AI 응답에 places 배열 없음");
                return List.of();
            }

            List<Course> generatedPlaces = new ArrayList<>();
            for (JsonNode placeNode : placesNode) {
                try {
                    Course place = Course.builder()
                        .name(placeNode.get("name").asText())
                        .address(placeNode.get("address").asText())
                        .latitude(placeNode.get("latitude").asDouble())
                        .longitude(placeNode.get("longitude").asDouble())
                        .description(placeNode.has("description") ?
                            placeNode.get("description").asText() : "AI 추천 장소")
                        .category(placeNode.has("category") ?
                            placeNode.get("category").asText() : "기타")
                        .tourPurposeTags("데이트,친구,가족")  // 기본 태그
                        .isHidden(false)
                        .popularityScore(50)
                        .dataSource("AI_GENERATED")  // AI 생성 표시
                        .placeImg(null)
                        .build();

                    // DB에 저장하여 ID 할당
                    Course savedPlace = courseService.saveCourse(place);
                    generatedPlaces.add(savedPlace);
                    log.debug("AI 생성: {} - {} ({})",
                        savedPlace.getName(), savedPlace.getAddress(), savedPlace.getCategory());

                } catch (Exception e) {
                    log.warn("장소 파싱 실패", e);
                }
            }

            return generatedPlaces;

        } catch (Exception e) {
            log.error("AI 장소 생성 실패", e);
            return List.of();
        }
    }

    public SessionData getSession(String sessionId) {
        String key = "ai:session:" + sessionId;

        try {
            Object session = redisTemplate.opsForValue().get(key);

            if (session == null) {
                throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                    "세션 ID: " + sessionId);
            }

            if (!(session instanceof SessionData)) {
                log.error("세션 데이터 타입 불일치: sessionId={}, type={}", sessionId,
                    session.getClass().getName());
                redisTemplate.delete(key);
                throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                    "세션이 만료되었거나 손상되었습니다: " + sessionId);
            }

            return (SessionData) session;

        } catch (SerializationException e) {
            log.error("세션 역직렬화 실패: sessionId={}", sessionId, e);
            redisTemplate.delete(key);
            throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                "세션이 만료되었거나 손상되었습니다: " + sessionId);
        }
    }

    private void saveSession(String sessionId, Long memberId,
        List<CourseRecommendResponse.PlaceDetail> places,
        String travelPurpose, String stayDuration, String transportation) {
        String key = "ai:session:" + sessionId;
        SessionData sessionData = new SessionData(
            memberId,
            places,
            LocalDateTime.now(),
            travelPurpose,
            stayDuration,
            transportation
        );

        redisTemplate.opsForValue().set(key, sessionData, Duration.ofMinutes(30));
        log.debug("세션 저장 완료: sessionId={}, memberId={}, 목적={}, 교통수단={}",
            sessionId, memberId, travelPurpose, transportation);
    }

    private double calculateTotalDistance(List<CourseRecommendResponse.PlaceDetail> places,
        double startLat, double startLon) {
        if (places.isEmpty()) {
            return 0.0;
        }

        double totalDistance = 0.0;
        double prevLat = startLat;
        double prevLon = startLon;

        for (CourseRecommendResponse.PlaceDetail place : places) {
            totalDistance += calculateDistance(prevLat, prevLon,
                place.latitude(), place.longitude());
            prevLat = place.latitude();
            prevLon = place.longitude();
        }

        return Math.round(totalDistance * 100.0) / 100.0;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String normalizeTransportation(String transportation) {
        return switch (transportation) {
            case "도보", "walk" -> "walk";
            case "대중교통", "transit" -> "transit";
            case "자동차", "car" -> "car";
            default -> {
                log.warn("알 수 없는 교통수단: {}, 기본값(transit) 사용", transportation);
                yield "transit";
            }
        };
    }

    private String normalizePurpose(String purpose) {
        return switch (purpose) {
            case "데이트", "dating" -> "dating";
            case "가족", "family" -> "family";
            case "친구", "friendship" -> "friendship";
            case "맛집", "맛집 탐방", "foodie" -> "foodie";
            default -> {
                log.warn("알 수 없는 여행 목적: {}, 원본값 사용", purpose);
                yield purpose;
            }
        };
    }

    private double getRadius(String transportation) {
        return switch (transportation) {
            case "walk" -> 1.0;
            case "transit" -> 3.0;
            case "car" -> 10.0;
            default -> 3.0;
        };
    }

    private String translateTransportation(String transportation) {
        return switch (transportation) {
            case "walk", "도보" -> "도보";
            case "transit", "대중교통" -> "대중교통";
            case "car", "자동차" -> "자동차";
            default -> transportation;
        };
    }

    private String translatePurpose(String purpose) {
        return switch (purpose) {
            case "dating", "데이트" -> "데이트";
            case "family", "가족" -> "가족";
            case "friendship", "친구" -> "친구";
            case "foodie", "맛집", "맛집 탐방" -> "맛집 탐방";
            default -> purpose;
        };
    }

    private List<Course> searchWithFallback(double lat, double lon, double radius, String purpose) {
        log.debug("반경 2배 확대 ({}km → {}km)", radius, radius * 2);
        Map<String, Object> params = Map.of(
            "latitude", lat,
            "longitude", lon,
            "radius", radius * 2,
            "purpose", purpose
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) mcpServer.executeTool("search_places",
            params);
        @SuppressWarnings("unchecked")
        List<Course> places = (List<Course>) result.get("places");

        if (places != null && places.size() >= 3) {
            log.debug("반경 확대로 {}개 장소 발견", places.size());
            return new ArrayList<>(places);
        }

        log.debug("목적 필터 제거");
        params = Map.of(
            "latitude", lat,
            "longitude", lon,
            "radius", radius * 2
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> result2 = (Map<String, Object>) mcpServer.executeTool("search_places",
            params);
        @SuppressWarnings("unchecked")
        List<Course> places2 = (List<Course>) result2.get("places");

        if (places2 != null && places2.size() >= 3) {
            log.debug("목적 필터 제거로 {}개 장소 발견", places2.size());
            return new ArrayList<>(places2);
        }

        log.debug("부천 전체 장소 검색");
        List<Course> allPlaces = courseService.getAllVisibleCourses();
        return allPlaces.stream().limit(10).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Course> generatePlacesWithAI(double centerLat, double centerLon,
        String purpose, String transportation,
        int count) {
        String prompt = String.format("""
                부천시 근처에서 '%s' 목적에 맞는 실제 존재하는 관광지/명소 %d곳을 추천해주세요.
                
                요구사항:
                1. 실제로 존재하는 장소여야 합니다 (가짜 장소 금지)
                2. 부천시 또는 인근 지역 (서울 서부, 인천 동부)
                3. 좌표는 (%.4f, %.4f) 중심으로 15km 이내
                4. %s로 이동 가능한 거리
                
                JSON 형식으로 응답하세요:
                {
                  "places": [
                    {
                      "name": "장소명",
                      "address": "전체 주소",
                      "latitude": 위도,
                      "longitude": 경도,
                      "description": "장소 설명 (50자 이내)",
                      "category": "카테고리 (관광지/음식점/카페/문화시설/공원 중 하나)",
                      "tourPurposeTags": "쉼표로 구분된 태그 (예: dating,family)"
                    }
                  ]
                }
                
                중요: 반드시 실제 존재하는 장소만 추천하세요. 정확한 주소와 좌표를 제공하세요.
                """,
            translatePurpose(purpose),
            count,
            centerLat,
            centerLon,
            translateTransportation(transportation)
        );

        try {
            ChatResponse response = chatClient
                .prompt(prompt)
                .call()
                .chatResponse();

            String aiResponse = response.getResult().getOutput().getText();
            log.debug("AI 장소 생성 응답: {}", aiResponse);

            return parseAIGeneratedPlaces(aiResponse, purpose);

        } catch (Exception e) {
            log.error("AI 장소 생성 실패", e);
            return List.of();
        }
    }

    private List<Course> parseAIGeneratedPlaces(String aiResponse, String purpose) {
        try {
            String jsonPart = extractJsonFromResponse(aiResponse);
            JsonNode root = objectMapper.readTree(jsonPart);
            JsonNode placesNode = root.get("places");

            if (placesNode == null || !placesNode.isArray()) {
                log.warn("AI 응답에 places 배열 없음");
                return List.of();
            }

            List<Course> generatedPlaces = new ArrayList<>();
            for (JsonNode placeNode : placesNode) {
                try {
                    // AI 생성 장소는 DB에 저장하지 않고 임시 ID 할당
                    Course place = Course.builder()
                        .name(placeNode.get("name").asText())
                        .address(placeNode.get("address").asText())
                        .latitude(placeNode.get("latitude").asDouble())
                        .longitude(placeNode.get("longitude").asDouble())
                        .description(placeNode.has("description") ?
                            placeNode.get("description").asText() : "AI 추천 장소")
                        .category(placeNode.has("category") ?
                            placeNode.get("category").asText() : "기타")
                        .tourPurposeTags(placeNode.has("tourPurposeTags") ?
                            placeNode.get("tourPurposeTags").asText() : purpose)
                        .isHidden(false)
                        .popularityScore(50)
                        .dataSource("AI_GENERATED")
                        .placeImg(null)
                        .build();

                    // DB에 저장하여 ID 할당
                    Course savedPlace = courseService.saveCourse(place);
                    generatedPlaces.add(savedPlace);
                    log.debug("AI 생성: {} ({})", place.getName(), place.getAddress());

                } catch (Exception e) {
                    log.warn("장소 파싱 실패", e);
                }
            }

            return generatedPlaces;

        } catch (Exception e) {
            log.error("AI 응답 파싱 실패", e);
            return List.of();
        }
    }

}