package com.connectlink.opinion.dto;

import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 의견 요청 고치기. 작성중과 회신중에 고친다. 업무는 바꾸지 않는다. 양식·대상은 보낸 목록으로 통째로 바뀐다. */
public record OpinionRequestUpdateRequest(
    @NotBlank(message = "요청명을 입력해 주세요.") @Size(max = 200, message = "요청명은 200자까지입니다.") String title,
    @NotBlank(message = "요청 목적·내용을 입력해 주세요.") @Size(max = 4000, message = "요청 내용은 4000자까지입니다.")
        String content,
    @NotNull(message = "회신 기한을 골라 주세요.") LocalDate dueDate,
    @Valid List<OpinionReplyFormRequest> forms,
    @Valid List<OpinionTargetRequest> targets) {

  public OpinionRequestUpdateRequest {
    forms = forms == null ? List.of() : List.copyOf(forms);
    targets = targets == null ? List.of() : List.copyOf(targets);
  }
}
