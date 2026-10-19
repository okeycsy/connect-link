package com.connectlink.testplan;

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

/** 테스트 요구사항 하나. 대상 기관이 이것마다 결과를 등록한다. 번호(001, 002 …)는 순서대로 화면이 붙인다. */
@Entity
@Table(name = "test_item")
public class TestItem extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_plan_id")
  private TestPlan plan;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  /** 요구사항. */
  @Column(name = "title", nullable = false, length = 500)
  private String title;

  /** 확인 사항(기대 결과, 수행 방법 등). */
  @Column(name = "description", length = 2000)
  private String description;

  protected TestItem() {}

  TestItem(TestPlan plan, int sortOrder, String title, String description) {
    this.plan = plan;
    this.sortOrder = sortOrder;
    this.title = title;
    this.description = description;
  }

  void change(int sortOrder, String title, String description) {
    this.sortOrder = sortOrder;
    this.title = title;
    this.description = description;
  }

  /** 화면 번호. 순서대로 "001" 꼴이다. */
  public String getNumber() {
    return String.format("%03d", sortOrder + 1);
  }

  public Long getId() {
    return id;
  }

  public TestPlan getPlan() {
    return plan;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }
}
