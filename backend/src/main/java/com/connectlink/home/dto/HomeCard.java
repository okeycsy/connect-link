package com.connectlink.home.dto;

import com.connectlink.home.HomeCardKey;
import com.connectlink.home.HomeCardSource;
import com.connectlink.home.Urgency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * 홈 카드 한 장. 기능 모듈이 무엇을 채우든 화면은 이 모양만 안다.
 *
 * @param urgency 카드 전체의 급한 정도. 가장 급한 카드가 홈에서 크게 보인다
 * @param link "모두 보기" 가 여는 주소(예: /account-info/contacts)
 */
public record HomeCard(
    HomeCardKey key,
    String title,
    HomeCardSource source,
    Urgency urgency,
    @Schema(nullable = true) String summary,
    List<HomeCardMetric> metrics,
    @Schema(nullable = true) HomeCardProgress progress,
    List<HomeCardItem> items,
    @Schema(nullable = true) String link) {

  /** 카드 위쪽의 숫자(예: 미확인 7명). */
  public record HomeCardMetric(String label, long value, String unit) {}

  /** 진행률(예: 기관 12/18). */
  public record HomeCardProgress(long done, long total, String label) {}

  /** 카드 안의 할 일 한 줄. */
  public record HomeCardItem(
      String title,
      @Schema(nullable = true) String subtitle,
      @Schema(nullable = true) LocalDate dueAt,
      Urgency urgency,
      @Schema(nullable = true) String link) {}

  /** 남색 밴드의 글래스 타일에 올리는 숫자. 카드와 함께 {@code HomeContribution} 으로 전달된다. */
  public record HomeCardKpi(String label, long value, String unit, boolean urgent) {}

  /** 기여자가 없는 자리. */
  public static HomeCard unavailable(HomeCardKey key, String link) {
    return new HomeCard(
        key,
        key.getTitle(),
        HomeCardSource.UNAVAILABLE,
        Urgency.NONE,
        null,
        List.of(),
        null,
        List.of(),
        link);
  }
}
