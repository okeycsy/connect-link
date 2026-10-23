package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 결제원 의견에 이어 단 답글 하나.
 *
 * @param authorKftc 결제원 담당자가 썼는지. 화면은 "결제원(김민찬 과장)", 아니면 대상 기관 이름으로 "KB국민은행(김민준 대리)"처럼 보인다
 * @param authorJobTitle 쓴 사람의 그때 직급
 * @param deletable 지금 사용자가 지울 수 있는지(쓴 사람)
 */
public record TestItemCommentResponse(
    Long id,
    String body,
    boolean authorKftc,
    String authorName,
    @Schema(nullable = true) String authorJobTitle,
    LocalDateTime writtenAt,
    boolean deletable) {}
