package com.connectlink.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 지금 남아 있는 연락 불가 표시. 표시한 사람은 결제원 담당자라 화면은 "결제원(이름 직급)" 으로 보인다.
 *
 * @param markedAt 표시한 때
 * @param markedByJobTitle 표시한 그때의 직급. 없으면 null
 */
public record ContactUnreachableResponse(
    LocalDateTime markedAt,
    @Schema(nullable = true) String note,
    String markedByName,
    @Schema(nullable = true) String markedByJobTitle) {}
