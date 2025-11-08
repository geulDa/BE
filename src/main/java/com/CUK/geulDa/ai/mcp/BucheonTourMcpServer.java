package com.CUK.geulDa.ai.mcp;

import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.domain.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BucheonTourMcpServer implements McpServer {

    private final PlaceService placeService;
    private final VectorStore vectorStore;

    @Override
    public List<McpTool> getTools() {
        return List.of(
                new McpTool("search_places", "반경/키워드로 장소 검색"),
                new McpTool("get_place_details", "장소 상세 조회"),
                new McpTool("semantic_search", "자연어로 장소 검색 (챗봇 전용)")
        );
    }

    @Override
    public Object executeTool(String toolName, Map<String, Object> params) {
        try {
            return switch (toolName) {
                case "search_places" -> searchPlaces(params);
                case "get_place_details" -> getPlaceDetails(params);
                case "semantic_search" -> semanticSearch(params);
                default -> Map.of("error", "알 수 없는 도구: " + toolName);
            };
        } catch (Exception e) {
            log.error("MCP 도구 실행 실패: {}", toolName, e);
            return Map.of("error", "도구 실행 중 오류 발생: " + e.getMessage());
        }
    }

    private Map<String, Object> searchPlaces(Map<String, Object> params) {
        // 반경 검색
        if (params.containsKey("latitude") && params.containsKey("longitude")) {
            double lat = ((Number) params.get("latitude")).doubleValue();
            double lon = ((Number) params.get("longitude")).doubleValue();
            double radius = params.containsKey("radius") ?
                    ((Number) params.get("radius")).doubleValue() : 3.0;

            List<Place> places = placeService.findPlacesWithinRadius(lat, lon, radius);

            // 목적별 필터링
            if (params.containsKey("purpose")) {
                String purpose = (String) params.get("purpose");
                places = placeService.filterByPurpose(places, purpose);
            }

            return Map.of(
                    "places", places.stream().limit(10).toList(),
                    "searchType", "radius",
                    "count", Math.min(places.size(), 10)
            );
        }
        // 키워드 검색
        else if (params.containsKey("keyword")) {
            String keyword = (String) params.get("keyword");
            List<Place> places = placeService.findByKeyword(keyword);

            return Map.of(
                    "places", places.stream().limit(5).toList(),
                    "searchType", "keyword",
                    "count", Math.min(places.size(), 5)
            );
        }

        return Map.of("error", "검색 파라미터 부족 (latitude+longitude 또는 keyword 필요)");
    }

    private Map<String, Object> getPlaceDetails(Map<String, Object> params) {
        if (!params.containsKey("placeId")) {
            return Map.of("error", "placeId 파라미터가 필요합니다");
        }

        Long placeId = ((Number) params.get("placeId")).longValue();

        return placeService.findByIds(List.of(placeId)).stream()
                .findFirst()
                .map(place -> Map.of(
                        "place", place,
                        "tags", placeService.getTourPurposeTags(place)
                ))
                .orElse(Map.of("error", "존재하지 않는 장소 ID: " + placeId));
    }

    private Map<String, Object> semanticSearch(Map<String, Object> params) {
        if (!params.containsKey("query")) {
            return Map.of("error", "query 파라미터가 필요합니다");
        }

        String query = (String) params.get("query");

        try {
            // 벡터 검색
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(3).build()
            );

            List<Long> placeIds = results.stream()
                    .map(doc -> {
                        try {
                            return Long.parseLong(doc.getId());
                        } catch (NumberFormatException e) {
                            log.warn("잘못된 장소 ID 형식: {}", doc.getId());
                            return null;
                        }
                    })
                    .filter(id -> id != null)
                    .toList();

            if (placeIds.isEmpty()) {
                log.warn("벡터 검색 결과 없음, 키워드 검색으로 대체: {}", query);
                return searchPlaces(Map.of("keyword", query));
            }

            List<Place> places = placeService.findByIds(placeIds);

            return Map.of(
                    "places", places,
                    "searchType", "semantic",
                    "count", places.size()
            );
        } catch (Exception e) {
            log.warn("벡터 검색 실패, 키워드 검색으로 대체: {}", query, e);
            return searchPlaces(Map.of("keyword", query));
        }
    }
}
