package com.connectlink.suspension;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

  /** 이 날 다음의 첫 영업일. */
  LocalDate nextBusinessDay(LocalDate date) {
    LocalDate next = date.plusDays(1);
    while (!isBusinessDay(next)) {
      next = next.plusDays(1);
    }
    return next;
  }
}
