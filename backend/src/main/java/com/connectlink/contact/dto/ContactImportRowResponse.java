package com.connectlink.contact.dto;

import com.connectlink.contact.ContactImportRowStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 담당자 일괄 등록 검사 결과 한 줄.
 *
 * @param rowNumber 엑셀의 줄 번호(머리글이 1)
 * @param message 건너뜀·오류의 까닭
 */
public record ContactImportRowResponse(
    int rowNumber,
    @Schema(nullable = true) String organizationCode,
    @Schema(nullable = true) String organizationName,
    @Schema(nullable = true) String name,
    @Schema(nullable = true) String email,
    ContactImportRowStatus status,
    String statusLabel,
    @Schema(nullable = true) String message) {}
