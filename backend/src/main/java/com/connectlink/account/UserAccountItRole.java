package com.connectlink.account;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.ItRole;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** 계정의 전산 세부 직무 한 건. 직무가 전산일 때만 있다. */
@Entity
@Table(name = "user_account_it_role")
public class UserAccountItRole extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private UserAccount account;

  @Enumerated(EnumType.STRING)
  @Column(name = "it_role", nullable = false, length = 30)
  private ItRole itRole;

  protected UserAccountItRole() {}

  UserAccountItRole(UserAccount account, ItRole itRole) {
    this.account = account;
    this.itRole = itRole;
  }

  public ItRole getItRole() {
    return itRole;
  }
}
