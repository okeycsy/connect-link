package com.connectlink.contact;

import com.connectlink.account.AccountApprovedEvent;
import com.connectlink.account.AccountWithdrawnEvent;
import com.connectlink.auth.CurrentUserProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정과 담당자 연락처를 잇는다. 가입이 승인되면 가입 때 받은 정보로 담당자 행을 만들고, 탈퇴하면 본인 행을 지운다. 승인·탈퇴와 같은 트랜잭션이라 실패하면 승인·탈퇴도
 * 되돌아간다.
 */
@Component
class ContactAccountListener {

  private final ContactService contactService;
  private final CurrentUserProvider currentUserProvider;

  ContactAccountListener(ContactService contactService, CurrentUserProvider currentUserProvider) {
    this.contactService = contactService;
    this.currentUserProvider = currentUserProvider;
  }

  @EventListener
  @Transactional
  public void onApproved(AccountApprovedEvent event) {
    contactService.createFromApprovedAccount(event);
  }

  /** 지운 사람은 탈퇴한 본인이거나 탈퇴 처리한 결제원 담당자(지금 요청의 로그인 사용자)다. */
  @EventListener
  @Transactional
  public void onWithdrawn(AccountWithdrawnEvent event) {
    contactService.removeWithdrawn(
        currentUserProvider.require(), event.email(), event.organizationCode());
  }
}
