package com.connectlink.contact.dto;

import com.connectlink.organization.Business;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 정기 변경 주간 등록·수정 양식. 수정할 때 업무는 바뀌지 않는다(보내도 버린다). */
public record UpdateCampaignSaveRequest(
    @NotNull(message = "업무를 골라 주세요.") Business business,
    @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 100, message = "이름은 100자까지 쓸 수 있습니다.")
        String title,
    @NotNull(message = "시작일을 골라 주세요.") LocalDate startDate,
    @NotNull(message = "종료일을 골라 주세요.") LocalDate endDate) {}
