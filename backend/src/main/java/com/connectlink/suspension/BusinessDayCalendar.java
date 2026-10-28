package com.connectlink.suspension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 영업일 달력. 토·일과 설정의 휴일({@code app.suspension.digest.holidays})이 아닌 날이 영업일이다. */
@Component
class BusinessDayCalendar {

  private final SuspensionDigestProperties properties;

  BusinessDayCalendar(SuspensionDigestProperties properties) {
    this.properties = properties;
  }

  boolean isBusinessDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY
        && day != DayOfWeek.SUNDAY
        && !Set.copyOf(properties.getHolidays()).contains(date);
  }

  /** 이 날이 든 주(월~일)의 첫날, 월요일. */
  static LocalDate weekStart(LocalDate date) {
    return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }

  /**
   * 주간 취합을 보내는 날인지. 그 주(월~일)의 마지막 영업일(보통 금요일)과 그보다 두 영업일 앞(보통 수요일)이다. 휴일로 영업일이 모자라 두 영업일 앞이 지난 주로
   * 넘어가면 마지막 영업일에만 보낸다.
   */
  boolean isWeeklyDigestDay(LocalDate date) {
    if (!isBusinessDay(date)) {
      return false;
    }
    LocalDate start = weekStart(date);
    List<LocalDate> days = start.datesUntil(start.plusDays(7)).filter(this::isBusinessDay).toList();
    int last = days.size() - 1;
    return date.equals(days.get(last)) || (last >= 2 && date.equals(days.get(last - 2)));
  }

  /** 이 날 다음의 첫 영업일. */
  LocalDate nextBusinessDay(LocalDate date) {
    LocalDate next = date.plusDays(1);
    while (!isBusinessDay(next)) {
      next = next.plusDays(1);
    }
    return next;
  }
}
