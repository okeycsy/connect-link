package com.connectlink.notice.dto;

import com.connectlink.notice.NoticeCategory;
import com.connectlink.opinion.dto.OpinionRequestLinkResponse;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 공지·자료실 글. 목록에서는 {@code content} 와 {@code canEdit} 도 함께 내려 상세를 따로 부르지 않는다.
 *
 * @param business 없으면 전 업무 공통 공지
 * @param opinionRequest 이어 둔 의견 요청. 없거나 이 사용자가 볼 수 없으면(대상 기관이 아님 등) null
 * @param canEdit 고치고 지울 수 있는지
 */
public record NoticeResponse(
    Long id,
    @Schema(nullable = true) Business business,
    @Schema(nullable = true) String businessLabel,
    NoticeCategory category,
    String categoryLabel,
    String title,
    String content,
    boolean pinned,
    @Schema(nullable = true) LocalDate effectiveDate,
    String authorName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    @Schema(nullable = true) OpinionRequestLinkResponse opinionRequest,
    boolean canEdit) {}
