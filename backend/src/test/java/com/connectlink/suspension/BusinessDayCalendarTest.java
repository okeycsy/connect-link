package com.connectlink.suspension;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 주간 취합을 보내는 날: 그 주 마지막 영업일과 그 두 영업일 앞. */
class BusinessDayCalendarTest {

  private static BusinessDayCalendar calendar(LocalDate... holidays) {
    SuspensionDigestProperties properties = new SuspensionDigestProperties();
    properties.setHolidays(List.of(holidays));
    return new BusinessDayCalendar(properties);
  }

  @Test
  void 휴일_없는_주는_수요일과_금요일이다() {
    BusinessDayCalendar calendar = calendar();
    // 2026-10-26 이 월요일이다.
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 26))).isFalse();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 27))).isFalse();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 28))).isTrue();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 29))).isFalse();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 30))).isTrue();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 31))).isFalse();
  }

  @Test
  void 금요일이_휴일이면_목요일과_그_두_영업일_앞인_화요일이다() {
    BusinessDayCalendar calendar = calendar(LocalDate.of(2026, 10, 30));
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 27))).isTrue();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 28))).isFalse();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 29))).isTrue();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 30))).isFalse();
  }

  @Test
  void 영업일이_둘뿐인_주는_마지막_영업일에만_보낸다() {
    BusinessDayCalendar calendar =
        calendar(
            LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 28));
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 29))).isFalse();
    assertThat(calendar.isWeeklyDigestDay(LocalDate.of(2026, 10, 30))).isTrue();
  }
}
