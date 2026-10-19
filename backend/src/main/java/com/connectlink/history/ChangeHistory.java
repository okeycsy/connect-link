package com.connectlink.history;

import com.connectlink.common.AuditedEntity;
import com.connectlink.history.dto.FieldChange;
import com.connectlink.organization.Business;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * 변경 이력 한 건. 누가 언제 무엇을 어떻게 바꿨는지와 항목별 변경 전·후 값을 남긴다. 한 번 쓰면 고치지 않는다(감사 컬럼의 created_* 가 변경자·변경 일시다).
 */
@Entity
@Table(name = "change_history")
public class ChangeHistory extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 30)
  private HistoryTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private Long targetId;

  /** 대상이 속한 업무 영역. 기관처럼 업무와 무관한 대상은 비어 있다. */
  @Enumerated(EnumType.STRING)
  @Column(name = "business", length = 30)
  private Business business;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 20)
  private ChangeAction action;

  /** 목록에 한 줄로 보일 요약. 예: "KB국민은행 수정". */
  @Column(name = "summary", nullable = false, length = 200)
  private String summary;

  /** 변경한 사람의 표시 이름. 이메일(created_by)과 함께 보여 준다. */
  @Column(name = "actor_name", nullable = false, length = 100)
  private String actorName;

  @Lob
  @Convert(converter = FieldChangesConverter.class)
  @Column(name = "changes", nullable = false)
  private List<FieldChange> changes = List.of();

  protected ChangeHistory() {}

  public ChangeHistory(
      HistoryTargetType targetType,
      Long targetId,
      Business business,
      ChangeAction action,
      String summary,
      String actorName,
      List<FieldChange> changes) {
    this.targetType = targetType;
    this.targetId = targetId;
    this.business = business;
    this.action = action;
    this.summary = summary;
    this.actorName = actorName;
    this.changes = List.copyOf(changes);
  }

  public Long getId() {
    return id;
  }

  public HistoryTargetType getTargetType() {
    return targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public Business getBusiness() {
    return business;
  }

  public ChangeAction getAction() {
    return action;
  }

  public String getSummary() {
    return summary;
  }

  public String getActorName() {
    return actorName;
  }

  public List<FieldChange> getChanges() {
    return changes;
  }
}
