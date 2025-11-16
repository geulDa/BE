package com.CUK.geulDa.ai.service;

import com.CUK.geulDa.ai.dto.ChatRequest;
import com.CUK.geulDa.ai.dto.ChatResponse;
import com.CUK.geulDa.ai.mcp.BucheonTourMcpServer;
import com.CUK.geulDa.domain.course.Course;
import com.CUK.geulDa.domain.course.service.CourseService;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ChatClient chatClient;
    private final BucheonTourMcpServer mcpServer;
    private final VectorStore vectorStore;
    private final CourseService courseService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${geulda.vector-store.path:vector-store.json}")
    private String vectorStorePath;

    private static final int BATCH_SIZE = 15;

    private volatile boolean isVectorStoreReady = false;

    @PostConstruct
    public void initializeVectorStore() {
        File vectorFile = new File(vectorStorePath);

        // 파일이 이미 있으면 즉시 준비 완료
        if (vectorFile.exists()) {
            isVectorStoreReady = true;
            log.info("✅ 벡터 스토어 파일 존재, 즉시 준비 완료: {} ({}KB)",
                    vectorStorePath, vectorFile.length() / 1024);
            return;
        }

        // 파일이 없으면 백그라운드에서 생성
        log.info("🔄 벡터 스토어 파일 없음, 백그라운드 생성 시작...");
        initializeVectorStoreAsync();
    }

    /**
     * 벡터스토어 비동기 초기화 (최초 생성)
     */
    @Async("vectorStoreExecutor")
    public void initializeVectorStoreAsync() {
        try {
            log.info("🔄 벡터 스토어 최초 생성 시작... (백그라운드)");
            long startTime = System.currentTimeMillis();

            // 페이징 기반 처리 (전체 로드 안함)
            final int[] totalProcessed = {0};
            final int[] batchNum = {0};

            courseService.processCoursesInBatches(BATCH_SIZE, courses -> {
                List<Document> documents = courses.stream()
                        .filter(course -> StringUtils.hasText(course.getDescription()))
                        .map(course -> new Document(
                                course.getId().toString(),
                                buildDocumentContent(course),
                                buildDocumentMetadata(course)
                        ))
                        .toList();

                if (!documents.isEmpty()) {
                    try {
                        vectorStore.add(documents);
                        totalProcessed[0] += documents.size();
                        log.info("✓ 배치 {} 완료: {}개 문서 (누적: {}개)",
                                ++batchNum[0], documents.size(), totalProcessed[0]);
                    } catch (Exception e) {
                        log.error("❌ 배치 처리 실패", e);
                        throw new RuntimeException("배치 처리 실패", e);
                    }
                }
            });

            if (totalProcessed[0] == 0) {
                log.warn("⚠️ 벡터 스토어에 추가할 장소가 없습니다");
                return;
            }

            // 파일로 저장
            File vectorFile = new File(vectorStorePath);
            if (vectorStore instanceof SimpleVectorStore simpleStore) {
                simpleStore.save(vectorFile);
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                log.info("💾 벡터 데이터 저장 완료: {} ({}초 소요, {}KB)",
                        vectorStorePath, elapsed, vectorFile.length() / 1024);
            }

            isVectorStoreReady = true;
            log.info("✅ 벡터 스토어 초기화 완료! (총 {}개 장소)", totalProcessed[0]);

        } catch (Exception e) {
            log.error("❌ 벡터 스토어 초기화 실패", e);
            isVectorStoreReady = false;
        }
    }

    /**
     * 장소 데이터 변경 시 벡터 스토어 재생성 (관리자 API에서 호출)
     */
    public void refreshVectorStore() {
        log.info("🔄 벡터 스토어 재생성 요청 접수 (백그라운드 실행)");
        isVectorStoreReady = false;  // 재생성 중에는 사용 불가
        refreshVectorStoreAsync();
    }

    /**
     * 벡터스토어 비동기 재생성
     */
    @Async("vectorStoreExecutor")
    public void refreshVectorStoreAsync() {
        try {
            log.info("🔄 벡터 스토어 재생성 시작... (백그라운드)");
            long startTime = System.currentTimeMillis();

            // 1. 기존 파일 삭제
            File vectorFile = new File(vectorStorePath);
            if (vectorFile.exists()) {
                boolean deleted = vectorFile.delete();
                log.info("📁 기존 벡터 파일 삭제: {}", deleted);
            }

            // 2. 페이징 기반 처리
            final int[] totalProcessed = {0};
            final int[] batchNum = {0};

            courseService.processCoursesInBatches(BATCH_SIZE, courses -> {
                List<Document> documents = courses.stream()
                        .filter(course -> StringUtils.hasText(course.getDescription()))
                        .map(course -> new Document(
                                course.getId().toString(),
                                buildDocumentContent(course),
                                buildDocumentMetadata(course)
                        ))
                        .toList();

                if (!documents.isEmpty()) {
                    try {
                        vectorStore.add(documents);
                        totalProcessed[0] += documents.size();
                        log.info("✓ 배치 {} 완료: {}개 문서 (누적: {}개)",
                                ++batchNum[0], documents.size(), totalProcessed[0]);
                    } catch (Exception e) {
                        log.error("❌ 배치 처리 실패", e);
                        throw new RuntimeException("배치 처리 실패", e);
                    }
                }
            });

            if (totalProcessed[0] == 0) {
                log.warn("⚠️ 벡터 스토어에 추가할 장소가 없습니다");
                return;
            }

            // 3. 파일로 저장
            if (vectorStore instanceof SimpleVectorStore simpleStore) {
                simpleStore.save(vectorFile);
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                log.info("💾 벡터 데이터 저장 완료: {} ({}초 소요, {}KB)",
                        vectorStorePath, elapsed, vectorFile.length() / 1024);
            }

            isVectorStoreReady = true;
            log.info("✅ 벡터 스토어 재생성 완료! (총 {}개 장소, {}초 소요)",
                    totalProcessed[0], (System.currentTimeMillis() - startTime) / 1000);

        } catch (Exception e) {
            log.error("❌ 벡터 스토어 재생성 실패", e);
            isVectorStoreReady = false;
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR,
                    "벡터 스토어 재생성 중 오류가 발생했습니다.");
        }
    }

    public boolean isVectorStoreReady() {
        return isVectorStoreReady;
    }

    private String buildDocumentContent(Course course) {
        StringBuilder content = new StringBuilder();
        content.append(course.getName()).append(" ");

        if (StringUtils.hasText(course.getDescription())) {
            content.append(course.getDescription()).append(" ");
        }

        if (StringUtils.hasText(course.getCategory())) {
            content.append(course.getCategory()).append(" ");
        }

        return content.toString().trim();
    }

    private Map<String, Object> buildDocumentMetadata(Course course) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", course.getName());
        metadata.put("category", course.getCategory() != null ? course.getCategory() : "");
        metadata.put("address", course.getAddress() != null ? course.getAddress() : "");

        return metadata;
    }

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();

        String key = "chat:session:" + sessionId;
        redisTemplate.opsForValue().set(
                key,
                Map.of("createdAt", LocalDateTime.now().toString()),
                Duration.ofHours(1)
        );

        log.info("채팅 세션 생성: {}", sessionId);
        return sessionId;
    }

    @Cacheable(value = "chatbot", key = "#sessionId + ':' + #message")
    public ChatResponse chat(String sessionId, String message) {
        // 세션 검증
        validateSession(sessionId);

        try {
            // 자연어 검색이 필요한지 판단
            boolean needsSemanticSearch = isSemanticQuery(message);

            // 컨텍스트 구성 (RAG)
            String context = needsSemanticSearch ?
                    buildSemanticContext(message) :
                    buildKeywordContext(message);
            String prompt = String.format("""
                당신은 부천시 관광 전문 AI 가이드 '글다'입니다.
                20년 경력의 부천 여행 전문가처럼 상세하고 실용적인 정보를 제공하세요.

                [부천 장소 정보]
                %s

                [사용자 질문]
                %s

                [답변 가이드라인]
                1. 인사 & 맥락 파악
                   - 사용자의 질문 의도를 정확히 파악
                   - 간단하고 친근한 인사로 시작 (1문장)

                2. 장소 추천 (최대 3개)
                   각 장소마다 다음 형식으로 작성:
                   📍 **장소명**
                   • 특징: 이 장소만의 독특한 매력 (1문장)
                   • 추천 이유: 사용자 질문과 연결된 구체적 이유 (1문장)
                   • 방문 팁: 최적 시간대, 주변 볼거리, 주의사항 등 (1-2문장)
                   • 위치: 주소 또는 접근성 정보

                3. 추가 정보 (선택)
                   - 장소 간 이동 시간/거리
                   - 계절별 추천 시기
                   - 함께 방문하면 좋은 주변 장소
                   - 맛집/카페 조합 추천

                4. 마무리
                   - 따뜻한 격려나 추가 도움 제안 (1문장)

                [필수 준수 사항]
                - 위 참고 정보에 있는 장소만 추천
                - 구체적인 숫자, 시간, 거리 포함
                - 이모지로 가독성 향상 (📍🎨🍽️☕🌳🎭⏰🚗 등)
                - 계절, 날씨, 시간대 고려한 맞춤 추천
                - 실제 방문자처럼 생생한 정보 제공
                - 참고 정보에 없는 장소 언급 금지
                - 모호한 표현 지양 ("좋아요", "괜찮아요" 등)
                - 과장된 홍보 문구 지양
                - 간결하게 작성

                [응답 예시]
                안녕하세요! 부천에서 데이트하기 좋은 곳을 찾으시는군요 😊

                📍 **석왕사**
                • 특징: 조용한 산속 사찰로 단풍과 벚꽃이 아름다운 힐링 명소입니다
                • 추천 이유: 도심 속 고즈넉한 분위기에서 여유로운 산책 데이트를 즐길 수 있어요
                • 방문 팁: 평일 오전 방문 시 한적하며, 주변 둘레길(약 1.5km)을 함께 걸으면 좋습니다
                • 위치: 부천시 원미구 춘의동 (시청에서 차로 10분)

                📍 **부천 아트벙커**
                • 특징: 지하 벙커를 개조한 복합문화공간, 독특한 건축미가 인상적입니다
                • 추천 이유: 전시, 영화, 공연을 한 곳에서 즐길 수 있어 문화 데이트에 최적입니다
                • 방문 팁: 주말 오후 2-5시 방문 추천, 전시 후 1층 카페에서 여유 시간 가지세요
                • 위치: 부천역 도보 5분 (무료 주차 가능)

                ⏰ 두 곳 모두 2-3시간이면 충분히 둘러볼 수 있어요. 궁금한 점 있으면 언제든 물어보세요! 🌟
                """, context, message);

            org.springframework.ai.chat.model.ChatResponse response = chatClient
                    .prompt(prompt)
                    .call()
                    .chatResponse();

            String botMessage = response.getResult().getOutput().getText();

            // 채팅 히스토리 저장
            saveChatHistory(sessionId, message, botMessage);

            return new ChatResponse(botMessage);

        } catch (Exception e) {
            log.error("챗봇 처리 실패", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR,
                    "챗봇 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private boolean isSemanticQuery(String message) {
        List<String> semanticKeywords = List.of(
                "분위기", "느낌", "같은", "비슷한", "추천", "어떤", "좋은",
                "어디", "뭐", "무엇"
        );
        return semanticKeywords.stream().anyMatch(message::contains);
    }

    private String buildSemanticContext(String query) {
        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>)
                mcpServer.executeTool("semantic_search", Map.of("query", query));

        if (searchResult.containsKey("error")) {
            log.warn("시맨틱 검색 실패, 키워드 검색으로 대체");
            return buildKeywordContext(query);
        }

        @SuppressWarnings("unchecked")
        List<Course> courses = (List<Course>) searchResult.get("places");

        if (courses == null || courses.isEmpty()) {
            return "현재 검색된 장소가 없습니다.";
        }

        return courses.stream()
                .map(course -> String.format("- %s: %s (%s)",
                        course.getName(),
                        course.getDescription() != null ? course.getDescription() : "",
                        course.getAddress() != null ? course.getAddress() : ""))
                .collect(Collectors.joining("\n"));
    }

    private String buildKeywordContext(String query) {
        // 키워드 추출
        String keyword = extractKeyword(query);

        @SuppressWarnings("unchecked")
        Map<String, Object> searchResult = (Map<String, Object>)
                mcpServer.executeTool("search_places", Map.of("keyword", keyword));

        if (searchResult.containsKey("error")) {
            return "현재 검색된 장소가 없습니다.";
        }

        @SuppressWarnings("unchecked")
        List<Course> courses = (List<Course>) searchResult.get("places");

        if (courses == null || courses.isEmpty()) {
            return "현재 검색된 장소가 없습니다.";
        }

        return courses.stream()
                .map(course -> String.format("- %s: %s",
                        course.getName(),
                        course.getDescription() != null ? course.getDescription() : ""))
                .collect(Collectors.joining("\n"));
    }

    private String extractKeyword(String query) {
        String[] stopWords = {"은", "는", "이", "가", "을", "를", "에", "의", "로", "으로",
                "에서", "어디", "뭐", "무엇", "있어", "있나요", "알려줘", "추천"};

        String cleaned = query;
        for (String stopWord : stopWords) {
            cleaned = cleaned.replace(stopWord, " ");
        }

        return cleaned.trim().split("\\s+")[0];
    }


    /**
     * 세션 유효성 검증
     */
    private void validateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                    "세션 ID가 제공되지 않았습니다.");
        }

        String key = "chat:session:" + sessionId;
        Boolean exists = redisTemplate.hasKey(key);

        if (exists == null || !exists) {
            log.warn("유효하지 않은 세션 접근 시도: {}", sessionId);
            throw new BusinessException(ErrorCode.AI_SESSION_NOT_FOUND,
                    "유효하지 않거나 만료된 세션입니다. 새 세션을 생성해주세요.");
        }

        log.debug("세션 검증 성공: {}", sessionId);
    }

    private void saveChatHistory(String sessionId, String userMessage, String botMessage) {
        String key = "chat:history:" + sessionId;

        Map<String, Object> turn = Map.of(
                "user", userMessage,
                "bot", botMessage,
                "timestamp", LocalDateTime.now().toString()
        );

        redisTemplate.opsForList().leftPush(key, turn);
        redisTemplate.opsForList().trim(key, 0, 9); // 최근 10턴만 유지
        redisTemplate.expire(key, Duration.ofHours(1));
    }
}
