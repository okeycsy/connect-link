package com.connectlink.testplan.dto;

import java.util.List;
import javax.validation.constraints.Size;

/** 알림 받을 결제원 담당자(그 업무를 맡은 결제원 계정)를 통째로 바꾼다. 비워도 된다. */
public record TestPlanRecipientsRequest(
    @Size(max = 100, message = "알림 대상은 100명까지입니다.") List<Long> accountIds) {

  public TestPlanRecipientsRequest {
    accountIds = accountIds == null ? List.of() : List.copyOf(accountIds);
  }
}
