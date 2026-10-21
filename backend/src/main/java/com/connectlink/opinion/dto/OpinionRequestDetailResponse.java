package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionRequestStatus;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 의견 요청 상세. 기관 담당자에게는 자기 기관의 대상 행만 담는다.
 *
 * @param sections 질문을 묶는 섹션(정한 순서). 하나 이상이다
 * @param questions 회신 때 답할 질문(정한 순서). 기관은 고른 답에 따라 지나는 섹션의 질문만 답한다
 * @param daysLeft 회신 기한까지 남은 날(오늘이 기한이면 0, 지났으면 음수)
 * @param cancelReason 회신중에 취소했으면 그 사유. 아니면 null
 */
public record OpinionRequestDetailResponse(
    Long id,
    Business business,
    String businessSlug,
    String businessLabel,
    String title,
    String content,
    LocalDate dueDate,
    long daysLeft,
    OpinionRequestStatus status,
    String statusLabel,
    boolean cancelled,
    @Schema(nullable = true) String cancelReason,
    String ownerName,
    @Schema(nullable = true) LocalDateTime openedAt,
    @Schema(nullable = true) LocalDateTime closedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OpinionSectionResponse> sections,
    List<OpinionQuestionResponse> questions,
    List<OpinionTargetResponse> targets,
    OpinionRequestPermissions permissions) {}
