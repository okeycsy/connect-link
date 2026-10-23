package com.connectlink.suspension;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.search.SearchCategory;
import com.connectlink.search.SearchProvider;
import com.connectlink.search.dto.SearchHit;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 전역 검색의 "일시중단" 묶음. 레일의 업무에서 기관 이름, 중단 이유로 찾는다. 결제원 담당자는 전 기관, 기관 담당자는 자기 기관 것만 나온다. */
@Component
@Transactional(readOnly = true)
public class SuspensionSearchProvider implements SearchProvider {

  private final SuspensionRepository repository;
  private final SuspensionAccessPolicy accessPolicy;
  private final Clock clock;

  public SuspensionSearchProvider(
      SuspensionRepository repository, SuspensionAccessPolicy accessPolicy, Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public SearchCategory category() {
    return SearchCategory.SUSPENSION;
  }

  @Override
  public List<SearchHit> search(CurrentUser user, Business business, String query, int limit) {
    String organizationCode;
    if (accessPolicy.canManage(user, business)) {
      organizationCode = null;
    } else if (accessPolicy.isOrganizationMember(user, business)) {
      organizationCode = user.organizationCode();
    } else {
      return List.of();
    }
    LocalDateTime now = LocalDateTime.now(clock);
    return repository
        .findAll(
            SuspensionSpecifications.of(
                    business,
                    organizationCode,
                    null,
                    null,
                    now,
                    organizationCode != null || accessPolicy.handlesUnlinked(user))
                .and(SuspensionSpecifications.matching(query)),
            PageRequest.of(0, limit, Sort.by(Sort.Order.desc("startAt"), Sort.Order.desc("id"))))
        .stream()
        .map(
            s ->
                new SearchHit(
                    s.getOrganization().getName()
                        + " 일시중단 · "
                        + s.targetWorkNames()
                        + " · "
                        + s.getReason(),
                    s.periodAt(now).getLabel()
                        + " · "
                        + s.getStatus().getLabel()
                        + " · "
                        + SuspensionNotifier.periodText(s.getStartAt(), s.getEndAt()),
                    SuspensionNotifier.linkOf(s, business)))
        .toList();
  }
}
