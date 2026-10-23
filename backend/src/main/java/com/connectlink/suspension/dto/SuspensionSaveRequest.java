package com.connectlink.suspension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 일시중단 등록·수정 양식. 등록할 때 결제원 담당자는 기관을 고르고, 기관 담당자는 자기 기관으로 정해진다({@code organizationId} 를 무시한다). 수정할 때
 * 기관은 바뀌지 않는다.
 *
 * @param kftcAction 결제원 조치사항(중단 동안 결제원이 할 일)
 * @param targetWorkIds 고른 대상 업무(기준정보 ID). 하나 이상이다. 등록하는 사람의 업무와 상관없이 고른다
 * @param contacts 기관 작업 담당자 연락처. 한 명 이상, 적은 순서대로 보인다
 */
public record SuspensionSaveRequest(
    @NotEmpty(message = "대상 업무를 하나 이상 골라 주세요.") @Size(max = 100, message = "대상 업무는 100개까지 고릅니다.")
        List<@NotNull Long> targetWorkIds,
    @Schema(nullable = true) Long organizationId,
    @NotNull(message = "중단 시작 일시를 골라 주세요.") LocalDateTime startAt,
    @NotNull(message = "중단 종료 일시를 골라 주세요.") LocalDateTime endAt,
    @NotBlank(message = "중단 이유(작업 내용)를 입력해 주세요.") @Size(max = 2000, message = "중단 이유는 2000자까지입니다.")
        String reason,
    @Schema(nullable = true) @Size(max = 2000, message = "결제원 조치사항은 2000자까지입니다.") String kftcAction,
    @NotEmpty(message = "담당자 연락처를 한 명 이상 입력해 주세요.") @Size(max = 10, message = "담당자 연락처는 10명까지입니다.")
        List<@Valid SuspensionContactRequest> contacts,
    @Schema(nullable = true) @Size(max = 2000, message = "추가 사항은 2000자까지입니다.") String extraNote) {}
