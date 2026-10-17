package com.connectlink.opinion;

import com.connectlink.common.AuditedEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/** 의견 요청의 질문 하나. 객관식이면 선택지({@link OpinionQuestionOption})를 가진다. 회신중이 되면 고치지 않는다. */
@Entity
@Table(name = "opinion_question")
public class OpinionQuestion extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false)
  private OpinionRequest request;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "question_type", nullable = false, length = 20)
  private OpinionQuestionType type;

  @Column(name = "title", nullable = false, length = 500)
  private String title;

  /** 최종제출 때 꼭 답해야 하는지. */
  @Column(name = "required", nullable = false)
  private boolean required;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<OpinionQuestionOption> options = new ArrayList<>();

  protected OpinionQuestion() {}

  OpinionQuestion(
      OpinionRequest request,
      int sortOrder,
      OpinionQuestionType type,
      String title,
      boolean required,
      List<String> options) {
    this.request = request;
    this.sortOrder = sortOrder;
    this.type = type;
    this.title = title;
    this.required = required;
    if (type.isChoice()) {
      for (int i = 0; i < options.size(); i++) {
        this.options.add(new OpinionQuestionOption(this, i, options.get(i)));
      }
    }
  }

  public Optional<OpinionQuestionOption> findOption(Long optionId) {
    return options.stream().filter(o -> o.getId().equals(optionId)).findFirst();
  }

  public Long getId() {
    return id;
  }

  public OpinionRequest getRequest() {
    return request;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public OpinionQuestionType getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public boolean isRequired() {
    return required;
  }

  /** 선택지. 정한 순서다. 주관식이면 비어 있다. */
  public List<OpinionQuestionOption> getOptions() {
    return options.stream()
        .sorted(Comparator.comparingInt(OpinionQuestionOption::getSortOrder))
        .toList();
  }

  /** 질문 하나의 내용(저장 전). 비교해서 바뀌었는지 볼 때도 쓴다. */
  record Draft(OpinionQuestionType type, String title, boolean required, List<String> options) {
    static Draft of(OpinionQuestion question) {
      return new Draft(
          question.getType(),
          question.getTitle(),
          question.isRequired(),
          question.getOptions().stream().map(OpinionQuestionOption::getLabel).toList());
    }
  }
}
