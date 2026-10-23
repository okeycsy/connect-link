package com.connectlink.testplan;

import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 기관 테스트 모듈의 알림. 모두 {@link NotificationService} 를 거쳐 알림 센터에 남는다. 테스트가 열리면 대상 기관 담당자에게 메일, 기관이 결과를
 * 등록하거나 테스트데이터를 생성 완료로 바꾸면 그 테스트의 알림 대상 결제원 담당자에게 매터모스트(명세), 결제원이 의견을 남기면 결과를 등록한 기관 담당자에게 알림 센터로만
 * 알린다. 의견에 이어 단 답글도 상대 쪽에 알림 센터로만 알린다.
 */
@Component
class TestPlanNotifier {

  private static final int TITLE_MAX = 200;
  private static final int BODY_MAX = 1000;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final NotificationService notificationService;

  TestPlanNotifier(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /** 기관 테스트 화면 주소. */
  static String linkOf(TestPlan plan) {
    return "/" + plan.getBusiness().getSlug() + "/tests/" + plan.getId();
  }

  /** "2026-10-20~2026-10-31" 꼴. */
  static String periodText(TestPlan plan) {
    return plan.getStartDate().format(DATE) + "~" + plan.getEndDate().format(DATE);
  }

  private static String headline(TestPlan plan) {
    return plan.getBusiness().getLabel() + " " + plan.getTitle();
  }

  /** 테스트가 열렸다(만들었거나 대상 기관으로 더해졌다). 대상 기관 담당자에게 알림과 메일. 테스트데이터를 요청했으면 함께 알린다. */
  void opened(TestPlan plan, Collection<String> organizationEmails) {
    publish(
        organizationEmails,
        plan,
        NotificationType.TEST_PLAN_OPENED,
        headline(plan) + " 기관 테스트가 열렸습니다",
        plan.getSubBusiness()
            + " · 수행 기간 "
            + periodText(plan)
            + " · 요구사항 "
            + plan.getItems().size()
            + "개. "
            + (plan.isTestDataRequested()
                ? testDataText(plan) + " 수행 기간에는 요구사항별 결과를 등록해 주세요."
                : "요구사항별 결과를 등록해 주세요."),
        EnumSet.of(NotificationChannel.EMAIL));
  }

  /** 테스트데이터를 요청한다(새로 요청했거나 미생성 기관에 다시 보냈다). 대상 기관 담당자에게 알림과 메일. */
  void testDataRequested(TestPlan plan, Collection<String> organizationEmails) {
    publish(
        organizationEmails,
        plan,
        NotificationType.TEST_DATA_REQUESTED,
        headline(plan) + " 테스트데이터를 요청합니다",
        plan.getSubBusiness() + " · 수행 기간 " + periodText(plan) + ". " + testDataText(plan),
        EnumSet.of(NotificationChannel.EMAIL));
  }

  /** "수행 전 테스트데이터를 2026-10-25 까지 만들어 올려 주세요(요청 사항: …)." */
  private static String testDataText(TestPlan plan) {
    String text = "수행 전 테스트데이터를 " + plan.getTestDataDueDate().format(DATE) + " 까지 만들어 올려 주세요";
    return plan.getTestDataInstruction() == null
        ? text + "."
        : text + "(요청 사항: " + plan.getTestDataInstruction() + ").";
  }

  /**
   * 기관이 결과를 등록했다. 테스트의 알림 대상 결제원 담당자에게 매터모스트.
   *
   * @param what 바뀐 요구사항 요약(예: "001 정상, 002 오류")
   */
  void reported(
      TestPlan plan,
      TestPlanTarget target,
      Collection<String> recipientEmails,
      String actorName,
      String what) {
    publish(
        recipientEmails,
        plan,
        NotificationType.TEST_RESULT_REPORTED,
        target.getOrganization().getName() + " " + headline(plan) + " 결과 등록",
        actorName + " 님이 결과를 등록했습니다: " + what + ". 기관 수행률 " + target.getProgressRate() + "%.",
        EnumSet.of(NotificationChannel.MATTERMOST));
  }

  /** 기관이 테스트데이터를 생성 완료로 바꿨다. 테스트의 알림 대상 결제원 담당자에게 매터모스트. */
  void testDataCreated(
      TestPlan plan,
      TestPlanTarget target,
      Collection<String> recipientEmails,
      String actorName,
      int fileCount) {
    publish(
        recipientEmails,
        plan,
        NotificationType.TEST_DATA_REPORTED,
        target.getOrganization().getName() + " " + headline(plan) + " 테스트데이터 생성 완료",
        actorName + " 님이 테스트데이터를 생성 완료로 등록했습니다. 올린 파일 " + fileCount + "개.",
        EnumSet.of(NotificationChannel.MATTERMOST));
  }

  /** 결제원이 결과에 의견을 남겼다. 그 결과를 등록한 기관 담당자에게 알림 센터로만. */
  void feedback(TestPlan plan, TestItemResult result, String actorName) {
    String rate =
        result.getFeedbackRate() == null ? "" : " 미니 수행률 " + result.getFeedbackRate() + "%";
    String note = result.getFeedbackNote() == null ? "" : " · " + result.getFeedbackNote();
    publish(
        Set.of(result.getReportedByEmail()),
        plan,
        NotificationType.TEST_RESULT_FEEDBACK,
        headline(plan) + " 결과에 결제원 의견이 있습니다",
        result.getItem().getNumber()
            + " "
            + result.getItem().getTitle()
            + " ·"
            + rate
            + note
            + " ("
            + actorName
            + ")",
        Set.of());
  }

  /**
   * 결제원 의견에 답글이 달렸다. 상대 쪽 담당자에게 알림 센터로만.
   *
   * @param author 글쓴이 표시(예: "KB국민은행(김민준 대리)")
   */
  void commented(
      TestPlan plan,
      TestPlanTarget target,
      TestItemResult result,
      Collection<String> recipientEmails,
      String author,
      String body) {
    publish(
        recipientEmails,
        plan,
        NotificationType.TEST_RESULT_COMMENT,
        target.getOrganization().getName() + " " + headline(plan) + " 의견에 답글이 있습니다",
        result.getItem().getNumber()
            + " "
            + result.getItem().getTitle()
            + " · "
            + author
            + ": "
            + body,
        Set.of());
  }

  private void publish(
      Collection<String> recipientEmails,
      TestPlan plan,
      NotificationType type,
      String title,
      String body,
      Set<NotificationChannel> channels) {
    for (String email : new LinkedHashSet<>(recipientEmails)) {
      if (email == null || email.isBlank()) {
        continue;
      }
      notificationService.publish(
          new NotificationCommand(
              email,
              type,
              plan.getBusiness(),
              cut(title, TITLE_MAX),
              cut(body, BODY_MAX),
              linkOf(plan),
              channels));
    }
  }

  /** 알림 칼럼 길이(title 200, body 1000)에 맞춘다. */
  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max - 1) + "…";
  }
}
