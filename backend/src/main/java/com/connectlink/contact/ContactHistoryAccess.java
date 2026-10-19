package com.connectlink.contact;

import com.connectlink.auth.CurrentUser;
import com.connectlink.history.HistoryTargetAccess;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.history.dto.FieldChange;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당자 변경 이력 권한과 마스킹(명세). 담당자를 볼 수 있는 사람이 이력을 보되, 이전 담당자 정보는 내선번호만 그대로 보이고 휴대폰번호(와 회사 이메일)는 가린다. 이력은
 * 가장 최근 한 건(이전 담당자 한 명)까지만 보인다. 지운 담당자는 대상이 없어 아무도 열지 않는다.
 */
@Component
@Transactional(readOnly = true)
class ContactHistoryAccess implements HistoryTargetAccess {

  private final ContactRepository repository;
  private final ContactAccessPolicy accessPolicy;

  ContactHistoryAccess(ContactRepository repository, ContactAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public HistoryTargetType targetType() {
    return HistoryTargetType.CONTACT;
  }

  @Override
  public boolean canView(CurrentUser user, Long targetId) {
    return repository.findById(targetId).map(c -> accessPolicy.canView(user, c)).orElse(false);
  }

  /** 변경 전 값(이전 담당자)의 휴대폰번호와 회사 이메일을 가린다. 변경 후 값은 지금 담당자라 그대로다. */
  @Override
  public FieldChange redact(CurrentUser user, FieldChange change) {
    return switch (change.field()) {
      case "mobilePhone" ->
          new FieldChange(
              change.field(),
              change.label(),
              ContactMasking.mobile(change.before()),
              change.after());
      case "email" ->
          new FieldChange(
              change.field(),
              change.label(),
              ContactMasking.email(change.before()),
              change.after());
      default -> change;
    };
  }

  /** 이전 이력은 한 명까지만(명세). */
  @Override
  public int maxVisible(CurrentUser user) {
    return 1;
  }
}
