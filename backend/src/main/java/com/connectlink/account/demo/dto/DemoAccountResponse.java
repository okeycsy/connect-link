package com.connectlink.account.demo.dto;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import java.util.Comparator;
import java.util.List;

/** 데모 계정 한 건. 고르는 화면에 이름, 소속, 역할, 담당 업무를 보여 준다. 기관 구분은 아바타 색(결제원/기관)을 정한다. */
public record DemoAccountResponse(
    String email,
    String name,
    String organizationName,
    OrganizationType organizationType,
    String roleLabel,
    boolean admin,
    List<String> businessLabels) {

  public static DemoAccountResponse from(CurrentUser user) {
    return new DemoAccountResponse(
        user.email(),
        user.name(),
        user.organizationName(),
        user.organizationType(),
        user.roleLabel(),
        user.admin(),
        user.businesses().stream()
            .sorted(Comparator.naturalOrder())
            .map(Business::getLabel)
            .toList());
  }
}
