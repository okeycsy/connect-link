package com.connectlink.suspension;

import com.connectlink.auth.CurrentUser;
import com.connectlink.history.HistoryTargetAccess;
import com.connectlink.history.HistoryTargetType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 일시중단 변경 이력 권한. 일시중단을 볼 수 있는 사람(그 업무 결제원 담당자, 그 기관 담당자)이 본다. 지운 건은 대상이 없어 아무도 열지 않는다. */
@Component
@Transactional(readOnly = true)
class SuspensionHistoryAccess implements HistoryTargetAccess {

  private final SuspensionRepository repository;
  private final SuspensionAccessPolicy accessPolicy;

  SuspensionHistoryAccess(SuspensionRepository repository, SuspensionAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public HistoryTargetType targetType() {
    return HistoryTargetType.SUSPENSION;
  }

  @Override
  public boolean canView(CurrentUser user, Long targetId) {
    return repository.findById(targetId).map(s -> accessPolicy.canView(user, s)).orElse(false);
  }
}
