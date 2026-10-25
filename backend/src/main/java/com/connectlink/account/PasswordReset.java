package com.connectlink.account;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 비밀번호 찾기 한 건. 회사 이메일로 코드를 보내면 생기고, 맞는 코드와 새 비밀번호를 넣으면 쓴 것으로 남는다. 코드는 해시만 둔다. */
@Entity
@Table(name = "password_reset")
public class PasswordReset extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  /** 코드의 BCrypt 해시. */
  @Column(name = "code_hash", nullable = false, length = 100)
  private String codeHash;

  /** 코드를 보낸 시각. 재발송 간격의 기준이다. */
  @Column(name = "issued_at", nullable = false)
  private LocalDateTime issuedAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  /** 새 비밀번호로 바꾼 시각. */
  @Column(name = "used_at")
  private LocalDateTime usedAt;

  protected PasswordReset() {}

  PasswordReset(String email, String codeHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
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

  boolean isExpired(LocalDateTime now) {
    return !now.isBefore(expiresAt);
  }

  boolean isUsed() {
    return usedAt != null;
  }

  void recordFailedAttempt() {
    failedAttempts++;
  }

  void use(LocalDateTime now) {
    this.usedAt = now;
  }
}
