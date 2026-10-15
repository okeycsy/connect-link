package com.connectlink.account.demo.dto;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import java.util.Comparator;
import java.util.List;

/** 데모 계정 한 건. 고르는 화면에 이름, 소속, 역할, 담당 업무를 보여 준다. */
public record DemoAccountResponse(
    String email,
    String name,
    String organizationName,
    String roleLabel,
    boolean admin,
    List<String> businessLabels) {

  public static DemoAccountResponse from(CurrentUser user) {
    return new DemoAccountResponse(
        user.email(),
        user.name(),
        user.organizationName(),
        user.roleLabel(),
        user.admin(),
        user.businesses().stream()
            .sorted(Comparator.naturalOrder())
            .map(Business::getLabel)
            .toList());
  }
}
