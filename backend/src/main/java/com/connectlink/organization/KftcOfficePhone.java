package com.connectlink.organization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 결제원 담당자 내선번호를 결제원 밖에서 거는 전체 번호로 바꾼다. 결제원 담당자는 결제원 안에서 쓰는 4자리 내선만 적고({@link OfficePhone}), 참가기관 등
 * 다른 기관 담당자에게는 대표 국번(설정 {@code app.organization.kftc-office-phone-prefix}, 예: 02-000-)을 앞에 붙여 보인다.
 * 저장 값은 그대로 4자리다.
 *
 * <p>공개 저장소라 실제 국번은 배포 설정(환경 변수 APP_ORGANIZATION_KFTC_OFFICE_PHONE_PREFIX)에만 넣는다. 비우면 바꾸지 않는다.
 */
@Component
public class KftcOfficePhone {

  private final String prefix;

  public KftcOfficePhone(@Value("${app.organization.kftc-office-phone-prefix:}") String prefix) {
    this.prefix = prefix == null ? "" : prefix.trim();
  }

  /** 결제원 내선(지역번호가 없는 짧은 번호)이면 국번을 붙인 전체 번호, 이미 전체 번호이거나 국번 설정이 없으면 그대로. 1101 → 02-000-1101. */
  public String fullNumber(String value) {
    if (value == null || value.isBlank() || prefix.isEmpty()) {
      return value;
    }
    String trimmed = value.trim();
    if (trimmed.replaceAll("\\D", "").length() >= OfficePhone.FULL_NUMBER_MIN_DIGITS) {
      return trimmed;
    }
    return prefix + trimmed;
  }

  /** 보는 사람이 결제원 쪽이 아니고 번호 주인이 결제원 담당자일 때만 전체 번호로 바꾼다. 결제원 사람끼리는 4자리 내선 그대로다. */
  public String forViewer(boolean viewerKftcSide, OrganizationType ownerType, String value) {
    if (viewerKftcSide || ownerType != OrganizationType.KFTC) {
      return value;
    }
    return fullNumber(value);
  }
}
