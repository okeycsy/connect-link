package com.connectlink.account;

import com.connectlink.common.Labeled;

/** 계정 상태. 승인 전·반려·탈퇴 계정은 로그인할 수 없다. */
public enum AccountStatus implements Labeled {
  PENDING_APPROVAL("승인 대기"),
  ACTIVE("사용 중"),
  REJECTED("반려"),
  WITHDRAWN("탈퇴");

  private final String label;

  AccountStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
