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

/** 일시중단 확인을 시작한 결제원 담당자 한 명. 시작할 때의 이름·직무와 이 사람이 쓴 조치·인수인계 사항을 남긴다. */
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

  /** 이 담당자의 조치 및 인수인계 사항. 아직 쓰지 않았으면 null. */
  @Column(name = "handover", length = 2000)
  private String handover;

  /** 사항을 마지막으로 쓴 일시. */
  @Column(name = "handover_at")
  private LocalDateTime handoverAt;

  protected SuspensionReviewer() {}

  SuspensionReviewer(
      Suspension suspension, String email, String name, String roleLabel, LocalDateTime startedAt) {
    this.suspension = suspension;
    this.email = email;
    this.name = name;
    this.roleLabel = roleLabel;
    this.startedAt = startedAt;
  }

  void writeHandover(String handover, LocalDateTime now) {
    this.handover = handover;
    this.handoverAt = now;
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

  public String getHandover() {
    return handover;
  }

  public LocalDateTime getHandoverAt() {
    return handoverAt;
  }
}
