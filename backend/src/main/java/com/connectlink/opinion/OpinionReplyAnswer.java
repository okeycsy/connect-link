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

/**
 * 기관 회신의 답 한 칸. 주관식은 질문마다 한 행({@code textValue}), 객관식은 고른 선택지마다 한 행({@code optionId})이다. 회신처럼 제출한
 * 답과 임시저장한 답을 {@link Stage} 로 나눈다.
 */
@Entity
@Table(name = "opinion_reply_answer")
public class OpinionReplyAnswer extends AuditedEntity {

  /** 제출한 답인지 임시저장한 답인지. */
  public enum Stage {
    SUBMITTED,
    DRAFT
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reply_id", nullable = false)
  private OpinionReply reply;

  @Column(name = "question_id", nullable = false)
  private Long questionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "stage", nullable = false, length = 20)
  private Stage stage;

  /** 고른 선택지. 주관식이면 null. */
  @Column(name = "option_id")
  private Long optionId;

  /** 주관식 답. 객관식이면 null. */
  @Column(name = "text_value", length = 4000)
  private String textValue;

  protected OpinionReplyAnswer() {}

  OpinionReplyAnswer(OpinionReply reply, Stage stage, Long questionId, Long optionId, String text) {
    this.reply = reply;
    this.stage = stage;
    this.questionId = questionId;
    this.optionId = optionId;
    this.textValue = text;
  }

  void changeText(String text) {
    this.textValue = text;
  }

  public Long getId() {
    return id;
  }

  public Long getQuestionId() {
    return questionId;
  }

  public Stage getStage() {
    return stage;
  }

  public Long getOptionId() {
    return optionId;
  }

  public String getTextValue() {
    return textValue;
  }
}
