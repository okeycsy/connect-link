package com.connectlink.attachment;

import com.connectlink.common.Labeled;

/**
 * 첨부파일의 반입 보안 검사 상태. 결제원 밖 계정(참가기관 담당자 등)이 외부망에서 올린 파일은 결제원이 내부망에서 열어 보게 되므로 내부망 반입이고, 보안 검사({@link
 * InboundFileInspector})를 통과해야 내려받을 수 있다. 검사 중·차단 파일은 목록에 이름과 상태만 보인다.
 */
public enum AttachmentInspectionStatus implements Labeled {
  /** 검사가 필요 없다(결제원 계정이 올린 파일, 검사 절차 전에 올린 파일). */
  NOT_REQUIRED("검사 필요 없음"),
  PENDING("보안 검사 중"),
  PASSED("검사 완료"),
  BLOCKED("반입 차단");

  private final String label;

  AttachmentInspectionStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }

  /** 내려받을 수 있는가. */
  public boolean downloadable() {
    return this == NOT_REQUIRED || this == PASSED;
  }
}
