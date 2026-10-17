package com.connectlink.opinion.dto;

import com.connectlink.organization.Business;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 의견 요청 만들기. 작성중으로 저장되고, 첨부자료와 양식 파일은 저장한 뒤 올린다. */
public record OpinionRequestCreateRequest(
    @NotNull(message = "업무를 골라 주세요.") Business business,
    @NotBlank(message = "요청명을 입력해 주세요.") @Size(max = 200, message = "요청명은 200자까지입니다.") String title,
    @NotBlank(message = "요청 목적·내용을 입력해 주세요.") @Size(max = 4000, message = "요청 내용은 4000자까지입니다.")
        String content,
    @NotNull(message = "회신 기한을 골라 주세요.") LocalDate dueDate,
    @Valid List<OpinionReplyFormRequest> forms,
    @Valid List<OpinionTargetRequest> targets) {

  public OpinionRequestCreateRequest {
    forms = forms == null ? List.of() : List.copyOf(forms);
    targets = targets == null ? List.of() : List.copyOf(targets);
  }
}
