package com.connectlink.testplan.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 결제원 의견에 이어 다는 답글.
 *
 * @param body 답글 내용
 */
public record TestItemCommentRequest(
    @NotBlank(message = "답글 내용을 써 주세요.") @Size(max = 2000, message = "답글은 2000자까지입니다.")
        String body) {}
