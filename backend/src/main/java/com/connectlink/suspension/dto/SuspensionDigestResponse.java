package com.connectlink.suspension.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 일시중단 취합 전달 한 번(전 영업일 취합 또는 주간 취합).
 *
 * @param sentOn 보낸 영업일
 * @param fromDate 모은 범위 처음. 전 영업일 취합은 작업일(보낸 날 다음 날), 주간 취합은 확정일(그 주 월요일)
 * @param toDate 모은 범위 끝. 전 영업일 취합은 작업일(다음 영업일), 주간 취합은 확정일(보낸 날)
 * @param recipients 받은 매터모스트 채널
 */
public record SuspensionDigestResponse(
    LocalDate sentOn,
    LocalDate fromDate,
    LocalDate toDate,
    int itemCount,
    List<String> recipients,
    String body,
    LocalDateTime sentAt) {}
