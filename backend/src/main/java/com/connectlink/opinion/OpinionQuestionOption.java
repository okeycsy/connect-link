package com.connectlink.opinion;

import com.connectlink.common.AuditedEntity;
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

/** 객관식 질문의 선택지 하나. 하나 고르기 질문이면 이 선택지를 골랐을 때 갈 섹션(이동)을 정할 수 있다. */
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

  /** 이 선택지를 고르면 갈 섹션. 비어 있으면 섹션의 이동을 따른다. */
  @Enumerated(EnumType.STRING)
  @Column(name = "jump_type", length = 10)
  private OpinionJumpType jumpType;

  /** 이동할 섹션의 순서(0부터). {@link OpinionJumpType#SECTION} 일 때만 있다. */
  @Column(name = "jump_order")
  private Integer jumpOrder;

  protected OpinionQuestionOption() {}

  OpinionQuestionOption(
      OpinionQuestion question, int sortOrder, OpinionQuestion.OptionDraft draft) {
    this.question = question;
    this.sortOrder = sortOrder;
    this.label = draft.label();
    if (draft.jump() != null) {
      this.jumpType = draft.jump().type();
      this.jumpOrder = draft.jump().order();
    }
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

  /** 이 선택지의 이동. 정하지 않았으면 null. */
  public OpinionQuestion.Jump getJump() {
    return OpinionQuestion.Jump.of(jumpType, jumpOrder);
  }
}
