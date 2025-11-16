package com.CUK.geulDa.ai.service.search;

import com.CUK.geulDa.ai.mcp.BucheonTourMcpServer;
import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseSearchService {

    private final BucheonTourMcpServer mcpServer;
    private final CourseService courseService;

    public List<Course> searchPlaces(double latitude, double longitude, double radius, String purpose) {
        Map<String, Object> searchParams = Map.of(
                "latitude", latitude,
                "longitude", longitude,
                "radius", radius,
                "purpose", purpose
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>)
                mcpServer.executeTool("search_places", searchParams);

        @SuppressWarnings("unchecked")
        List<Course> searchedPlaces = (List<Course>) searchResult.get("places");

        return searchedPlaces != null ? new ArrayList<>(searchedPlaces) : new ArrayList<>();
    }

    public List<Course> searchWithFallback(double lat, double lon, double radius, String purpose) {
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

    public Optional<Course> searchBySemanticSimilarity(String query) {
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

    public MustVisitResult findMustVisitPlaces(List<Course> candidates, String mustVisitPlace) {
        if (mustVisitPlace == null || mustVisitPlace.isBlank()) {
            return new MustVisitResult(List.of(), candidates);
        }

        log.debug("필수 방문지 요청: '{}'", mustVisitPlace);

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

    private MustVisitResult findSinglePlace(List<Course> candidates, String mustVisitPlace) {
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

        List<Course> allPlaces = courseService.getAllVisibleCourses();
        Optional<Course> exactMatchInDb = allPlaces.stream()
                .filter(place -> place.getName().equals(mustVisitPlace))
                .findFirst();

        if (exactMatchInDb.isPresent()) {
            Course mustVisit = exactMatchInDb.get();
            log.debug("전체 DB에서 정확한 매칭 성공: {}", mustVisit.getName());
            return new MustVisitResult(List.of(mustVisit), candidates);
        }

        log.debug("모든 전략 실패: '{}'", mustVisitPlace);
        return new MustVisitResult(List.of(), candidates);
    }

    public record MustVisitResult(List<Course> mustVisitPlaces, List<Course> candidates) {}
}
