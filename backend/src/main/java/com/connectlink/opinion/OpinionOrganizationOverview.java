package com.connectlink.opinion;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationOverviewContributor;
import com.connectlink.organization.OrganizationOverviewSectionKey;
import com.connectlink.organization.dto.OrganizationOverviewItem;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 한눈에 보기의 "회신을 기다리는 의견 요청" 칸. 그 기관이 대상인 회신중 요청 가운데 아직 제출하지 않았거나 수정 중인 것, 기한이 이른 순이다. 기한이 지났으면 눈에
 * 띄게 보인다. 그 업무 결제원 담당자만 본다.
 */
@Component
@Transactional(readOnly = true)
public class OpinionOrganizationOverview implements OrganizationOverviewContributor {

  private static final Pageable OPEN_REQUESTS =
      PageRequest.of(0, 200, Sort.by(Sort.Order.asc("dueDate"), Sort.Order.desc("id")));

  private final OpinionRequestRepository repository;
  private final OpinionReplyRepository replyRepository;
  private final OpinionAccessPolicy accessPolicy;
  private final Clock clock;

  public OpinionOrganizationOverview(
      OpinionRequestRepository repository,
      OpinionReplyRepository replyRepository,
      OpinionAccessPolicy accessPolicy,
      Clock clock) {
    this.repository = repository;
    this.replyRepository = replyRepository;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public OrganizationOverviewSectionKey key() {
    return OrganizationOverviewSectionKey.OPINION_REQUEST;
  }

  @Override
  public List<OrganizationOverviewItem> contribute(
      CurrentUser user, Business business, Organization organization) {
    if (!accessPolicy.canManage(user, business)) {
      return null;
    }
    LocalDate today = LocalDate.now(clock);
    List<OrganizationOverviewItem> items = new ArrayList<>();
    for (OpinionRequest request :
        repository
            .findTargeted(
                business, OpinionRequestStatus.OPEN, organization.getCode(), OPEN_REQUESTS)
            .getContent()) {
      Optional<OpinionReply> reply =
          request
              .findTarget(organization.getId())
              .flatMap(t -> replyRepository.findByTarget_Id(t.getId()));
      boolean waiting =
          reply.isEmpty()
              || !reply.get().isReplied()
              || reply.get().getStatus() == OpinionReplyStatus.REVISING;
      if (!waiting) {
        continue;
      }
      boolean overdue = request.getDueDate().isBefore(today);
      items.add(
          OrganizationOverviewItem.of(
              request.getTitle(),
              "기한 "
                  + request.getDueDate()
                  + (overdue ? " (지남)" : "")
                  + " · "
                  + request.getOwnerName()
                  + " 요청",
              reply.map(r -> r.getStatus().getLabel()).orElse("미작성"),
              overdue,
              OpinionNotifier.linkOf(request)));
    }
    return items;
  }
}
