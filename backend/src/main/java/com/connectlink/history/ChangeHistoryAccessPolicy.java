package com.connectlink.history;

import com.connectlink.auth.CurrentUser;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** 변경 이력 모듈의 권한 규칙. 대상 종류별 {@link HistoryTargetAccess} 빈에 묻고, 빈이 없는 대상은 거부한다. */
@Component
public class ChangeHistoryAccessPolicy {

  private final Map<HistoryTargetType, HistoryTargetAccess> accesses =
      new EnumMap<>(HistoryTargetType.class);

  public ChangeHistoryAccessPolicy(List<HistoryTargetAccess> accessBeans) {
    for (HistoryTargetAccess access : accessBeans) {
      HistoryTargetAccess previous = accesses.putIfAbsent(access.targetType(), access);
      if (previous != null) {
        throw new IllegalStateException(
            "변경 이력 대상 "
                + access.targetType()
                + " 의 권한 빈이 둘입니다: "
                + previous.getClass()
                + ", "
                + access.getClass());
      }
    }
  }

  public boolean canView(CurrentUser user, HistoryTargetType targetType, Long targetId) {
    HistoryTargetAccess access = accesses.get(targetType);
    return access != null && access.canView(user, targetId);
  }

  public void ensureCanView(CurrentUser user, HistoryTargetType targetType, Long targetId) {
    if (!canView(user, targetType, targetId)) {
      throw new AccessDeniedException("이 대상의 변경 이력을 볼 권한이 없습니다.");
    }
  }

  /** 대상 모듈의 마스킹 규칙을 적용한 항목. 빈이 없으면 그대로다(여기까지 왔다면 이미 조회가 허용된 상태다). */
  HistoryTargetAccess accessFor(HistoryTargetType targetType) {
    return accesses.get(targetType);
  }
}
