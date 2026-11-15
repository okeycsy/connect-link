package com.connectlink.attachment;

import com.connectlink.common.Labeled;

/**
 * 첨부파일의 반출 결재 상태. 결제원 담당자가 내부망에서 올린 파일은 참가기관이 외부망에서 보게 되므로 외부망 반출이고, 결제원 규정대로 같은 팀 팀장의 결재를 받아야
 * 참가기관에 보인다. 결재 전·반려된 파일은 결제원 계정에만 보인다.
 */
public enum AttachmentApprovalStatus implements Labeled {
  /** 결재가 필요 없다(참가기관이 올린 파일, 결재 절차 전에 올린 파일). */
  NOT_REQUIRED("결재 필요 없음"),
  PENDING("결재 대기"),
  APPROVED("승인"),
  REJECTED("반려");

  private final String label;

  AttachmentApprovalStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }

  /** 참가기관 등 결제원 밖 계정에게 보이는가. */
  public boolean releasedOutside() {
    return this == NOT_REQUIRED || this == APPROVED;
  }
}
