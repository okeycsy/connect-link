package com.connectlink.opinion;

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

/**
 * 업권별 회신 양식. 요청마다 이름(예: 은행권, 증권·보험권)과 안내를 정하고, 양식 파일은 첨부({@code OPINION_REPLY_FORM})로 붙인다. 대상 기관마다
 * 양식을 하나 고르면 그 기관 담당자는 자기 양식만 내려받는다.
 */
@Entity
@Table(name = "opinion_reply_form")
public class OpinionReplyForm extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false)
  private OpinionRequest request;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  /** 작성 안내. 없으면 null. */
  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected OpinionReplyForm() {}

  OpinionReplyForm(OpinionRequest request, String name, String description, int sortOrder) {
    this.request = request;
    this.name = name;
    this.description = description;
    this.sortOrder = sortOrder;
  }

  void change(String name, String description, int sortOrder) {
    this.name = name;
    this.description = description;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public OpinionRequest getRequest() {
    return request;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getSortOrder() {
    return sortOrder;
  }
}
