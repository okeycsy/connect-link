package com.connectlink.organization;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.dto.OrganizationOverviewItem;
import java.util.List;

/**
 * 기관 한눈에 보기의 칸 기여자. 기능 모듈이 자기 {@link OrganizationOverviewSectionKey} 칸을 채울 때 이 인터페이스의 빈을 하나 만든다. 같은
 * 칸에 기여자가 둘이면 기동이 실패한다.
 */
public interface OrganizationOverviewContributor {

  OrganizationOverviewSectionKey key();

  /**
   * 이 기관의 그 업무 항목. 권한 밖 데이터는 여기서부터 내려보내지 않는다. 이 사용자가 그 기능에서 이 기관을 볼 수 없으면 null 을 돌려주고, 그러면 화면에 그 칸이
   * 없다. 볼 수 있지만 항목이 없으면 빈 목록이다.
   */
  List<OrganizationOverviewItem> contribute(
      CurrentUser user, Business business, Organization organization);
}
