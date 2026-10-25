package com.connectlink.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 기관 한눈에 보기 칸의 항목 한 줄. 연락처 칸만 전화·이메일을 채우고(가림은 서버가 이미 했다, 표기는 화면이 한다), 나머지 칸은 비운다.
 *
 * @param badge 상태 표시(예: 미회신, 주담당). 없으면 null
 * @param attention 눈에 띄게 보일지(기한 지남, 확인 필요, 진행 중 일시중단 등)
 * @param link 화면 주소
 */
public record OrganizationOverviewItem(
    String title,
    @Schema(nullable = true) String subtitle,
    @Schema(nullable = true) String badge,
    boolean attention,
    String link,
    @Schema(nullable = true) String extensionPhone,
    @Schema(nullable = true) String mobilePhone,
    @Schema(nullable = true) String email) {

  /** 전화·이메일이 없는 항목. */
  public static OrganizationOverviewItem of(
      String title, String subtitle, String badge, boolean attention, String link) {
    return new OrganizationOverviewItem(title, subtitle, badge, attention, link, null, null, null);
  }
}
