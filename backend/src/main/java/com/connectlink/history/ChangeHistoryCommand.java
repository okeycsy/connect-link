package com.connectlink.history;

import com.connectlink.history.dto.FieldChange;
import com.connectlink.organization.Business;
import java.util.List;

/** 이력 한 건을 남겨 달라는 요청. 기능 모듈이 {@link ChangeHistoryService#record} 에 넘긴다. */
public record ChangeHistoryCommand(
    HistoryTargetType targetType,
    Long targetId,
    Business business,
    ChangeAction action,
    String summary,
    List<FieldChange> changes) {

  public static ChangeHistoryCommand created(
      HistoryTargetType targetType,
      Long targetId,
      Business business,
      String summary,
      List<FieldChange> changes) {
    return new ChangeHistoryCommand(
        targetType, targetId, business, ChangeAction.CREATED, summary, changes);
  }

  public static ChangeHistoryCommand updated(
      HistoryTargetType targetType,
      Long targetId,
      Business business,
      String summary,
      List<FieldChange> changes) {
    return new ChangeHistoryCommand(
        targetType, targetId, business, ChangeAction.UPDATED, summary, changes);
  }

  public static ChangeHistoryCommand deleted(
      HistoryTargetType targetType,
      Long targetId,
      Business business,
      String summary,
      List<FieldChange> changes) {
    return new ChangeHistoryCommand(
        targetType, targetId, business, ChangeAction.DELETED, summary, changes);
  }
}
