package com.connectlink.auth.audit;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 개인정보 처리 접속기록 한 건(개인정보의 안전성 확보조치 기준 제8조). 누가(계정), 언제(접속 일시), 어디서(접속지 IP), 누구의 정보를(처리한
 * 정보주체: 요청이 가리킨 대상), 무엇을 했는지(수행 업무, 요청 방법·경로, 결과 상태)를 남긴다. 고치지 않고 쌓기만 하며 보관 기간이 지나면 {@link
 * AccessLogCleanupJob} 이 지운다.
 */
@Entity
@Table(name = "access_log")
public class AccessLog extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "accessed_at", nullable = false)
  private LocalDateTime accessedAt;

  @Column(name = "account_email", nullable = false, length = 200)
  private String accountEmail;

  @Column(name = "account_name", length = 50)
  private String accountName;

  @Column(name = "organization_name", length = 100)
  private String organizationName;

  @Column(name = "ip_address", nullable = false, length = 64)
  private String ipAddress;

  /** 수행 업무(예: "담당자 연락처 엑셀 다운로드"). */
  @Column(name = "action", nullable = false, length = 100)
  private String action;

  /** 처리한 정보주체를 가리키는 값(요청 경로의 대상, 예: "담당자 연락처 #12"). 목록 조회처럼 대상이 여럿이면 조회 조건. */
  @Column(name = "target", length = 500)
  private String target;

  @Column(name = "http_method", nullable = false, length = 10)
  private String httpMethod;

  @Column(name = "request_path", nullable = false, length = 500)
  private String requestPath;

  @Column(name = "status_code", nullable = false)
  private int statusCode;

  protected AccessLog() {}

  public AccessLog(
      LocalDateTime accessedAt,
      String accountEmail,
      String accountName,
      String organizationName,
      String ipAddress,
      String action,
      String target,
      String httpMethod,
      String requestPath,
      int statusCode) {
    this.accessedAt = accessedAt;
    this.accountEmail = accountEmail;
    this.accountName = accountName;
    this.organizationName = organizationName;
    this.ipAddress = ipAddress;
    this.action = action;
    this.target = target;
    this.httpMethod = httpMethod;
    this.requestPath = requestPath;
    this.statusCode = statusCode;
  }

  public Long getId() {
    return id;
  }

  public LocalDateTime getAccessedAt() {
    return accessedAt;
  }

  public String getAccountEmail() {
    return accountEmail;
  }

  public String getAccountName() {
    return accountName;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getAction() {
    return action;
  }

  public String getTarget() {
    return target;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public String getRequestPath() {
    return requestPath;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
