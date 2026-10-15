package com.connectlink.search;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.search.dto.SearchHit;
import java.util.List;

/**
 * 전역 검색 제공자. 기능 모듈이 자기 {@link SearchCategory} 묶음의 결과를 돌려줄 때 이 인터페이스의 빈을 하나 만든다. 같은 묶음에 제공자가 둘이면 기동이
 * 실패한다(샘플 제공자는 local·demo·test 프로필에만 있으므로 실제 제공자가 생기면 샘플을 지운다).
 */
public interface SearchProvider {

  SearchCategory category();

  default SearchSource source() {
    return SearchSource.LIVE;
  }

  /**
   * 현재 사용자가 볼 수 있는 범위 안에서, 레일의 업무 영역에 맞는 결과를 최대 {@code limit} 건 돌려준다. 권한 밖 데이터는 여기서부터 내려보내지 않는다.
   */
  List<SearchHit> search(CurrentUser user, Business business, String query, int limit);
}
