package com.connectlink.notice.dto;

import com.connectlink.notice.NoticeCategory;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 공지·자료실 글 등록·수정. 업무는 등록할 때만 정하고(비우면 전 업무 공통 공지) 수정에서는 무시한다.
 *
 * @param effectiveDate 규약·시행세칙의 시행일. 없으면 null
 * @param opinionRequestId 이어 둘 같은 업무의 의견 요청(예: 회의 참석 회신). 공통 공지에는 둘 수 없다. 없으면 null
 */
public record NoticeSaveRequest(
    @Schema(nullable = true) Business business,
    @NotNull(message = "분류를 골라 주세요.") NoticeCategory category,
    @NotBlank(message = "제목을 입력해 주세요.") @Size(max = 200, message = "제목은 200자까지입니다.") String title,
    @NotBlank(message = "내용을 입력해 주세요.") @Size(max = 4000, message = "내용은 4000자까지입니다.")
        String content,
    boolean pinned,
    @Schema(nullable = true) LocalDate effectiveDate,
    @Schema(nullable = true) Long opinionRequestId) {}
