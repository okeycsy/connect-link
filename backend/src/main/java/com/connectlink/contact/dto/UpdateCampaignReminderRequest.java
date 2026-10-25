package com.connectlink.contact.dto;

import com.connectlink.notification.dto.MailPreviewResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 리마인드 메일 보내기.
 *
 * @param organizationIds 보낼 기관. 비우면 아직 "현행화 완료" 를 누르지 않은 대상 기관 전부다. 이미 완료했거나 대상이 아닌 기관은 건너뛴다
 * @param subject 고친 메일 제목. 비우면(null) 기본 문구
 * @param body 고친 메일 본문. 비우면(null) 기본 문구
 */
public record UpdateCampaignReminderRequest(
    @Schema(nullable = true) List<Long> organizationIds,
    @Schema(nullable = true)
        @Size(max = MailPreviewResponse.SUBJECT_MAX, message = "제목은 200자까지 쓸 수 있습니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목을 입력해 주세요.")
        String subject,
    @Schema(nullable = true)
        @Size(max = MailPreviewResponse.BODY_MAX, message = "본문은 1000자까지 쓸 수 있습니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "본문을 입력해 주세요.")
        String body) {}
