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
 * @param urgency 카드 전체의 급한 정도
 * @param items 할 일 줄. 비어 있으면 이 사람에게 할 일이 없는 카드라 홈에 나가지 않는다
 * @param link "모두 보기" 가 여는 주소(예: /account-info/contacts)
 */
public record HomeCard(
    HomeCardKey key,
    String title,
    HomeCardSource source,
    Urgency urgency,
    @Schema(nullable = true) String summary,
    List<HomeCardItem> items,
    @Schema(nullable = true) String link) {

  /** 카드 안의 할 일 한 줄. */
  public record HomeCardItem(
      String title,
      @Schema(nullable = true) String subtitle,
      @Schema(nullable = true) LocalDate dueAt,
      Urgency urgency,
      @Schema(nullable = true) String link) {}
}
