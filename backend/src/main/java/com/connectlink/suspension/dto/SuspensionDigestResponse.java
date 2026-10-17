package com.connectlink.suspension.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 전 영업일 취합 전달 한 번.
 *
 * @param sentOn 보낸 영업일
 * @param fromDate 모은 작업일 처음(보낸 날 다음 날)
 * @param toDate 모은 작업일 끝(다음 영업일)
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
