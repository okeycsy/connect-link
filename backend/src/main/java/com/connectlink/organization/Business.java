package com.connectlink.organization;

/** 업무 영역. 화면 왼쪽 레일의 항목이자 조회·수정 범위의 단위다. 세부업무는 자유 텍스트({@code subBusiness})로 따로 받는다. */
public enum Business {
  ACCOUNT_INFO("account-info", "계좌통합관리", "계통"),
  AUTO_TRANSFER("auto-transfer", "자동이체통합관리", "자통"),
  LOAN_TRANSFER("loan-transfer", "대출이동", "대출"),
  CMS("cms", "CMS", "CMS");

  /** 화면 주소에 쓰는 slug (예: /account-info/home). */
  private final String slug;

  private final String label;

  /** 레일 토큰에 쓰는 짧은 이름. */
  private final String shortLabel;

  Business(String slug, String label, String shortLabel) {
    this.slug = slug;
    this.label = label;
    this.shortLabel = shortLabel;
  }

  public String getSlug() {
    return slug;
  }

  public String getLabel() {
    return label;
  }

  public String getShortLabel() {
    return shortLabel;
  }

  /** 주소의 slug 로 업무 영역을 찾는다. 없으면 비어 있다. */
  public static java.util.Optional<Business> fromSlug(String slug) {
    return java.util.Arrays.stream(values()).filter(b -> b.slug.equals(slug)).findFirst();
  }
}
