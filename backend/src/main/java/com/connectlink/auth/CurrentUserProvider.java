package com.connectlink.auth;

/** 현재 요청의 로그인 사용자를 돌려준다. Controller 는 이것으로 사용자를 얻어 Service 에 넘긴다. */
public interface CurrentUserProvider {

  /**
   * 로그인 사용자.
   *
   * @throws com.connectlink.common.BusinessException 로그인 상태가 아니면 401
   */
  CurrentUser require();
}
