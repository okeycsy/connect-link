package com.connectlink.suspension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 확인을 시작한 결제원 담당자와 그 사람이 쓴 조치 및 인수인계 사항.
 *
 * @param handover 이 담당자의 조치 및 인수인계 사항. 아직 쓰지 않았으면 null
 * @param handoverAt 사항을 마지막으로 쓴 일시
 * @param mine 로그인한 사람 본인인지. 본인 사항을 고칠 때 미리 채운다
 */
public record SuspensionReviewerResponse(
    String name,
    String roleLabel,
    LocalDateTime startedAt,
    @Schema(nullable = true) String handover,
    @Schema(nullable = true) LocalDateTime handoverAt,
    boolean mine) {}
