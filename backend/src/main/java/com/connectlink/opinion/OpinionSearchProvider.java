package com.connectlink.opinion;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.search.SearchCategory;
import com.connectlink.search.SearchProvider;
import com.connectlink.search.dto.SearchHit;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 전역 검색의 "의견 요청" 묶음. 레일의 업무에서 요청명으로 찾는다. 결제원 담당자는 작성중까지, 기관 담당자는 자기 기관이 대상인 회신중·마감 요청만 나온다. */
@Component
@Transactional(readOnly = true)
public class OpinionSearchProvider implements SearchProvider {

  /** 볼 수 없는 요청을 거른 뒤에도 limit 을 채우도록 넉넉히 읽는다. */
  private static final int READ_FACTOR = 5;

  private final OpinionRequestRepository repository;
  private final OpinionAccessPolicy accessPolicy;
  private final Clock clock;

  public OpinionSearchProvider(
      OpinionRequestRepository repository, OpinionAccessPolicy accessPolicy, Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public SearchCategory category() {
    return SearchCategory.OPINION_REQUEST;
  }

  @Override
  public List<SearchHit> search(CurrentUser user, Business business, String query, int limit) {
    EnumSet<OpinionRequestStatus> statuses;
    if (accessPolicy.canManage(user, business)) {
      statuses = EnumSet.allOf(OpinionRequestStatus.class);
    } else if (accessPolicy.isReplier(user, business)) {
      statuses = EnumSet.of(OpinionRequestStatus.OPEN, OpinionRequestStatus.CLOSED);
    } else {
      return List.of();
    }
    LocalDate today = LocalDate.now(clock);
    return repository
        .searchByTitle(business, statuses, query.trim(), PageRequest.of(0, limit * READ_FACTOR))
        .stream()
        .filter(r -> accessPolicy.canView(user, r))
        .limit(limit)
        .map(r -> new SearchHit(r.getTitle(), subtitle(r, today), OpinionNotifier.linkOf(r)))
        .toList();
  }

  private static String subtitle(OpinionRequest request, LocalDate today) {
    if (request.getStatus() != OpinionRequestStatus.OPEN) {
      return request.isCancelled() ? "마감(취소)" : request.getStatus().getLabel();
    }
    long daysLeft = ChronoUnit.DAYS.between(today, request.getDueDate());
    String due = daysLeft > 0 ? daysLeft + "일 남음" : daysLeft == 0 ? "오늘 기한" : (-daysLeft) + "일 지남";
    return "회신중 · 회신 기한 " + request.getDueDate() + " (" + due + ")";
  }
}
