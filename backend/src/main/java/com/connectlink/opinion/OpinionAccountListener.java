package com.connectlink.opinion;

import com.connectlink.account.AccountWithdrawnEvent;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정이 탈퇴하면 같은 이메일의 회신 담당자를 의견 요청에서 뺀다(연락처도 함께 지워진다). 탈퇴와 같은 트랜잭션이라 실패하면 탈퇴도 되돌아간다. 그 사람이 쓴 회신은 기관의
 * 회신이라 남긴다(이름은 쓸 때 그대로).
 */
@Component
class OpinionAccountListener {

  private final OpinionTargetRecipientRepository recipientRepository;

  OpinionAccountListener(OpinionTargetRecipientRepository recipientRepository) {
    this.recipientRepository = recipientRepository;
  }

  @EventListener
  @Transactional
  public void onWithdrawn(AccountWithdrawnEvent event) {
    String email = event.email().toLowerCase(Locale.ROOT);
    for (OpinionTargetRecipient recipient : recipientRepository.findByEmail(email)) {
      recipient.getTarget().removeRecipient(email);
    }
  }
}
