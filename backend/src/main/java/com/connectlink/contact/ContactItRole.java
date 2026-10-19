package com.connectlink.contact;

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

/** 담당자의 전산 세부 직무 한 건. 직무가 전산일 때만 있다. */
@Entity
@Table(name = "contact_it_role")
public class ContactItRole extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contact_id", nullable = false)
  private Contact contact;

  @Enumerated(EnumType.STRING)
  @Column(name = "it_role", nullable = false, length = 30)
  private ItRole itRole;

  protected ContactItRole() {}

  ContactItRole(Contact contact, ItRole itRole) {
    this.contact = contact;
    this.itRole = itRole;
  }

  public ItRole getItRole() {
    return itRole;
  }
}
