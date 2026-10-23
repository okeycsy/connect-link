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

/** 일시중단 양식의 기관 작업 담당자 연락처 한 명. 적은 순서({@code sortOrder})대로 보인다. */
@Entity
@Table(name = "suspension_contact")
public class SuspensionContact extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "suspension_id")
  private Suspension suspension;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "phone", nullable = false, length = 30)
  private String phone;

  @Column(name = "email", length = 200)
  private String email;

  protected SuspensionContact() {}

  SuspensionContact(Suspension suspension, int sortOrder, Suspension.ContactInfo info) {
    this.suspension = suspension;
    this.sortOrder = sortOrder;
    apply(info);
  }

  void apply(Suspension.ContactInfo info) {
    this.name = info.name();
    this.phone = info.phone();
    this.email = info.email();
  }

  Suspension.ContactInfo info() {
    return new Suspension.ContactInfo(name, phone, email);
  }

  public Long getId() {
    return id;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }
}
