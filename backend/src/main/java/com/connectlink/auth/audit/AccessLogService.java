package com.connectlink.auth.audit;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.audit.dto.AccessLogResponse;
import com.connectlink.common.NetworkZoneResolver;
import com.connectlink.common.PageResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인정보 처리 접속기록(개인정보의 안전성 확보조치 기준 제8조). 남기기는 {@link AccessLogFilter} 가 하고, 보기는 관리자만 내부망에서 한다(점검용).
 * 보관 기간({@code app.audit.access-log.retention})이 지난 기록은 {@link AccessLogCleanupJob} 이 지운다.
 */
@Service
@Transactional(readOnly = true)
public class AccessLogService {

  private final AccessLogRepository repository;
  private final AccessLogProperties properties;
  private final NetworkZoneResolver networkZoneResolver;
  private final Clock clock;

  public AccessLogService(
      AccessLogRepository repository,
      AccessLogProperties properties,
      NetworkZoneResolver networkZoneResolver,
      Clock clock) {
    this.repository = repository;
    this.properties = properties;
    this.networkZoneResolver = networkZoneResolver;
    this.clock = clock;
  }

  /** 기록 한 건. 요청 처리와 따로 저장해, 요청이 실패해도(거부된 시도도) 남는다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
      CurrentUser user,
      String ipAddress,
      AccessLogRule rule,
      String method,
      String path,
      String query,
      int status) {
    repository.save(
        new AccessLog(
            now(),
            user.email(),
            user.name(),
            user.organizationName(),
            ipAddress == null ? "-" : ipAddress,
            rule.action(),
            rule.targetOf(path, query),
            method,
            truncate(query == null || query.isBlank() ? path : path + "?" + query, 500),
            status));
  }

  /**
   * 접속기록 목록(최근 것부터). 관리자만, 내부망에서만 본다(403). 날짜는 그날 하루 전체를 포함한다.
   *
   * @param email 계정 이메일 일부(대소문자 무시). 비우면 전부
   */
  public PageResponse<AccessLogResponse> list(
      CurrentUser user, LocalDate from, LocalDate to, String email, int page, int size) {
    if (!user.admin()) {
      throw new AccessDeniedException("접속기록은 관리자만 볼 수 있습니다.");
    }
    networkZoneResolver.ensureInternal("접속기록 조회");
    Specification<AccessLog> spec =
        (root, query, cb) -> {
          List<Predicate> where = new ArrayList<>();
          if (from != null) {
            where.add(cb.greaterThanOrEqualTo(root.get("accessedAt"), from.atStartOfDay()));
          }
          if (to != null) {
            where.add(cb.lessThan(root.get("accessedAt"), to.plusDays(1).atStartOfDay()));
          }
          if (email != null && !email.isBlank()) {
            where.add(
                cb.like(
                    cb.lower(root.get("accountEmail")),
                    "%" + email.trim().toLowerCase(Locale.ROOT) + "%"));
          }
          return cb.and(where.toArray(Predicate[]::new));
        };
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Order.desc("accessedAt"), Sort.Order.desc("id")));
    return PageResponse.from(repository.findAll(spec, pageable), AccessLogResponse::from);
  }

  /** 보관 기간이 지난 기록을 지운다. 지운 건수. */
  @Transactional
  public int deleteExpired() {
    return repository.deleteOlderThan(now().minus(properties.getRetention()));
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  private static String truncate(String value, int max) {
    return value.length() > max ? value.substring(0, max) : value;
  }
}
