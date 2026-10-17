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

/** 객관식 질문의 선택지 하나. */
@Entity
@Table(name = "opinion_question_option")
public class OpinionQuestionOption extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private OpinionQuestion question;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "label", nullable = false, length = 200)
  private String label;

  protected OpinionQuestionOption() {}

  OpinionQuestionOption(OpinionQuestion question, int sortOrder, String label) {
    this.question = question;
    this.sortOrder = sortOrder;
    this.label = label;
  }

  public Long getId() {
    return id;
  }

  public OpinionQuestion getQuestion() {
    return question;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getLabel() {
    return label;
  }
}
