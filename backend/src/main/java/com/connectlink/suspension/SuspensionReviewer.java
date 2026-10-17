package com.connectlink.suspension;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** 일시중단 확인을 시작한 결제원 담당자 한 명. 시작할 때의 이름·직무를 남긴다. */
@Entity
@Table(name = "suspension_reviewer")
public class SuspensionReviewer extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "suspension_id")
  private Suspension suspension;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /** 직무 표기(예: "전산(운영)"). 업무팀·개발팀·운영팀 중 누가 확인했는지 보인다. */
  @Column(name = "role_label", nullable = false, length = 50)
  private String roleLabel;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  protected SuspensionReviewer() {}

  SuspensionReviewer(
      Suspension suspension, String email, String name, String roleLabel, LocalDateTime startedAt) {
    this.suspension = suspension;
    this.email = email;
    this.name = name;
    this.roleLabel = roleLabel;
    this.startedAt = startedAt;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String getRoleLabel() {
    return roleLabel;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }
}
