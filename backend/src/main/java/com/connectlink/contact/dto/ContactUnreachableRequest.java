package com.connectlink.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Size;

/**
 * 연락 불가 표시 요청.
 *
 * @param note 메모(예: "02:10 내선·휴대폰 모두 받지 않음"). 없어도 된다
 * @param notifyOrganization 그 기관의 같은 업무 담당자에게도 메일로 알리는지. 결제원 그 업무 담당자에게는 늘 알린다
 */
public record ContactUnreachableRequest(
    @Schema(nullable = true) @Size(max = 500, message = "메모는 500자까지 씁니다.") String note,
    boolean notifyOrganization) {}
