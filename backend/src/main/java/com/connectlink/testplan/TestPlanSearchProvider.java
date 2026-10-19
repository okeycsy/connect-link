package com.connectlink.testplan;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.search.SearchCategory;
import com.connectlink.search.SearchProvider;
import com.connectlink.search.dto.SearchHit;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 전역 검색의 "기관 테스트" 묶음. 레일의 업무에서 개발 건 이름과 단위업무로 찾는다. 결제원 담당자는 전부, 기관 담당자는 자기 기관이 대상인 것만 나온다. */
@Component
@Transactional(readOnly = true)
public class TestPlanSearchProvider implements SearchProvider {

  private final TestPlanRepository repository;
  private final TestPlanAccessPolicy accessPolicy;

  public TestPlanSearchProvider(TestPlanRepository repository, TestPlanAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public SearchCategory category() {
    return SearchCategory.TEST_PLAN;
  }

  @Override
  public List<SearchHit> search(CurrentUser user, Business business, String query, int limit) {
    boolean manager = accessPolicy.canManage(user, business);
    String organizationCode;
    if (manager) {
      organizationCode = null;
    } else if (accessPolicy.isOrganizationMember(user, business)) {
      organizationCode = user.organizationCode();
    } else {
      return List.of();
    }
    return repository
        .findAll(
            TestPlanSpecifications.of(business, organizationCode, null)
                .and(TestPlanSpecifications.matching(query)),
            PageRequest.of(0, limit, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
        .stream()
        .map(
            p -> {
              int rate =
                  manager
                      ? p.getProgressRate()
                      : p.findTargetByCode(organizationCode)
                          .map(TestPlanTarget::getProgressRate)
                          .orElse(0);
              return new SearchHit(
                  p.getTitle(),
                  p.getSubBusiness()
                      + " · "
                      + p.getState().getLabel()
                      + " · 수행률 "
                      + rate
                      + "% · "
                      + TestPlanNotifier.periodText(p),
                  TestPlanNotifier.linkOf(p));
            })
        .toList();
  }
}
