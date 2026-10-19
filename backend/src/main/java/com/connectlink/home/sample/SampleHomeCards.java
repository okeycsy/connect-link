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
                  "KB국민은행 · 김민준 (현업)",
                  "최근 확인 " + today.minusMonths(8).withDayOfMonth(2),
                  null,
                  Urgency.OVERDUE,
                  link),
              new HomeCardItem(
                  "SBI저축은행 · 이창수 (전산·운영)",
                  "최근 확인 " + today.minusMonths(7).withDayOfMonth(15),
                  null,
                  Urgency.OVERDUE,
                  link),
              new HomeCardItem(
                  "우리은행 · 정재원 (현업)",
                  "최근 확인 " + today.minusMonths(6).minusDays(3),
                  null,
                  Urgency.OVERDUE,
                  link),
              new HomeCardItem(
                  "우체국 · 윤하은 (전산·대외계)",
                  "최근 확인 " + today.minusMonths(6).minusDays(9),
                  null,
                  Urgency.OVERDUE,
                  link),
              new HomeCardItem(
                  "하하통신 · 오은비 (현업)",
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
              new HomeCardItem("신한은행 · 미확인", "담당자 3명 중 1명 확인", end, Urgency.SOON, link),
              new HomeCardItem("삼성증권 · 미확인", "담당자 2명 모두 미확인", end, Urgency.SOON, link)),
          null);
    }
  }
}
