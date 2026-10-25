package com.connectlink.opinion.dto;

import com.connectlink.notification.dto.MailPreviewResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 리마인드 메일 보내기.
 *
 * @param organizationIds 보낼 미회신 기관. 비우면 미회신 기관 전부에 보낸다. 이미 회신한 기관은 빠진다
 * @param subject 고친 메일 제목. 비우면(null) 기본 문구
 * @param body 고친 메일 본문. 비우면(null) 기본 문구
 */
public record OpinionReminderSendRequest(
    @Schema(nullable = true) List<Long> organizationIds,
    @Schema(nullable = true)
        @Size(max = MailPreviewResponse.SUBJECT_MAX, message = "제목은 200자까지 쓸 수 있습니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목을 입력해 주세요.")
        String subject,
    @Schema(nullable = true)
        @Size(max = MailPreviewResponse.BODY_MAX, message = "본문은 1000자까지 쓸 수 있습니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "본문을 입력해 주세요.")
        String body) {}
