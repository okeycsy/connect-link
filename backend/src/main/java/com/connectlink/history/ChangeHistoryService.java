package com.connectlink.history;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.PageResponse;
import com.connectlink.history.dto.ChangeHistoryResponse;
import com.connectlink.history.dto.FieldChange;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

  /**
   * 대상 하나의 이력, 최신순. 대상 모듈이 보여 줄 건수를 제한하면({@link HistoryTargetAccess#maxVisible}) 그 건수까지만 한 쪽으로 준다.
   */
  public PageResponse<ChangeHistoryResponse> list(
      CurrentUser user, HistoryTargetType targetType, Long targetId, Pageable pageable) {
    accessPolicy.ensureCanView(user, targetType, targetId);
    HistoryTargetAccess access = accessPolicy.accessFor(targetType);
    int limit = access.maxVisible(user);
    Page<ChangeHistory> page;
    if (limit == Integer.MAX_VALUE) {
      page =
          repository.findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
              targetType, targetId, pageable);
    } else {
      // 첫 쪽의 앞 limit 건만 있는 한 쪽이다. 전체 건수도 limit 을 넘지 않아 화면이 "더 있음" 을 알 수 없다.
      List<ChangeHistory> visible =
          pageable.getPageNumber() > 0
              ? List.of()
              : repository
                  .findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
                      targetType,
                      targetId,
                      PageRequest.of(0, Math.min(limit, pageable.getPageSize())))
                  .getContent();
      page =
          new PageImpl<>(
              visible,
              PageRequest.of(pageable.getPageNumber(), Math.max(1, limit)),
              visible.size());
    }
    return PageResponse.from(
        page,
        history -> ChangeHistoryResponse.from(history, redact(access, user, history.getChanges())));
  }

  private static List<FieldChange> redact(
      HistoryTargetAccess access, CurrentUser user, List<FieldChange> changes) {
    return changes.stream().map(change -> access.redact(user, change)).toList();
  }
}
