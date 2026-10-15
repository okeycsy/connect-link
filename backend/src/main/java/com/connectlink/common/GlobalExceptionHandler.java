package com.connectlink.common;

import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** API 오류를 {@link ErrorResponse} 하나의 형식으로 바꾼다. 오류 형식은 여기서만 만든다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    List<ErrorResponse.FieldError> fieldErrors =
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
            .toList();
    return build(
        HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요.", "VALIDATION_FAILED", fieldErrors, request);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusiness(
      BusinessException e, HttpServletRequest request) {
    return build(e.getStatus(), e.getMessage(), e.getCode(), List.of(), request);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(
      ResponseStatusException e, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(e.getRawStatusCode());
    return build(status, e.getReason(), status.name(), List.of(), request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException e, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "권한이 없습니다.", "ACCESS_DENIED", List.of(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
    log.error("처리되지 않은 오류: {} {}", request.getMethod(), request.getRequestURI(), e);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다.", "INTERNAL_ERROR", List.of(), request);
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String detail,
      String code,
      List<ErrorResponse.FieldError> fieldErrors,
      HttpServletRequest request) {
    ErrorResponse body =
        new ErrorResponse(
            "about:blank",
            status.getReasonPhrase(),
            status.value(),
            detail,
            request.getRequestURI(),
            code,
            fieldErrors,
            OffsetDateTime.now());
    return ResponseEntity.status(status).body(body);
  }
}
