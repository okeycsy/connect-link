package com.connectlink.opinion.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 사유 하나를 받는 요청: 요청 취소, 회신 수정 요청, 수정 요청 반려. */
public record OpinionReasonRequest(
    @NotBlank(message = "사유를 입력해 주세요.") @Size(max = 500, message = "사유는 500자까지입니다.")
        String reason) {}
