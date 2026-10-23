package com.connectlink.suspension;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.organization.Business;
import com.connectlink.suspension.dto.SuspensionBusinessResponse;
import com.connectlink.suspension.dto.TargetWorkResponse;
import com.connectlink.suspension.dto.TargetWorkSaveRequest;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 일시중단 대상 업무 기준정보. 로그인한 사람은 사용 중 목록을 보고, 관리자는 전부를 보고 더하고 고친다. */
@Service
@Transactional
public class TargetWorkService {

  private final TargetWorkRepository repository;
  private final SuspensionAccessPolicy accessPolicy;

  public TargetWorkService(TargetWorkRepository repository, SuspensionAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  /** 순서, 이름순. {@code includeInactive} 는 관리자만 쓴다. */
  @Transactional(readOnly = true)
  public List<TargetWorkResponse> list(CurrentUser user, boolean includeInactive) {
    if (includeInactive) {
      ensureCanManage(user);
    }
    return repository.findAllByOrderBySortOrderAscNameAsc().stream()
        .filter(w -> includeInactive || w.isActive())
        .map(TargetWorkService::toResponse)
        .toList();
  }

  public TargetWorkResponse create(CurrentUser user, TargetWorkSaveRequest body) {
    ensureCanManage(user);
    String name = body.name().trim();
    if (repository.existsByName(name)) {
      throw duplicate();
    }
    TargetWork work = new TargetWork(name, blankToNull(body.category()), body.sortOrder());
    work.change(name, blankToNull(body.category()), body.sortOrder(), body.active());
    work.replaceBusinesses(businessesOf(body.businesses()));
    return toResponse(repository.saveAndFlush(work));
  }

  public TargetWorkResponse update(CurrentUser user, Long id, TargetWorkSaveRequest body) {
    ensureCanManage(user);
    TargetWork work =
        repository
            .findById(id)
            .orElseThrow(
                () -> BusinessException.notFound("TARGET_WORK_NOT_FOUND", "대상 업무를 찾을 수 없습니다."));
    String name = body.name().trim();
    if (repository.existsByNameAndIdNot(name, id)) {
      throw duplicate();
    }
    work.change(name, blankToNull(body.category()), body.sortOrder(), body.active());
    work.replaceBusinesses(businessesOf(body.businesses()));
    repository.flush();
    return toResponse(work);
  }

  /**
   * 고른 대상 업무를 고른 순서와 상관없이 기준정보 순서로 불러온다. 겹치면 하나로 친다. 없는 업무나 새로 고를 수 없는(사용 안 함) 업무가 있으면 대상 업무 칸에
   * 알린다. {@code kept} 는 이미 고른 업무라 사용 안 함이어도 그대로 둘 수 있는 것이다.
   */
  @Transactional(readOnly = true)
  public List<TargetWork> loadSelectable(Collection<Long> ids, Collection<Long> kept) {
    Map<Long, TargetWork> found = new LinkedHashMap<>();
    repository.findAllById(ids).forEach(w -> found.put(w.getId(), w));
    for (Long id : ids) {
      TargetWork work = found.get(id);
      if (work == null || (!work.isActive() && !kept.contains(id))) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST,
            "SUSPENSION_TARGET_WORK_INVALID",
            "targetWorkIds",
            "고를 수 없는 대상 업무가 있습니다. 목록을 새로 불러 다시 골라 주세요.");
      }
    }
    return found.values().stream()
        .sorted(
            Comparator.comparingInt(TargetWork::getSortOrder).thenComparing(TargetWork::getName))
        .toList();
  }

  static TargetWorkResponse toResponse(TargetWork w) {
    return new TargetWorkResponse(
        w.getId(),
        w.getName(),
        w.getCategory(),
        w.getSortOrder(),
        w.isActive(),
        w.getBusinesses().stream().map(SuspensionBusinessResponse::of).toList());
  }

  private void ensureCanManage(CurrentUser user) {
    if (!accessPolicy.canManageTargetWorks(user)) {
      throw new AccessDeniedException(SuspensionAccessPolicy.TARGET_WORK_DENIED);
    }
  }

  private static EnumSet<Business> businessesOf(List<Business> businesses) {
    return businesses.isEmpty() ? EnumSet.noneOf(Business.class) : EnumSet.copyOf(businesses);
  }

  private static BusinessException duplicate() {
    return BusinessException.onField(
        HttpStatus.CONFLICT, "TARGET_WORK_NAME_DUPLICATE", "name", "같은 이름의 대상 업무가 있습니다.");
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
