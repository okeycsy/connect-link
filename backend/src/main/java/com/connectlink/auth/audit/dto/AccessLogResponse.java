package com.connectlink.auth.audit.dto;

import com.connectlink.auth.audit.AccessLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 접속기록 한 건. */
public record AccessLogResponse(
    Long id,
    LocalDateTime accessedAt,
    String accountEmail,
    @Schema(nullable = true) String accountName,
    @Schema(nullable = true) String organizationName,
    String ipAddress,
    String action,
    @Schema(nullable = true, description = "처리한 정보주체를 가리키는 값(대상 번호 또는 조회 조건)") String target,
    String httpMethod,
    String requestPath,
    int statusCode) {

  public static AccessLogResponse from(AccessLog log) {
    return new AccessLogResponse(
        log.getId(),
        log.getAccessedAt(),
        log.getAccountEmail(),
        log.getAccountName(),
        log.getOrganizationName(),
        log.getIpAddress(),
        log.getAction(),
        log.getTarget(),
        log.getHttpMethod(),
        log.getRequestPath(),
        log.getStatusCode());
  }
}
