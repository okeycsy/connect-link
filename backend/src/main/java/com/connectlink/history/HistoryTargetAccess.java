package com.connectlink.history;

import com.connectlink.auth.CurrentUser;
import com.connectlink.history.dto.FieldChange;

/**
 * 변경 이력 대상별 권한. 기능 모듈이 자기 {@link HistoryTargetType} 에 대해 이 인터페이스의 빈을 하나 만든다. 이력은 대상을 볼 수 있는 사람만 보며,
 * 빈이 없는 대상의 이력은 아무도 볼 수 없다.
 */
public interface HistoryTargetAccess {

  HistoryTargetType targetType();

  /** 이 대상의 이력을 볼 수 있는가. 대상 자체의 조회 권한과 같게 두는 것이 원칙이다. */
  boolean canView(CurrentUser user, Long targetId);

  /** 보여 주기 전에 항목을 가린다. 기본은 그대로다. 연락처처럼 "이전 담당자의 휴대폰은 마스킹" 같은 규칙이 있는 모듈이 재정의한다. 마스킹은 서버가 한다. */
  default FieldChange redact(CurrentUser user, FieldChange change) {
    return change;
  }

  /**
   * 이 사용자에게 보여 줄 최신 이력의 최대 건수. 기본은 제한이 없다. 연락처처럼 "이전 이력은 한 명까지만" 같은 규칙이 있는 모듈이 재정의한다. 요청한 쪽이 더 큰
   * 쪽수를 달라고 해도 서버가 이 건수에서 자른다.
   */
  default int maxVisible(CurrentUser user) {
    return Integer.MAX_VALUE;
  }
}
