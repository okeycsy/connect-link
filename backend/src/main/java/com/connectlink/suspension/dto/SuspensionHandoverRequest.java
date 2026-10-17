package com.connectlink.suspension.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 결제원 담당자의 조치 및 인수인계 사항. 쓰면 일시중단이 확정된다. */
public record SuspensionHandoverRequest(
    @NotBlank(message = "조치 및 인수인계 사항을 입력해 주세요.")
        @Size(max = 2000, message = "조치 및 인수인계 사항은 2000자까지입니다.")
        String handover) {}
