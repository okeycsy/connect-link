package com.connectlink.attachment.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 파일 반출 반려. 사유는 올린 사람에게 알림으로 간다. */
public record AttachmentRejectionRequest(
    @NotBlank(message = "반려 사유를 적어 주세요.") @Size(max = 500, message = "반려 사유는 500자까지 적을 수 있습니다.")
        String reason) {}
