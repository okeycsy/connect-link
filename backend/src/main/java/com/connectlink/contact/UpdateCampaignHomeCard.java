package com.connectlink.contact;

import com.connectlink.auth.CurrentUser;
import com.connectlink.contact.dto.CurrentUpdateCampaignResponse;
import com.connectlink.contact.dto.UpdateCampaignOrganizationResponse;
import com.connectlink.contact.dto.UpdateCampaignProgressResponse;
import com.connectlink.contact.dto.UpdateCampaignResponse;
import com.connectlink.home.HomeCardContributor;
import com.connectlink.home.HomeCardKey;
import com.connectlink.home.HomeCardSource;
import com.connectlink.home.HomeContribution;
import com.connectlink.home.Urgency;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeCard.HomeCardItem;
import com.connectlink.organization.Business;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 "정기 변경 진행률" 카드. 결제원 담당자에게는 진행 중인 주간의 기관 진행률과 미확인 기관을, 기관 담당자에게는 자기 기관의 현행화 완료 여부를 보인다. 진행 중인
 * 주간이 없으면 결제원 담당자에게는 빈 카드, 기관 담당자에게는 카드가 없다. 마감 3일 전부터 임박이다.
 */
@Component
@Transactional(readOnly = true)
public class UpdateCampaignHomeCard implements HomeCardContributor {

  private static final int MAX_ITEMS = 5;
  private static final long SOON_DAYS = 3;

  private final UpdateCampaignService campaignService;
  private final ContactAccessPolicy accessPolicy;
  private final Clock clock;

  public UpdateCampaignHomeCard(
      UpdateCampaignService campaignService, ContactAccessPolicy accessPolicy, Clock clock) {
    this.campaignService = campaignService;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public HomeCardKey key() {
    return HomeCardKey.UPDATE_CAMPAIGN;
  }

  @Override
  public HomeContribution contribute(CurrentUser user, Business business) {
    if (accessPolicy.canManage(user, business)) {
      return forManager(business);
    }
    if (accessPolicy.isOrganizationMember(user, business)) {
      return forMember(user, business);
    }
    return null;
  }

  private HomeContribution forManager(Business business) {
    String adminLink = "/" + business.getSlug() + "/contacts/update-campaigns";
    Optional<UpdateCampaignProgressResponse> open = campaignService.openProgress(business);
    if (open.isEmpty()) {
      return HomeContribution.of(card(Urgency.NONE, "진행 중인 정기 변경 주간 없음", List.of(), adminLink));
    }
    UpdateCampaignResponse campaign = open.get().campaign();
    LocalDate end = campaign.endDate();
    Urgency urgency = urgencyOf(end);
    String campaignLink = adminLink + "?id=" + campaign.id();
    List<HomeCardItem> items =
        open.get().organizations().stream()
            .filter(o -> !o.completed())
            .sorted(
                java.util.Comparator.comparing(
                    UpdateCampaignOrganizationResponse::organizationName))
            .limit(MAX_ITEMS)
            .map(
                o ->
                    new HomeCardItem(
                        o.organizationName() + " · 미확인",
                        o.changed()
                            ? "정보 변경 있음 · 담당자 " + o.contactCount() + "명"
                            : "담당자 " + o.contactCount() + "명",
                        end,
                        urgency,
                        campaignLink))
            .toList();
    return HomeContribution.of(
        card(
            urgency,
            "미완료 " + (campaign.targetCount() - campaign.completedCount()) + "곳 · " + remaining(end),
            items,
            campaignLink));
  }

  private HomeContribution forMember(CurrentUser user, Business business) {
    CurrentUpdateCampaignResponse current = campaignService.current(user, business);
    if (current.campaign() == null || current.ownOrganization() == null) {
      return null;
    }
    UpdateCampaignResponse campaign = current.campaign();
    UpdateCampaignOrganizationResponse own = current.ownOrganization();
    String link = "/" + business.getSlug() + "/contacts";
    String summary =
        own.completed() ? "현행화 완료" : "담당자를 확인하고 현행화 완료를 눌러 주세요 · " + remaining(campaign.endDate());
    Urgency urgency = own.completed() ? Urgency.NONE : urgencyOf(campaign.endDate());
    return HomeContribution.of(
        card(
            urgency,
            summary,
            own.completed()
                ? List.of()
                : List.of(
                    new HomeCardItem(
                        user.organizationName() + " · 현행화 미완료",
                        campaign.startDate() + " ~ " + campaign.endDate(),
                        campaign.endDate(),
                        urgency,
                        link)),
            link));
  }

  private HomeCard card(Urgency urgency, String summary, List<HomeCardItem> items, String link) {
    return new HomeCard(
        key(), key().getTitle(), HomeCardSource.REAL, urgency, summary, items, link);
  }

  private Urgency urgencyOf(LocalDate end) {
    return ChronoUnit.DAYS.between(LocalDate.now(clock), end) <= SOON_DAYS
        ? Urgency.SOON
        : Urgency.NONE;
  }

  private String remaining(LocalDate end) {
    long days = ChronoUnit.DAYS.between(LocalDate.now(clock), end);
    return days == 0 ? "오늘 마감" : "마감까지 " + days + "일";
  }
}
