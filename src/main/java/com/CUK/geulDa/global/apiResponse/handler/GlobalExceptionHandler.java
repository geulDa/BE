package com.CUK.geulDa.global.apiResponse.handler;

import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RFC 7807 ProblemDetail 기반 전역 예외 처리 핸들러
 *
 * Spring Boot 3.0부터 지원되는 ProblemDetail을 사용하여 표준화된 에러 응답을 제공합니다.
 * ProblemDetail은 HTTP API 에러를 구조화된 JSON 형식으로 반환하는 RFC 7807 표준을 구현합니다.
 *
 * [ProblemDetail 구조]
 * {
 *   "type": "about:blank",              // 문제 유형을 설명하는 URI
 *   "title": "Not Found",               // 사람이 읽을 수 있는 짧은 제목
 *   "status": 404,                      // HTTP 상태 코드
 *   "detail": "사용자를 찾을 수 없습니다.",  // 구체적인 오류 설명
 *   "instance": "/api/users/123",       // 문제가 발생한 요청 경로
 *   "errorCode": "E001",                // 커스텀 에러 코드 (추가 속성)
 *   "timestamp": "2025-10-18T..."      // 발생 시각 (추가 속성)
 * }
 *
 * setProperty() 메서드를 통해 기본 필드 외에 커스텀 필드를 추가할 수 있습니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String ERROR_CODE_KEY = "errorCode";
    private static final String PATH_KEY = "path";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException e,
            HttpServletRequest request) {

        ErrorCode errorCode = e.getErrorCode();
        log.error("Business Exception: {} - {}", errorCode.getCode(), e.getResponseMessage(), e);

        /*
         * ProblemDetail.forStatusAndDetail()로 기본 구조를 생성합니다.
         * 이 메서드는 자동으로 status와 detail 필드를 설정합니다.
         */
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                errorCode.getHttpStatus(),
                e.getResponseMessage()
        );

        /*
         * RFC 7807 표준 필드를 설정합니다.
         * - type: 에러 유형을 나타내는 URI (about:blank는 일반적인 HTTP 에러를 의미)
         * - title: HTTP 상태 코드의 표준 문구 (예: "Not Found", "Bad Request")
         * - instance: 에러가 발생한 요청 경로
         */
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(errorCode.getHttpStatus().getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        /*
         * setProperty()로 커스텀 필드를 추가합니다.
         * 이 필드들은 클라이언트가 에러를 더 상세하게 처리할 수 있도록 돕습니다.
         */
        problemDetail.setProperty(ERROR_CODE_KEY, errorCode.getCode());
        problemDetail.setProperty(TIMESTAMP_KEY, LocalDateTime.now());
        problemDetail.setProperty(PATH_KEY, request.getRequestURI());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Validation failed: {}", ex.getMessage());

        /*
         * @Valid 검증 실패 시 각 필드별 오류 정보를 수집합니다.
         * 클라이언트는 이 정보를 통해 어떤 필드가 왜 실패했는지 알 수 있습니다.
         */
        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("field", error.getField());
                    errorMap.put("message", error.getDefaultMessage());
                    errorMap.put("rejectedValue",
                            error.getRejectedValue() != null ? error.getRejectedValue().toString() : "null");
                    return errorMap;
                })
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "입력값 검증에 실패했습니다."
        );

        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty(ERROR_CODE_KEY, ErrorCode.VALIDATION_FAILED.getCode());
        problemDetail.setProperty(TIMESTAMP_KEY, LocalDateTime.now());
        problemDetail.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Method not supported: {}", ex.getMessage());

        String supportedMethods = ex.getSupportedHttpMethods() != null
                ? ex.getSupportedHttpMethods().toString()
                : "NONE";

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "지원하지 않는 HTTP 메소드입니다. 지원되는 메소드: " + supportedMethods
        );

        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty(ERROR_CODE_KEY, "E405");
        problemDetail.setProperty(TIMESTAMP_KEY, LocalDateTime.now());
        problemDetail.setProperty("supportedMethods", supportedMethods);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Media type not supported: {}", ex.getMessage());

        String supportedMediaTypes = ex.getSupportedMediaTypes().toString();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "지원하지 않는 미디어 타입입니다. 지원되는 타입: " + supportedMediaTypes
        );

        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty(ERROR_CODE_KEY, "E415");
        problemDetail.setProperty(TIMESTAMP_KEY, LocalDateTime.now());
        problemDetail.setProperty("supportedMediaTypes", supportedMediaTypes);

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Missing request parameter: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "필수 요청 파라미터 '" + ex.getParameterName() + "'이(가) 누락되었습니다."
        );

        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty(ERROR_CODE_KEY, ErrorCode.MISSING_PARAMETER.getCode());
        problemDetail.setProperty(TIMESTAMP_KEY, LocalDateTime.now());
        problemDetail.setProperty("parameterName", ex.getParameterName());
        problemDetail.setProperty("parameterType", ex.getParameterType());

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(
            BindException ex,
            HttpServletRequest request) {

        log.warn("Binding failed: {}", ex.getMessage());

        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("field", error.getField());
                    errorMap.put("message", error.getDefaultMessage());
                    return errorMap;
                })
                .collect(Collectors.toList());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "요청 파라미터 바인딩에 실패했습니다."
        );

        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty(ERROR_CODE_KEY, ErrorCode.INVALID_INPUT.getCode());
        problemDetail.setProperty(TIMESTAMP_KEY, LocalDateTime.now());
        problemDetail.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(
            Exception e,
            HttpServletRequest request) {

        /*
         * 예상치 못한 예외는 보안을 위해 상세 정보를 로그에만 기록하고
         * 클라이언트에는 일반화된 메시지만 전달합니다.
         */
        log.error("Unexpected exception at {}: {}", request.getRequestURI(), e.getMessage(), e);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        );

        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty(ERROR_CODE_KEY, ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        problemDetail.setProperty(TIMESTAMP_KEY, LocalDateTime.now());
        problemDetail.setProperty(PATH_KEY, request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
