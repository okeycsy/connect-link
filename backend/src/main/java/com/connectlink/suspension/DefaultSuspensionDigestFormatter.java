package com.connectlink.suspension;

import com.connectlink.organization.Business;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 임시 기본 양식. 업무별로 묶어 한 건에 한 줄(일시, 기관, 중단 이유)과 결제원 담당자마다 쓴 조치·인수인계 사항을 한 줄씩 적는다. 2000자를 넘으면 뒤를 줄이고 남은
 * 건수를 적는다. 전달 양식이 정해지면 바꾼다({@link SuspensionDigestFormatter}).
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
    // 관련 레일 업무별로 묶는다. 여러 업무에 걸친 건은 그 업무마다 적고 건수는 한 번씩 센다. 관련 업무가 없는 건은 맨 뒤 "그 밖의 업무"에.
    Map<Business, List<Suspension>> byBusiness = new EnumMap<>(Business.class);
    List<Suspension> unlinked = new ArrayList<>();
    suspensions.forEach(
        s -> {
          if (s.getBusinesses().isEmpty()) {
            unlinked.add(s);
          }
          s.getBusinesses()
              .forEach(b -> byBusiness.computeIfAbsent(b, k -> new ArrayList<>()).add(s));
        });
    Map<String, List<Suspension>> sections = new LinkedHashMap<>();
    byBusiness.forEach((b, list) -> sections.put(b.getLabel(), list));
    if (!unlinked.isEmpty()) {
      sections.put("그 밖의 업무", unlinked);
    }
    StringBuilder body = new StringBuilder(head);
    Set<Long> written = new HashSet<>();
    for (Map.Entry<String, List<Suspension>> entry : sections.entrySet()) {
      String section = "\n\n#### " + entry.getKey();
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
        written.add(s.getId());
      }
    }
    int rest = suspensions.size() - written.size();
    if (rest > 0) {
      body.append("\n\n외 ").append(rest).append("건은 연결이에서 확인해 주세요.");
    }
    return body.toString();
  }

  @Override
  public String confirmedSubject(Suspension suspension) {
    return "[연결이] 일시중단 확정: "
        + suspension.getOrganization().getName()
        + " ("
        + SuspensionNotifier.periodText(suspension.getStartAt(), suspension.getEndAt())
        + ")";
  }

  @Override
  public String confirmedBody(Suspension suspension) {
    String businesses =
        suspension.getBusinesses().isEmpty()
            ? "그 밖의 업무"
            : suspension.getBusinesses().stream()
                .sorted()
                .map(Business::getLabel)
                .collect(Collectors.joining(", "));
    StringBuilder body =
        new StringBuilder("- 기관: ")
            .append(suspension.getOrganization().getName())
            .append("\n- 일시: ")
            .append(SuspensionNotifier.periodText(suspension.getStartAt(), suspension.getEndAt()))
            .append("\n- 대상 업무: ")
            .append(suspension.targetWorkNames())
            .append("\n- 관련 업무: ")
            .append(businesses)
            .append("\n- 중단 이유: ")
            .append(suspension.getReason())
            .append("\n\n#### 조치 및 인수인계 사항");
    suspension.getReviewers().stream()
        .filter(r -> r.getHandover() != null)
        .forEach(r -> body.append("\n- ").append(r.getName()).append(": ").append(r.getHandover()));
    String text = body.toString();
    return text.length() <= MAX_LENGTH ? text : text.substring(0, MAX_LENGTH - 1) + "…";
  }

  @Override
  public String weeklySubject(LocalDate fromDate, LocalDate toDate, int count) {
    return "[연결이] 이번 주 확정 일시중단 " + count + "건 (" + range(fromDate, toDate) + ")";
  }

  @Override
  public String weeklyBody(LocalDate fromDate, LocalDate toDate, List<Suspension> suspensions) {
    String head = range(fromDate, toDate) + " 에 확정된 일시중단 " + suspensions.size() + "건입니다.";
    if (suspensions.isEmpty()) {
      return head;
    }
    StringBuilder body = new StringBuilder(head).append("\n");
    int written = 0;
    for (Suspension s : suspensions) {
      String line = weeklyLine(s);
      if (body.length() + line.length() > MAX_LENGTH - 40) {
        break;
      }
      body.append(line);
      written++;
    }
    int rest = suspensions.size() - written;
    if (rest > 0) {
      body.append("\n\n외 ").append(rest).append("건은 연결이에서 확인해 주세요.");
    }
    return body.toString();
  }

  /** 기관 이름 · 중단 기간, 그 아래 조치사항. 조치사항은 확인자별 조치 및 인수인계 사항이고, 없으면(결제원 작업 안내) 결제원 조치사항 칸이다. */
  private static String weeklyLine(Suspension s) {
    StringBuilder line =
        new StringBuilder("\n- **")
            .append(s.getOrganization().getName())
            .append("** · ")
            .append(SuspensionNotifier.periodText(s.getStartAt(), s.getEndAt()));
    List<String> actions =
        s.getReviewers().stream()
            .filter(r -> r.getHandover() != null)
            .map(r -> r.getName() + ": " + r.getHandover())
            .toList();
    if (actions.isEmpty() && s.getKftcAction() != null && !s.getKftcAction().isBlank()) {
      actions = List.of(s.getKftcAction());
    }
    for (String action : actions) {
      line.append("\n  - 조치: ").append(action);
    }
    return line.toString();
  }

  private static String line(Suspension s) {
    StringBuilder line =
        new StringBuilder("\n- ")
            .append(SuspensionNotifier.periodText(s.getStartAt(), s.getEndAt()))
            .append(" ")
            .append(s.getOrganization().getName())
            .append(" [")
            .append(s.targetWorkNames())
            .append("]");
    line.append(" · ").append(s.getReason());
    s.getReviewers().stream()
        .filter(r -> r.getHandover() != null)
        .forEach(
            r ->
                line.append("\n  - 조치·인수인계(")
                    .append(r.getName())
                    .append("): ")
                    .append(r.getHandover()));
    return line.toString();
  }

  private static String range(LocalDate fromDate, LocalDate toDate) {
    return fromDate.equals(toDate) ? fromDate.toString() : fromDate + "~" + toDate;
  }
}
