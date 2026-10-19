package com.connectlink.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 한 업무의 정기 변경 주기와 리마인드 설정.
 *
 * @param autoCreate 주기가 돌아오면 주간을 스스로 등록하는지
 * @param cycleMonths 정기 변경 주기(개월)
 * @param durationDays 스스로 등록하는 주간 길이(일)
 * @param reminderIntervalDays 미회신 기관에 리마인드를 다시 보내는 간격(일)
 * @param nextCycleDate 다음 정기 변경 요청일(최근 주간 시작일 + 주기). 등록한 주간이 없으면 null 이고, 첫 주간은 결제원 담당자가 등록한다
 */
public record UpdateCampaignScheduleResponse(
    boolean autoCreate,
    int cycleMonths,
    int durationDays,
    int reminderIntervalDays,
    @Schema(nullable = true) LocalDate nextCycleDate) {}
