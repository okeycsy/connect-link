package com.connectlink.home;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;

/**
 * 홈 카드 기여자. 기능 모듈이 자기 {@link HomeCardKey} 자리에 카드를 끼울 때 이 인터페이스의 빈을 하나 만든다. 같은 자리에 기여자가 둘이면 기동이
 * 실패한다(샘플 기여자는 local·demo 프로필에만 있으므로 실제 기여자가 생기면 샘플을 지운다).
 */
public interface HomeCardContributor {

  HomeCardKey key();

  /**
   * 현재 사용자와 레일의 업무 영역에 맞는 카드(와 KPI). 권한 밖 데이터는 여기서부터 내려보내지 않는다. 이 사용자가 쓰지 않는 자리면(예: 참가기관 담당자의 가입
   * 승인) null 을 돌려주고, 그러면 홈에 그 카드가 없다.
   */
  HomeContribution contribute(CurrentUser user, Business business);
}
