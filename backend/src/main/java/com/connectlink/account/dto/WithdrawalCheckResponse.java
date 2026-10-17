package com.connectlink.account.dto;

import java.util.List;

/** 탈퇴할 수 있는지. 막는 까닭이 하나라도 있으면 {@code allowed} 는 false 다. */
public record WithdrawalCheckResponse(boolean allowed, List<WithdrawalBlockerResponse> blockers) {

  public static WithdrawalCheckResponse of(List<WithdrawalBlockerResponse> blockers) {
    return new WithdrawalCheckResponse(blockers.isEmpty(), blockers);
  }
}
