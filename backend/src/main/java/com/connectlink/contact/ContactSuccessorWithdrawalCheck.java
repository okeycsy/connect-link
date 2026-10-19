package com.connectlink.contact;

import com.connectlink.account.WithdrawalBlocker;
import com.connectlink.account.WithdrawalCheck;
import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 후임자 규칙(명세 "기관 담당자가 혼자면 후임자 정보를 입력해야 삭제"). 참가기관 담당자가 어떤 업무에서 기관의 유일한 담당자면, 그 업무에 후임자를 연락처에 등록하기
 * 전까지 탈퇴하지 못한다. 결제원 담당자에게는 쓰지 않는다.
 */
@Component
class ContactSuccessorWithdrawalCheck implements WithdrawalCheck {

  static final String CODE = "CONTACT_SUCCESSOR_REQUIRED";

  private final ContactService contactService;

  ContactSuccessorWithdrawalCheck(ContactService contactService) {
    this.contactService = contactService;
  }

  @Override
  public List<WithdrawalBlocker> check(CurrentUser account) {
    List<Business> sole =
        contactService.soleContactBusinesses(account.email(), account.organizationCode());
    if (sole.isEmpty()) {
      return List.of();
    }
    String labels = sole.stream().map(Business::getLabel).collect(Collectors.joining(", "));
    return List.of(
        new WithdrawalBlocker(
            CODE,
            account.organizationName()
                + " 의 "
                + labels
                + " 담당자가 본인뿐입니다. 연락처에 후임자를 먼저 등록하면 탈퇴할 수 있습니다.",
            "/" + sole.get(0).getSlug() + "/contacts"));
  }
}
