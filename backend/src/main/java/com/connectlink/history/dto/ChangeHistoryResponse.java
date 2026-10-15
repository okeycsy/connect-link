package com.connectlink.history.dto;

import com.connectlink.history.ChangeAction;
import com.connectlink.history.ChangeHistory;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 변경 이력 한 건(화면용). */
public record ChangeHistoryResponse(
    Long id,
    HistoryTargetType targetType,
    Long targetId,
    @Schema(nullable = true) Business business,
    ChangeAction action,
    String actionLabel,
    String summary,
    List<FieldChange> changes,
    String changedBy,
    String changedByName,
    LocalDateTime changedAt) {

  public static ChangeHistoryResponse from(ChangeHistory history, List<FieldChange> changes) {
    return new ChangeHistoryResponse(
        history.getId(),
        history.getTargetType(),
        history.getTargetId(),
        history.getBusiness(),
        history.getAction(),
        history.getAction().getLabel(),
        history.getSummary(),
        changes,
        history.getCreatedBy(),
        history.getActorName(),
        history.getCreatedAt());
  }
}
