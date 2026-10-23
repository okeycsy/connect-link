package com.connectlink.suspension;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * 일시중단 대상 업무 기준정보 하나(금융결제원 업무명 중심, 예: 타행환, CD공동망). 레일 업무와 따로이고 수십 개다. 관련 레일 업무를 0개 이상 두며, 일시중단의
 * 목록·알림·확인 범위는 고른 대상 업무의 관련 레일로 정한다. 관련 레일이 없으면 결제원 운영·상황실·대외계가 맡는다. 관리자가 관리한다.
 */
@Entity
@Table(name = "target_work")
public class TargetWork extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  /** 고르기 화면에서 묶어 보이는 분류(예: 공동망, 전자금융). 없으면 null. */
  @Column(name = "category", length = 50)
  private String category;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  /** 끄면 새로 고를 수 없다. 이미 고른 일시중단에는 그대로 남는다. */
  @Column(name = "active", nullable = false)
  private boolean active;

  @OneToMany(mappedBy = "targetWork", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private List<TargetWorkBusiness> businesses = new ArrayList<>();

  protected TargetWork() {}

  public TargetWork(String name, String category, int sortOrder) {
    this.name = name;
    this.category = category;
    this.sortOrder = sortOrder;
    this.active = true;
  }

  public void change(String name, String category, int sortOrder, boolean active) {
    this.name = name;
    this.category = category;
    this.sortOrder = sortOrder;
    this.active = active;
  }

  /** 관련 레일 업무를 바꾼다. 그대로인 행은 둔다. */
  public void replaceBusinesses(Set<Business> next) {
    boolean changed = businesses.removeIf(b -> !next.contains(b.getBusiness()));
    Set<Business> have = getBusinesses();
    for (Business business : next) {
      if (!have.contains(business)) {
        businesses.add(new TargetWorkBusiness(this, business));
        changed = true;
      }
    }
    if (changed) {
      touch();
    }
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public boolean isActive() {
    return active;
  }

  /** 관련 레일 업무. 업무 순서다. */
  public Set<Business> getBusinesses() {
    EnumSet<Business> result = EnumSet.noneOf(Business.class);
    businesses.forEach(b -> result.add(b.getBusiness()));
    return result;
  }
}
