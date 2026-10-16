package com.connectlink.account;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 가입 때 회사 이메일 인증 한 건. 코드를 보내면 생기고, 맞는 코드를 넣으면 인증 토큰을 하나 내준다. 가입 신청은 그 토큰으로 "이 이메일을 인증했다" 를 증명하며, 한
 * 번 쓴 토큰은 다시 못 쓴다. 코드와 토큰은 해시만 둔다.
 */
@Entity
@Table(name = "email_verification")
public class EmailVerification extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  /** 인증 코드의 BCrypt 해시. */
  @Column(name = "code_hash", nullable = false, length = 100)
  private String codeHash;

  /** 코드를 보낸 시각. 재발송 간격의 기준이다. */
  @Column(name = "issued_at", nullable = false)
  private LocalDateTime issuedAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  /** 인증 토큰의 SHA-256 해시(16진수). 맞는 코드를 넣을 때마다 새로 만든다. */
  @Column(name = "token_hash", length = 64, unique = true)
  private String tokenHash;

  @Column(name = "token_expires_at")
  private LocalDateTime tokenExpiresAt;

  /** 가입 신청에 토큰을 쓴 시각. */
  @Column(name = "consumed_at")
  private LocalDateTime consumedAt;

  protected EmailVerification() {}

  EmailVerification(
      String email, String codeHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
    this.email = email;
    this.codeHash = codeHash;
    this.issuedAt = issuedAt;
    this.expiresAt = expiresAt;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public LocalDateTime getIssuedAt() {
    return issuedAt;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public LocalDateTime getTokenExpiresAt() {
    return tokenExpiresAt;
  }

  public boolean isCodeExpired(LocalDateTime now) {
    return !now.isBefore(expiresAt);
  }

  void recordFailedAttempt() {
    failedAttempts++;
  }

  /** 맞는 코드를 확인했다. 앞서 내준 토큰은 버리고 새 토큰으로 바꾼다. */
  void verify(String tokenHash, LocalDateTime now, LocalDateTime tokenExpiresAt) {
    this.verifiedAt = now;
    this.tokenHash = tokenHash;
    this.tokenExpiresAt = tokenExpiresAt;
  }

  /** 가입 신청에 쓸 수 있는 토큰인지: 인증했고, 아직 안 썼고, 유효 시간 안이다. */
  boolean isTokenUsable(LocalDateTime now) {
    return verifiedAt != null
        && consumedAt == null
        && tokenExpiresAt != null
        && now.isBefore(tokenExpiresAt);
  }

  void consume(LocalDateTime now) {
    this.consumedAt = now;
  }
}
