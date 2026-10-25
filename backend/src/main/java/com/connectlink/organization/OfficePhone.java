package com.connectlink.organization;

import com.connectlink.common.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 담당자 사무실 번호(내선번호 칸) 규칙. 결제원 담당자는 결제원 안에서 쓰는 4자리 내선이면 되지만, 참가기관 등 다른 기관 담당자의 번호는 결제원에서 바로 걸 수 있게
 * 지역번호부터 전체 번호(예: 02-0000-0000)를 적는다. 가입 신청과 담당자 등록·수정·일괄 등록이 같은 규칙을 쓴다.
 */
public final class OfficePhone {

  /** 지역번호를 포함한 전체 번호의 최소 숫자 수(02-000-0000 = 9자리). */
  static final int FULL_NUMBER_MIN_DIGITS = 9;

  public static final String NOT_FULL_NUMBER = "OFFICE_PHONE_FULL_NUMBER_REQUIRED";

  private OfficePhone() {}

  /** 결제원이 아닌 기관이면 전체 번호인지 본다. 아니면 그 칸({@code field})의 400 오류다. */
  public static void requireFullNumberOutsideKftc(
      Organization organization, String value, String field) {
    if (organization.getType() == OrganizationType.KFTC || value == null) {
      return;
    }
    if (value.replaceAll("\\D", "").length() < FULL_NUMBER_MIN_DIGITS) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          NOT_FULL_NUMBER,
          field,
          "기관 담당자 번호는 지역번호부터 전체 번호로 적어 주세요. 예: 02-0000-0000");
    }
  }
}
