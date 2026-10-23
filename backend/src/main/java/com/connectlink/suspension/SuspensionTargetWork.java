package com.connectlink.suspension;

import com.connectlink.common.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** 일시중단이 고른 대상 업무 하나. 고를 때의 이름을 남긴다. */
@Entity
@Table(name = "suspension_target_work")
public class SuspensionTargetWork extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "suspension_id")
  private Suspension suspension;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "target_work_id")
  private TargetWork targetWork;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  protected SuspensionTargetWork() {}

  SuspensionTargetWork(Suspension suspension, TargetWork targetWork) {
    this.suspension = suspension;
    this.targetWork = targetWork;
    this.name = targetWork.getName();
  }

  public Long getId() {
    return id;
  }

  public TargetWork getTargetWork() {
    return targetWork;
  }

  public String getName() {
    return name;
  }
}
