package com.connectlink.home.sample;

import com.connectlink.auth.CurrentUser;
import com.connectlink.home.HomeCardContributor;
import com.connectlink.home.HomeCardKey;
import com.connectlink.home.HomeCardSource;
import com.connectlink.home.HomeContribution;
import com.connectlink.home.Urgency;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeCard.HomeCardItem;
import com.connectlink.home.dto.HomeCard.HomeCardKpi;
import com.connectlink.home.dto.HomeCard.HomeCardMetric;
import com.connectlink.home.dto.HomeCard.HomeCardProgress;
import com.connectlink.organization.Business;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 기능 프로젝트가 붙기 전까지 홈 카드 자리를 채우는 샘플 기여자들(SHY 결정 2026-10-15, "샘플로 채우기").
 *
 * <p>전부 가상 데이터이며 local·demo·test 프로필에만 있다. 각 기능 프로젝트가 자기 모듈에 실제 {@link HomeCardContributor} 를 만들면 그
 * 자리의 샘플 클래스를 지운다. 날짜는 오늘 기준 상대값이라 시연 때 "임박·초과" 가 항상 보인다.
 */
public final class SampleHomeCards {

  private SampleHomeCards() {}

  /** 업무 영역마다 숫자가 달라 보이도록 쓰는 작은 편차(0~3). */
  private static int shift(Business business) {
    return business.ordinal();
  }

  private static String link(Business business, HomeCardKey key) {
    return "/" + business.getSlug() + "/" + key.getFeature();
  }

  private static HomeContribution card(
      HomeCardKey key,
      Business business,
      Urgency urgency,
      String summary,
      List<HomeCardMetric> metrics,
      HomeCardProgress progress,
      List<HomeCardItem> items,
      HomeCardKpi kpi) {
    return new HomeContribution(
        new HomeCard(
            key,
            key.getTitle(),
            HomeCardSource.SAMPLE,
            urgency,
            summary,
            metrics,
            progress,
            items,
            link(business, key)),
        kpi);
  }

  /** 가. 최근 확인일 6개월 초과 담당자. */
  @Component
  @Profile({"local", "demo", "test"})
  public static class ContactConfirm implements HomeCardContributor {
    @Override
    public HomeCardKey key() {
      return HomeCardKey.CONTACT_CONFIRM;
    }

    @Override
    public HomeContribution contribute(CurrentUser user, Business business) {
      LocalDate today = LocalDate.now();
      int overdue = 7 - shift(business);
      String link = link(business, key());
      return card(
          key(),
          business,
          Urgency.OVERDUE,
          "최근 확인일이 6개월을 넘긴 담당자에게 확인 요청 메일이 나갔습니다.",
          List.of(
              new HomeCardMetric("6개월 초과", overdue, "명"),
              new HomeCardMetric("전체 담당자", 42 + shift(business) * 3, "명")),
          null,
          List.of(
              new HomeCardItem(
                  "가나은행 · 김가나 (현업)",
                  "최근 확인 " + today.minusMonths(8).withDayOfMonth(2),
                  null,
                  Urgency.OVERDUE,
                  link),
              new HomeCardItem(
                  "마바저축은행 · 이창수 (전산·운영)",
                  "최근 확인 " + today.minusMonths(7).withDayOfMonth(15),
                  null,
                  Urgency.OVERDUE,
                  link),
              new HomeCardItem(
                  "타파보험 · 정타파 (현업)",
                  "최근 확인 " + today.minusMonths(6).minusDays(3),
                  null,
                  Urgency.OVERDUE,
                  link),
              new HomeCardItem(
                  "자차카드 · 윤자차 (전산·대외계)",
                  "최근 확인 " + today.minusMonths(6).minusDays(9),
                  null,
                  Urgency.OVERDUE,
                  link),
              new HomeCardItem(
                  "하하통신 · 오하하 (현업)",
                  "최근 확인 " + today.minusMonths(6).minusDays(12),
                  null,
                  Urgency.OVERDUE,
                  link)),
          new HomeCardKpi("확인 필요 담당자", overdue, "명", overdue > 0));
    }
  }

  /** 가. 정기 변경(현행화) 주간 진행률. */
  @Component
  @Profile({"local", "demo", "test"})
  public static class UpdateCampaign implements HomeCardContributor {
    @Override
    public HomeCardKey key() {
      return HomeCardKey.UPDATE_CAMPAIGN;
    }

    @Override
    public HomeContribution contribute(CurrentUser user, Business business) {
      LocalDate today = LocalDate.now();
      LocalDate end = today.plusDays(6);
      int total = 18;
      int done = 11 + shift(business);
      String link = link(business, key());
      return card(
          key(),
          business,
          Urgency.SOON,
          today.minusDays(3) + " ~ " + end + " 정기 변경 주간. 마감까지 6일.",
          List.of(
              new HomeCardMetric("완료 기관", done, "곳"), new HomeCardMetric("미완료", total - done, "곳")),
          new HomeCardProgress(done, total, "기관"),
          List.of(
              new HomeCardItem("다라은행 · 미확인", "담당자 3명 중 1명 확인", end, Urgency.SOON, link),
              new HomeCardItem("사아증권 · 미확인", "담당자 2명 모두 미확인", end, Urgency.SOON, link)),
          null);
    }
  }

  /** 나. 회신 대기 의견 요청. */
  @Component
  @Profile({"local", "demo", "test"})
  public static class OpinionReplies implements HomeCardContributor {
    @Override
    public HomeCardKey key() {
      return HomeCardKey.OPINION_REPLIES;
    }

    @Override
    public HomeContribution contribute(CurrentUser user, Business business) {
      LocalDate today = LocalDate.now();
      int open = 2 + (shift(business) % 2);
      String link = link(business, key());
      String label = business.getLabel();
      return card(
          key(),
          business,
          Urgency.SOON,
          "회신중인 요청 " + open + "건. 기한이 가까운 요청에 리마인드를 보낼 수 있습니다.",
          List.of(
              new HomeCardMetric("회신중", open, "건"),
              new HomeCardMetric("회신율", 67 - shift(business) * 5, "%")),
          new HomeCardProgress(12 - shift(business), 18, "기관 회신"),
          List.of(
              new HomeCardItem(
                  label + " 수수료 체계 개편 의견", "회신 12/18 기관", today.plusDays(2), Urgency.SOON, link),
              new HomeCardItem(
                  label + " 전문 항목 추가 검토", "회신 4/18 기관", today.plusDays(9), Urgency.NONE, link)),
          new HomeCardKpi("회신 대기 의견 요청", open, "건", false));
    }
  }

  /** 다. 진행 중 기관 테스트. */
  @Component
  @Profile({"local", "demo", "test"})
  public static class TestProgress implements HomeCardContributor {
    @Override
    public HomeCardKey key() {
      return HomeCardKey.TEST_PROGRESS;
    }

    @Override
    public HomeContribution contribute(CurrentUser user, Business business) {
      int running = 1 + (shift(business) % 2);
      int passed = 9 + shift(business) * 2;
      String link = link(business, key());
      return card(
          key(),
          business,
          Urgency.NONE,
          "요구사항별 결과(정상·오류·수행중·미수행)를 기관이 등록하면 수행률이 올라갑니다.",
          List.of(new HomeCardMetric("진행 중", running, "건"), new HomeCardMetric("오류 보고", 2, "건")),
          new HomeCardProgress(passed, 28, "요구사항 정상"),
          List.of(
              new HomeCardItem(
                  business.getLabel() + " 전문 개정 2차 테스트",
                  "대상 7개 기관 · 수행률 " + (passed * 100 / 28) + "%",
                  null,
                  Urgency.NONE,
                  link),
              new HomeCardItem("자차카드 오류 2건 피드백 대기", "요구사항 #7, #12", null, Urgency.NONE, link)),
          new HomeCardKpi("진행 중 기관 테스트", running, "건", false));
    }
  }

  /** 라. 오늘·예정 일시중단. */
  @Component
  @Profile({"local", "demo", "test"})
  public static class Suspensions implements HomeCardContributor {
    @Override
    public HomeCardKey key() {
      return HomeCardKey.SUSPENSIONS;
    }

    @Override
    public HomeContribution contribute(CurrentUser user, Business business) {
      LocalDate today = LocalDate.now();
      boolean todayHas = shift(business) % 2 == 0;
      String link = link(business, key());
      List<HomeCardItem> items =
          todayHas
              ? List.of(
                  new HomeCardItem(
                      "다라은행 · 02:00~04:00", "코어뱅킹 정기 점검 · 확정", today, Urgency.SOON, link),
                  new HomeCardItem(
                      "하하통신 · 23:00~01:00",
                      "망 장비 교체 · 검토 중",
                      today.plusDays(3),
                      Urgency.NONE,
                      link))
              : List.of(
                  new HomeCardItem(
                      "사아증권 · 01:00~03:00",
                      "DR 전환 훈련 · 확정",
                      today.plusDays(2),
                      Urgency.NONE,
                      link));
      return card(
          key(),
          business,
          todayHas ? Urgency.SOON : Urgency.NONE,
          todayHas ? "오늘 확정된 일시중단이 1건 있습니다." : "오늘 일시중단은 없습니다.",
          List.of(
              new HomeCardMetric("오늘", todayHas ? 1 : 0, "건"),
              new HomeCardMetric("예정", items.size() - (todayHas ? 1 : 0), "건")),
          null,
          items,
          new HomeCardKpi("오늘·예정 일시중단", items.size(), "건", todayHas));
    }
  }

  /** 공통. 가입 승인 대기. 계정 모듈이 붙으면 실제 신청 목록으로 바꾼다. */
  @Component
  @Profile({"local", "demo", "test"})
  public static class SignupApproval implements HomeCardContributor {
    @Override
    public HomeCardKey key() {
      return HomeCardKey.SIGNUP_APPROVAL;
    }

    @Override
    public HomeContribution contribute(CurrentUser user, Business business) {
      LocalDate today = LocalDate.now();
      String link = link(business, key());
      return card(
          key(),
          business,
          Urgency.NONE,
          "이메일 인증을 마친 신청입니다. 도메인과 기관이 맞는지 보고 승인합니다.",
          List.of(new HomeCardMetric("대기", 2, "건")),
          null,
          List.of(
              new HomeCardItem(
                  "가나은행 · 김가나",
                  business.getLabel() + " · 현업 · 도메인 일치",
                  today.minusDays(1),
                  Urgency.NONE,
                  link),
              new HomeCardItem(
                  "다라은행 · 박다라",
                  business.getLabel() + " · 전산(개발) · 도메인 일치",
                  today.minusDays(2),
                  Urgency.NONE,
                  link)),
          null);
    }
  }
}
