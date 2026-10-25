package com.connectlink.suspension.dto;

import com.connectlink.suspension.SuspensionPeriod;
import java.time.LocalDateTime;

/**
 * 다른 기관 일시중단 일정 한 건. 기관 담당자가 같은 업무의 다른 기관 일정을 볼 때 쓰며, 일정(기관·일시·대상 업무)만 담는다. 중단 이유·담당자·공문 같은 내용은 그
 * 기관과 결제원만 본다.
 */
public record SuspensionScheduleResponse(
    String organizationCode,
    String organizationName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String targetWorkNames,
    SuspensionPeriod period) {}
