package com.connectlink.opinion;

import com.connectlink.common.AuditedEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
 * 의견 요청의 섹션 하나. 구글 폼의 섹션처럼 질문을 묶고, 이 섹션 다음에 갈 섹션(이동)을 정한다. 섹션 안의 객관식(하나 고르기) 답에 이동이 있으면 그것이
 * 먼저다({@link OpinionQuestionPath}). 요청에는 섹션이 하나 이상 있고, 회신중이 되면 고치지 않는다.
 */
@Entity
@Table(name = "opinion_section")
public class OpinionSection extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false)
  private OpinionRequest request;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  /** 섹션 제목. 섹션이 하나뿐이면 보통 비운다. */
  @Column(name = "title", length = 200)
  private String title;

  /** 섹션 설명. */
  @Column(name = "description", length = 2000)
  private String description;

  /** 이 섹션 다음 이동. 비어 있으면 바로 다음 섹션이다. */
  @Enumerated(EnumType.STRING)
  @Column(name = "jump_type", length = 10)
  private OpinionJumpType jumpType;

  /** 이동할 섹션의 순서(0부터). {@link OpinionJumpType#SECTION} 일 때만 있다. */
  @Column(name = "jump_order")
  private Integer jumpOrder;

  @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<OpinionQuestion> questions = new ArrayList<>();

  protected OpinionSection() {}

  OpinionSection(OpinionRequest request, int sortOrder, Draft draft) {
    this.request = request;
    this.sortOrder = sortOrder;
    this.title = draft.title();
    this.description = draft.description();
    if (draft.jump() != null) {
      this.jumpType = draft.jump().type();
      this.jumpOrder = draft.jump().order();
    }
  }

  OpinionQuestion addQuestion(int sortOrder, OpinionQuestion.Draft draft) {
    OpinionQuestion question = new OpinionQuestion(this, sortOrder, draft);
    questions.add(question);
    return question;
  }

  /**
   * 이 섹션을 마치고 갈 곳. 섹션 안 객관식(하나 고르기)에서 고른 선택지에 이동이 있으면 그것(여럿이면 뒤의 질문, 구글 폼과 같다), 없으면 섹션의 이동이다. null
   * 이면 바로 다음 섹션.
   *
   * @param chosen 질문 ID 별 고른 선택지 ID
   */
  OpinionQuestion.Jump nextJump(Map<Long, Set<Long>> chosen) {
    OpinionQuestion.Jump jump = null;
    for (OpinionQuestion question : getQuestions()) {
      OpinionQuestion.Jump picked =
          question.chosenJump(chosen.getOrDefault(question.getId(), Set.of()));
      if (picked != null) {
        jump = picked;
      }
    }
    return jump != null ? jump : getJump();
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

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  /** 이 섹션 다음 이동. 정하지 않았으면 null(바로 다음 섹션). */
  public OpinionQuestion.Jump getJump() {
    return OpinionQuestion.Jump.of(jumpType, jumpOrder);
  }

  /** 섹션의 질문. 정한 순서다. */
  public List<OpinionQuestion> getQuestions() {
    return questions.stream()
        .sorted(Comparator.comparingInt(OpinionQuestion::getSortOrder))
        .toList();
  }

  /** 섹션 하나의 내용(저장 전). 비교해서 바뀌었는지 볼 때도 쓴다. */
  record Draft(String title, String description, OpinionQuestion.Jump jump) {
    static Draft of(OpinionSection section) {
      return new Draft(section.getTitle(), section.getDescription(), section.getJump());
    }
  }
}
