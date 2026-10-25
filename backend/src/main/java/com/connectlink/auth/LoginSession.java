package com.connectlink.auth;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** 세션에 두는 로그인 시각. 비밀번호를 바꾸기 전에 로그인한 다른 세션을 {@link ActiveAccountFilter} 가 끝내는 기준이다. */
public final class LoginSession {

  static final String LOGIN_AT = LoginSession.class.getName() + ".LOGIN_AT";

  private LoginSession() {}

  /** 이 요청의 세션에 로그인 시각을 적는다(DB 시각 칼럼과 같은 밀리초 정밀도). 로그인할 때와, 이 세션에서 비밀번호를 바꿔 이 세션은 남길 때 부른다. */
  public static void markLogin(HttpServletRequest request, LocalDateTime now) {
    request.getSession().setAttribute(LOGIN_AT, now.truncatedTo(ChronoUnit.MILLIS));
  }

  /** 세션의 로그인 시각. 세션이 없거나 적힌 적이 없으면 null. */
  static LocalDateTime loginAt(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session == null ? null : (LocalDateTime) session.getAttribute(LOGIN_AT);
  }
}
