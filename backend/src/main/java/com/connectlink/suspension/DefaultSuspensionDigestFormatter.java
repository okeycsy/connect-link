package com.connectlink.suspension;

import com.connectlink.organization.Business;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 임시 기본 양식. 업무별로 묶어 한 건에 한 줄(일시, 기관, 중단 이유)과 결제원 조치·인수인계 사항 한 줄을 적는다. 2000자를 넘으면 뒤를 줄이고 남은 건수를 적는다.
 * 전달 양식이 정해지면 바꾼다({@link SuspensionDigestFormatter}).
 */
@Component
class DefaultSuspensionDigestFormatter implements SuspensionDigestFormatter {

  static final int MAX_LENGTH = 2000;

  @Override
  public String subject(LocalDate fromDate, LocalDate toDate, int count) {
    return "[연결이] 일시중단 확정 " + count + "건 (" + range(fromDate, toDate) + ")";
  }

  @Override
  public String body(LocalDate fromDate, LocalDate toDate, List<Suspension> suspensions) {
    String head = "작업일 " + range(fromDate, toDate) + " 확정 일시중단 " + suspensions.size() + "건입니다.";
    if (suspensions.isEmpty()) {
      return head;
    }
    Map<Business, List<Suspension>> byBusiness = new LinkedHashMap<>();
    suspensions.forEach(
        s -> byBusiness.computeIfAbsent(s.getBusiness(), k -> new ArrayList<>()).add(s));
    StringBuilder body = new StringBuilder(head);
    int written = 0;
    for (Map.Entry<Business, List<Suspension>> entry : byBusiness.entrySet()) {
      String section = "\n\n#### " + entry.getKey().getLabel();
      if (body.length() + section.length() > MAX_LENGTH - 40) {
        break;
      }
      body.append(section);
      for (Suspension s : entry.getValue()) {
        String line = line(s);
        if (body.length() + line.length() > MAX_LENGTH - 40) {
          break;
        }
        body.append(line);
        written++;
      }
    }
    int rest = suspensions.size() - written;
    if (rest > 0) {
      body.append("\n\n외 ").append(rest).append("건은 연결이에서 확인해 주세요.");
    }
    return body.toString();
  }

  private static String line(Suspension s) {
    StringBuilder line =
        new StringBuilder("\n- ")
            .append(SuspensionNotifier.periodText(s.getStartAt(), s.getEndAt()))
            .append(" ")
            .append(s.getOrganization().getName());
    line.append(" · ").append(s.getReason());
    if (s.getHandover() != null) {
      line.append("\n  - 조치·인수인계: ").append(s.getHandover());
    }
    return line.toString();
  }

  private static String range(LocalDate fromDate, LocalDate toDate) {
    return fromDate.equals(toDate) ? fromDate.toString() : fromDate + "~" + toDate;
  }
}
