package com.connectlink.opinion;

import com.connectlink.common.AuditedEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

/**
 * 의견 요청의 질문 하나. 섹션({@link OpinionSection})에 묶이고, 객관식이면 선택지({@link OpinionQuestionOption})를 가진다.
 * 객관식(하나 고르기)은 선택지마다 고르면 갈 섹션을 정할 수 있다. 회신중이 되면 고치지 않는다. 순서({@code sortOrder})는 섹션을 넘어 요청 전체에서 센다.
 */
@Entity
@Table(name = "opinion_question")
public class OpinionQuestion extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false)
  private OpinionRequest request;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "section_id", nullable = false)
  private OpinionSection section;

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

  OpinionQuestion(OpinionSection section, int sortOrder, Draft draft) {
    this.request = section.getRequest();
    this.section = section;
    this.sortOrder = sortOrder;
    this.type = draft.type();
    this.title = draft.title();
    this.required = draft.required();
    if (type.isChoice()) {
      for (int i = 0; i < draft.options().size(); i++) {
        this.options.add(new OpinionQuestionOption(this, i, draft.options().get(i)));
      }
    }
  }

  public Optional<OpinionQuestionOption> findOption(Long optionId) {
    return options.stream().filter(o -> o.getId().equals(optionId)).findFirst();
  }

  /** 고른 선택지의 이동. 객관식(하나 고르기)에서 고른 선택지에 이동이 있을 때만 있고, 아니면 null 이다. */
  Jump chosenJump(Set<Long> chosenOptionIds) {
    if (type != OpinionQuestionType.SINGLE_CHOICE) {
      return null;
    }
    return options.stream()
        .filter(o -> chosenOptionIds.contains(o.getId()) && o.getJump() != null)
        .map(OpinionQuestionOption::getJump)
        .findFirst()
        .orElse(null);
  }

  public Long getId() {
    return id;
  }

  public OpinionRequest getRequest() {
    return request;
  }

  public OpinionSection getSection() {
    return section;
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

  /**
   * 섹션 이동 하나.
   *
   * @param order 이동할 섹션의 순서(0부터). {@link OpinionJumpType#END} 이면 null
   */
  public record Jump(OpinionJumpType type, Integer order) {
    static Jump of(OpinionJumpType type, Integer order) {
      return type == null ? null : new Jump(type, type == OpinionJumpType.SECTION ? order : null);
    }
  }

  /**
   * 질문 하나의 내용(저장 전). 비교해서 바뀌었는지 볼 때도 쓴다. 선택지 이동은 하나 고르기에만 있다.
   *
   * @param sectionIndex 묶일 섹션의 순서(0부터)
   */
  record Draft(
      int sectionIndex,
      OpinionQuestionType type,
      String title,
      boolean required,
      List<OptionDraft> options) {
    static Draft of(OpinionQuestion question) {
      return new Draft(
          question.getSection().getSortOrder(),
          question.getType(),
          question.getTitle(),
          question.isRequired(),
          question.getOptions().stream()
              .map(o -> new OptionDraft(o.getLabel(), o.getJump()))
              .toList());
    }
  }

  /** 선택지 하나의 내용(저장 전). */
  record OptionDraft(String label, Jump jump) {}
}
