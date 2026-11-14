package com.connectlink.auth.audit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.AntPathMatcher;

/**
 * 접속기록을 남기는 요청과 그 수행 업무 이름. 개인정보(담당자 이름·연락처·이메일)를 조회·등록·수정·삭제·내려받는 API 와 로그인이다. 개인정보를 다루는 API 를
 * 새로 만들면 여기에 한 줄을 더한다.
 *
 * @param method HTTP 방법
 * @param pattern 경로(Ant 패턴, {@code {id}} 는 대상 번호)
 * @param action 수행 업무
 * @param targetLabel 경로의 {@code {id}} 가 가리키는 대상 이름(예: "담당자 연락처"). 없으면 조회 조건(쿼리)을 대상으로 남긴다
 */
record AccessLogRule(String method, String pattern, String action, String targetLabel) {

  private static final AntPathMatcher MATCHER = new AntPathMatcher();

  static final List<AccessLogRule> RULES =
      List.of(
          new AccessLogRule("POST", "/api/v1/auth/login", "로그인", null),
          new AccessLogRule("GET", "/api/v1/contacts", "담당자 연락처 목록 조회", null),
          new AccessLogRule("GET", "/api/v1/contacts/excel", "담당자 연락처 엑셀 다운로드", null),
          new AccessLogRule("POST", "/api/v1/contacts/imports", "담당자 연락처 일괄 등록", null),
          new AccessLogRule("POST", "/api/v1/contacts", "담당자 연락처 등록", null),
          new AccessLogRule("GET", "/api/v1/contacts/{id}", "담당자 연락처 상세 조회", "담당자 연락처"),
          new AccessLogRule("PUT", "/api/v1/contacts/{id}", "담당자 연락처 수정", "담당자 연락처"),
          new AccessLogRule("DELETE", "/api/v1/contacts/{id}", "담당자 연락처 삭제", "담당자 연락처"),
          new AccessLogRule("GET", "/api/v1/sign-ups", "가입 신청 목록 조회", null),
          new AccessLogRule("GET", "/api/v1/sign-ups/{id}", "가입 신청 상세 조회", "계정"),
          new AccessLogRule("POST", "/api/v1/sign-ups/{id}/approval", "가입 승인", "계정"),
          new AccessLogRule("POST", "/api/v1/sign-ups/{id}/rejection", "가입 반려", "계정"),
          new AccessLogRule("POST", "/api/v1/accounts/{id}/withdrawal", "탈퇴 처리", "계정"),
          new AccessLogRule("GET", "/api/v1/change-histories", "변경 이력 조회", null),
          new AccessLogRule("GET", "/api/v1/search", "전역 검색", null),
          new AccessLogRule(
              "GET", "/api/v1/opinion-requests/{id}/collation/excel", "의견 취합표 엑셀 다운로드", "의견 요청"),
          new AccessLogRule("GET", "/api/v1/access-logs", "접속기록 조회", null));

  /** 이 요청에 맞는 규칙. 경로가 정확히 같은 규칙이 패턴 규칙보다 먼저다(/contacts/excel 이 /contacts/{id} 로 잡히지 않게). */
  static Optional<AccessLogRule> match(String method, String path) {
    Optional<AccessLogRule> exact =
        RULES.stream().filter(r -> r.method.equals(method) && r.pattern.equals(path)).findFirst();
    if (exact.isPresent()) {
      return exact;
    }
    return RULES.stream()
        .filter(r -> r.method.equals(method) && r.pattern.contains("{"))
        .filter(r -> MATCHER.match(r.pattern, path))
        .filter(r -> isNumber(MATCHER.extractUriTemplateVariables(r.pattern, path).get("id")))
        .findFirst();
  }

  /** 처리한 정보주체를 가리키는 값. 경로에 대상 번호가 있으면 "이름 #번호", 없으면 조회 조건. */
  String targetOf(String path, String query) {
    if (targetLabel != null) {
      Map<String, String> vars = MATCHER.extractUriTemplateVariables(pattern, path);
      return targetLabel + " #" + vars.get("id");
    }
    if (query == null || query.isBlank()) {
      return null;
    }
    return query.length() > 500 ? query.substring(0, 500) : query;
  }

  private static boolean isNumber(String value) {
    return value != null && !value.isEmpty() && value.chars().allMatch(Character::isDigit);
  }
}
