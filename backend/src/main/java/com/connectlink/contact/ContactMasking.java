package com.connectlink.contact;

/** 이전 담당자 정보 가리기. 화면은 서버가 가린 값을 그대로 보인다(프론트 shared/format.ts 의 formatPhone 이 * 가 든 값은 건드리지 않는다). */
final class ContactMasking {

  private ContactMasking() {}

  /** 휴대폰번호 가운데를 가린다. 010-1234-5678 → 010-****-5678. 비었으면 null. */
  static String mobile(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String digits = value.replaceAll("\\D", "");
    if (digits.length() < 7) {
      return "*".repeat(digits.length());
    }
    String head = digits.substring(0, 3);
    String tail = digits.substring(digits.length() - 4);
    return head + "-" + "*".repeat(digits.length() - 7) + "-" + tail;
  }

  /** 회사 이메일 아이디를 앞 두 글자만 남기고 가린다. hong.gildong@kftc.example.com → ho***@kftc.example.com. */
  static String email(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    int at = value.indexOf('@');
    if (at <= 0) {
      return "***";
    }
    String id = value.substring(0, at);
    return id.substring(0, Math.min(2, id.length())) + "***" + value.substring(at);
  }
}
