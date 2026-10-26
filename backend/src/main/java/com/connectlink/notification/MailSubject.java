package com.connectlink.notification;

import com.connectlink.organization.Business;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 메일 제목 머리말. 모든 메일 제목은 "[금융결제원] (업무명) …" 꼴이고, 업무와 무관한 계정 메일(인증 코드, 가입, 탈퇴 등)은 "(연결이)" 다. 이미
 * "[금융결제원]" 으로 시작하는 제목(예: 리마인드)은 그대로 둔다.
 */
public final class MailSubject {

  public static final String SENDER = "[금융결제원]";
  public static final String SERVICE = "연결이";

  private MailSubject() {}

  /** 업무 메일 제목. 업무가 없으면 계정 메일과 같이 "(연결이)" 다. */
  public static String of(Business business, String title) {
    return withScope(business == null ? SERVICE : business.getLabel(), title);
  }

  /** 여러 업무에 걸친 메일 제목(예: 일시중단). 업무를 업무 순서대로 "·" 로 잇고, 없으면 "(연결이)" 다. */
  public static String of(Collection<Business> businesses, String title) {
    String scope =
        businesses.stream().sorted().map(Business::getLabel).collect(Collectors.joining("·"));
    return withScope(scope.isEmpty() ? SERVICE : scope, title);
  }

  /** 계정 메일 제목: "[금융결제원] (연결이) …". */
  public static String service(String title) {
    return withScope(SERVICE, title);
  }

  private static String withScope(String scope, String title) {
    if (title.startsWith(SENDER)) {
      return title;
    }
    return SENDER + " (" + scope + ") " + title;
  }
}
