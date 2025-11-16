package com.CUK.geulDa.ai.service.ai;

import com.CUK.geulDa.ai.service.util.DataTransformService;
import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.course.service.CourseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIPlaceGenerator {

    private final ChatClient chatClient;
    private final CourseService courseService;
    private final DataTransformService dataTransformService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Course> generatePlacesByUserRequest(String userRequest) {
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

            List<Course> coursesToSave = new ArrayList<>();
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
                            .tourPurposeTags("데이트,친구,가족")
                            .isHidden(false)
                            .popularityScore(50)
                            .dataSource("AI_GENERATED")
                            .placeImg(null)
                            .build();
                    coursesToSave.add(place);
                } catch (Exception e) {
                    log.warn("장소 파싱 실패", e);
                }
            }

            return courseService.saveAllCourses(coursesToSave);

        } catch (Exception e) {
            log.error("AI 장소 생성 실패", e);
            return List.of();
        }
    }

    public List<Course> generatePlacesWithAI(double centerLat, double centerLon,
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
                dataTransformService.translatePurpose(purpose),
                count,
                centerLat,
                centerLon,
                dataTransformService.translateTransportation(transportation)
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

            List<Course> coursesToSave = new ArrayList<>();
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
                            .tourPurposeTags(placeNode.has("tourPurposeTags") ?
                                    placeNode.get("tourPurposeTags").asText() : purpose)
                            .isHidden(false)
                            .popularityScore(50)
                            .dataSource("AI_GENERATED")
                            .placeImg(null)
                            .build();
                    coursesToSave.add(place);
                } catch (Exception e) {
                    log.warn("장소 파싱 실패", e);
                }
            }

            return courseService.saveAllCourses(coursesToSave);

        } catch (Exception e) {
            log.error("AI 응답 파싱 실패", e);
            return List.of();
        }
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
