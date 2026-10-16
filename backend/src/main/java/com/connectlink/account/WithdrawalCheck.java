package com.connectlink.account;

import com.connectlink.auth.CurrentUser;
import java.util.List;

/**
 * 탈퇴 전에 확인할 것. 기능 모듈은 자기 데이터 때문에 탈퇴를 막아야 할 때 이 빈 하나를 둔다(예: 연락처 모듈의 "기관에 담당자가 혼자면 후임자를 먼저 등록" 규칙). 내
 * 정보 화면은 탈퇴 버튼을 누르기 전에 막는 까닭을 모두 보여 주고, 탈퇴 API 도 같은 확인을 다시 한다. 탈퇴한 뒤 자기 데이터를 지우는 일은 {@link
 * AccountWithdrawnEvent} 를 받아서 한다.
 */
public interface WithdrawalCheck {

  /** 이 사용자의 탈퇴를 막는 까닭. 막을 까닭이 없으면 빈 목록이다. */
  List<WithdrawalBlocker> check(CurrentUser user);
}
