package com.connectlink.account.dto;

import com.connectlink.account.WithdrawalBlocker;

/** 탈퇴를 막는 까닭 하나. {@code link} 는 그 일을 하는 화면 주소이고 없으면 null 이다. */
public record WithdrawalBlockerResponse(String code, String message, String link) {

  public static WithdrawalBlockerResponse from(WithdrawalBlocker blocker) {
    return new WithdrawalBlockerResponse(blocker.code(), blocker.message(), blocker.link());
  }
}
