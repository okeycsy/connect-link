package com.connectlink.common;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 모든 API 오류 응답의 공통 형식. RFC 7807(type, title, status, detail, instance)에 업무 오류 코드 {@code code}와 필드별
 * 검증 메시지 {@code fieldErrors}를 더했다.
 */
public record ErrorResponse(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    String code,
    List<FieldError> fieldErrors,
    OffsetDateTime timestamp) {

  /** 요청 본문 필드 하나의 검증 실패. */
  public record FieldError(String field, String message) {}
}
