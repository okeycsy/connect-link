package com.connectlink.account;

import com.connectlink.account.dto.SignUpApplicationResponse;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.PageResponse;
import com.connectlink.home.HomeCardContributor;
import com.connectlink.home.HomeCardKey;
import com.connectlink.home.HomeCardSource;
import com.connectlink.home.HomeContribution;
import com.connectlink.home.Urgency;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeCard.HomeCardItem;
import com.connectlink.organization.Business;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** 홈 "가입 승인 대기" 카드. 그 업무의 신청을 처리할 수 있는 사람(결제원 담당자, 관리자)에게만 보이고, 최근 신청 세 건을 보여 준다. */
@Component
public class SignUpApprovalHomeCard implements HomeCardContributor {

  private static final int ITEM_COUNT = 3;

  private final SignUpReviewService reviewService;
  private final SignUpReviewAccessPolicy accessPolicy;

  public SignUpApprovalHomeCard(
      SignUpReviewService reviewService, SignUpReviewAccessPolicy accessPolicy) {
    this.reviewService = reviewService;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public HomeCardKey key() {
    return HomeCardKey.SIGNUP_APPROVAL;
  }

  @Override
  public HomeContribution contribute(CurrentUser user, Business business) {
    if (!accessPolicy.canReview(user, business)) {
      return null;
    }
    PageResponse<SignUpApplicationResponse> pending =
        reviewService.list(
            user, business, AccountStatus.PENDING_APPROVAL, PageRequest.of(0, ITEM_COUNT));
    String link = "/" + business.getSlug() + "/" + key().getFeature();
    List<HomeCardItem> items =
        pending.content().stream()
            .map(
                a ->
                    new HomeCardItem(
                        a.organizationName() + " · " + a.name(),
                        a.jobRoleLabel() + " · " + (a.emailDomainMatches() ? "도메인 일치" : "도메인 다름"),
                        a.appliedAt().toLocalDate(),
                        Urgency.NONE,
                        SignUpReviewService.linkOf(business, a.id())))
            .toList();
    return HomeContribution.of(
        new HomeCard(
            key(),
            key().getTitle(),
            HomeCardSource.REAL,
            Urgency.NONE,
            pending.totalElements() > 0
                ? "승인 대기 " + pending.totalElements() + "건"
                : "기다리는 가입 신청 없음",
            items,
            link));
  }
}
