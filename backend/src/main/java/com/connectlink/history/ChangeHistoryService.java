package com.connectlink.history;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.PageResponse;
import com.connectlink.history.dto.ChangeHistoryResponse;
import com.connectlink.history.dto.FieldChange;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 변경 이력. 기능 모듈은 자기 변경 트랜잭션 안에서 {@link #record} 를 불러 남기고, 화면은 대상별로 최신순 목록을 읽는다. 변경 전·후 값의 마스킹은 대상
 * 모듈의 {@link HistoryTargetAccess#redact} 가 맡는다.
 */
@Service
@Transactional(readOnly = true)
public class ChangeHistoryService {

  private final ChangeHistoryRepository repository;
  private final ChangeHistoryAccessPolicy accessPolicy;

  public ChangeHistoryService(
      ChangeHistoryRepository repository, ChangeHistoryAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  /** 이력을 남긴다. 호출한 쪽의 트랜잭션에 참여하므로 본 변경이 롤백되면 이력도 함께 사라진다. 바뀐 항목이 없는 수정은 남기지 않는다. */
  @Transactional
  public void record(CurrentUser user, ChangeHistoryCommand command) {
    if (command.action() == ChangeAction.UPDATED && command.changes().isEmpty()) {
      return;
    }
    repository.save(
        new ChangeHistory(
            command.targetType(),
            command.targetId(),
            command.business(),
            command.action(),
            command.summary(),
            user.name(),
            command.changes()));
  }

  /** 대상 하나의 이력, 최신순. */
  public PageResponse<ChangeHistoryResponse> list(
      CurrentUser user, HistoryTargetType targetType, Long targetId, Pageable pageable) {
    accessPolicy.ensureCanView(user, targetType, targetId);
    HistoryTargetAccess access = accessPolicy.accessFor(targetType);
    return PageResponse.from(
        repository.findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
            targetType, targetId, pageable),
        history -> ChangeHistoryResponse.from(history, redact(access, user, history.getChanges())));
  }

  private static List<FieldChange> redact(
      HistoryTargetAccess access, CurrentUser user, List<FieldChange> changes) {
    return changes.stream().map(change -> access.redact(user, change)).toList();
  }
}
