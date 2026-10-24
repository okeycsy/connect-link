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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 연락처 카드. 결제원 담당자에게는 "정기 변경 진행률"(진행 중인 주간의 미확인 기관), 기관 담당자에게는 "연락처 변경·확인 요청"(우리 기관 현행화 미완료와 6개월
 * 넘은 담당자)을 보인다. 할 일이 없으면 홈이 카드를 뺀다. 마감 3일 전부터 임박이다.
 */
@Component
@Transactional(readOnly = true)
public class UpdateCampaignHomeCard implements HomeCardContributor {

  private static final int MAX_ITEMS = 5;
  private static final long SOON_DAYS = 3;

  private final UpdateCampaignService campaignService;
  private final ContactRepository contactRepository;
  private final ContactAccessPolicy accessPolicy;
  private final Clock clock;

  public UpdateCampaignHomeCard(
      UpdateCampaignService campaignService,
      ContactRepository contactRepository,
      ContactAccessPolicy accessPolicy,
      Clock clock) {
    this.campaignService = campaignService;
    this.contactRepository = contactRepository;
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
                        campaignLink,
                        o.organizationCode()))
            .toList();
    return HomeContribution.of(
        card(
            urgency,
            "미완료 " + (campaign.targetCount() - campaign.completedCount()) + "곳 · " + remaining(end),
            items,
            campaignLink));
  }

  /**
   * 기관 담당자: 결제원이 요청한 연락처 일을 한 카드에 모은다. 정기 변경 주간에 우리 기관이 "현행화 완료" 를 아직 누르지 않았으면 그 한 줄, 최근 확인일이 6개월을
   * 넘긴 우리 기관 담당자마다 한 줄이다. SHY 요청(2026-10-24).
   */
  private HomeContribution forMember(CurrentUser user, Business business) {
    String link = "/" + business.getSlug() + "/contacts";
    List<HomeCardItem> items = new ArrayList<>();
    List<String> summary = new ArrayList<>();
    CurrentUpdateCampaignResponse current = campaignService.current(user, business);
    if (current.campaign() != null
        && current.ownOrganization() != null
        && !current.ownOrganization().completed()) {
      UpdateCampaignResponse campaign = current.campaign();
      items.add(
          new HomeCardItem(
              campaign.title() + " · 현행화 미완료",
              "담당자를 확인하고 \"현행화 완료\" 를 눌러 주세요",
              campaign.endDate(),
              urgencyOf(campaign.endDate()),
              link));
      summary.add("정기 변경 " + remaining(campaign.endDate()));
    }
    String code = user.organizationCode();
    Specification<Contact> overdue =
        ContactSpecifications.scope(business, code)
            .and((root, query, cb) -> cb.equal(root.get("organization").get("code"), code))
            .and(
                ContactSpecifications.filter(
                    null,
                    null,
                    null,
                    LocalDateTime.now(clock).minus(ContactService.RECONFIRM_AFTER)));
    long overdueCount = contactRepository.count(overdue);
    if (overdueCount > 0) {
      contactRepository
          .findAll(
              overdue,
              PageRequest.of(
                  0, MAX_ITEMS, Sort.by(Sort.Order.asc("lastConfirmedAt"), Sort.Order.asc("id"))))
          .forEach(
              c ->
                  items.add(
                      new HomeCardItem(
                          c.getName() + " (" + c.jobRoleLabel() + ") · 확인 필요",
                          "최근 확인 " + c.getLastConfirmedAt().toLocalDate(),
                          null,
                          Urgency.OVERDUE,
                          link + "?id=" + c.getId())));
      summary.add("확인 필요 " + overdueCount + "명");
    }
    return HomeContribution.of(
        new HomeCard(
            key(),
            "연락처 변경·확인 요청",
            HomeCardSource.REAL,
            Urgency.NONE,
            String.join(" · ", summary),
            items,
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
