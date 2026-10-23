package com.connectlink.testplan;

import com.connectlink.account.AccountWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정이 탈퇴하면 기관 테스트의 결과 등록 알림 대상에서 뺀다. 탈퇴와 같은 트랜잭션이라 실패하면 탈퇴도 되돌아간다. 그 사람이 등록한 결과와 의견, 답글은 기관·결제원의
 * 기록이라 남긴다(이름은 쓸 때 그대로).
 */
@Component
class TestPlanAccountListener {

  private final TestPlanRecipientRepository recipientRepository;

  TestPlanAccountListener(TestPlanRecipientRepository recipientRepository) {
    this.recipientRepository = recipientRepository;
  }

  @EventListener
  @Transactional
  public void onWithdrawn(AccountWithdrawnEvent event) {
    for (TestPlanRecipient recipient : recipientRepository.findByAccountId(event.accountId())) {
      recipient.getPlan().removeRecipient(event.accountId());
    }
  }
}
