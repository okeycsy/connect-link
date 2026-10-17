package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionReplyStatus;
import com.connectlink.opinion.OpinionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 의견 요청 목록 한 줄.
 *
 * @param daysLeft 회신 기한까지 남은 날(지났으면 음수)
 * @param targetCount 대상 기관 수. 기관 담당자에게는 null(다른 기관의 진행 상황을 보이지 않는다)
 * @param repliedCount 회신(제출)한 기관 수. 기관 담당자에게는 null
 * @param myReplyStatus 기관 담당자 자기 기관의 회신 상태. 저장한 적이 없거나 결제원 쪽이면 null
 */
public record OpinionRequestSummaryResponse(
    Long id,
    String title,
    OpinionRequestStatus status,
    String statusLabel,
    boolean cancelled,
    LocalDate dueDate,
    long daysLeft,
    String ownerName,
    @Schema(nullable = true) Integer targetCount,
    @Schema(nullable = true) Integer repliedCount,
    @Schema(nullable = true) OpinionReplyStatus myReplyStatus,
    @Schema(nullable = true) String myReplyStatusLabel,
    @Schema(nullable = true) LocalDateTime openedAt,
    LocalDateTime updatedAt) {}
