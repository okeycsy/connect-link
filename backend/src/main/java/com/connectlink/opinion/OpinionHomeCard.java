package com.connectlink.opinion;

import com.connectlink.auth.CurrentUser;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 "회신 대기 의견 요청" 카드. 결제원 담당자에게는 그 업무의 회신중 요청과 회신율을, 기관 담당자에게는 자기 기관이 아직 제출하지 않은 회신중 요청을 기한이 가까운
 * 순으로 보인다. 기한이 3일 안이면 임박, 지났으면 초과다. 그 밖의 사용자(이용기관)에게는 카드가 없다.
 */
@Component
@Transactional(readOnly = true)
public class OpinionHomeCard implements HomeCardContributor {

  private static final int ITEM_COUNT = 3;
  private static final int SOON_DAYS = 3;

  /** 카드는 회신중 요청을 이만큼까지 센다. 한 업무에 동시에 열린 요청이 이보다 많을 일은 드물다. */
  private static final Pageable OPEN_REQUESTS =
      PageRequest.of(0, 200, Sort.by(Sort.Order.asc("dueDate"), Sort.Order.desc("id")));

  private final OpinionRequestRepository repository;
  private final OpinionReplyRepository replyRepository;
  private final OpinionAccessPolicy accessPolicy;
  private final Clock clock;

  public OpinionHomeCard(
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
  public HomeCardKey key() {
    return HomeCardKey.OPINION_REPLIES;
  }

  @Override
  public HomeContribution contribute(CurrentUser user, Business business) {
    LocalDate today = LocalDate.now(clock);
    if (accessPolicy.canManage(user, business)) {
      return forManager(business, today);
    }
    if (accessPolicy.isReplier(user, business)) {
      return forReplier(user, business, today);
    }
    return null;
  }

  private HomeContribution forManager(Business business, LocalDate today) {
    List<OpinionRequest> open =
        repository
            .findByBusinessAndStatus(business, OpinionRequestStatus.OPEN, OPEN_REQUESTS)
            .getContent();
    long targets = 0;
    long replied = 0;
    List<HomeCardItem> items = new ArrayList<>();
    for (OpinionRequest request : open) {
      int targetCount = request.getTargets().size();
      long repliedCount =
          replyRepository.findByTarget_Request_Id(request.getId()).stream()
              .filter(OpinionReply::isReplied)
              .count();
      targets += targetCount;
      replied += repliedCount;
      if (items.size() < ITEM_COUNT) {
        items.add(
            new HomeCardItem(
                request.getTitle(),
                "회신 " + repliedCount + "/" + targetCount + " 기관",
                request.getDueDate(),
                urgencyOf(request, today),
                OpinionNotifier.linkOf(request)));
      }
    }
    Urgency urgency = worst(open, today);
    int rate = targets == 0 ? 0 : Math.round(replied * 100f / targets);
    return HomeContribution.of(
        new HomeCard(
            key(),
            key().getTitle(),
            HomeCardSource.REAL,
            urgency,
            open.isEmpty() ? "회신중인 요청 없음" : "회신중 " + open.size() + "건 · 회신율 " + rate + "%",
            items,
            "/" + business.getSlug() + "/" + key().getFeature()));
  }

  private HomeContribution forReplier(CurrentUser user, Business business, LocalDate today) {
    List<OpinionRequest> targeted =
        repository
            .findTargeted(
                business, OpinionRequestStatus.OPEN, user.organizationCode(), OPEN_REQUESTS)
            .getContent();
    // 아직 제출하지 않은 요청과, 결제원이 수정을 허락해 다시 제출해야 하는 요청(수정 중)이 기관이 할 일이다.
    Map<Long, Optional<OpinionReply>> own = new HashMap<>();
    targeted.forEach(r -> own.put(r.getId(), ownReply(user, r)));
    List<OpinionRequest> waiting =
        targeted.stream().filter(r -> stageOf(own.get(r.getId())) != null).toList();
    List<HomeCardItem> items =
        waiting.stream()
            .limit(ITEM_COUNT)
            .map(
                r ->
                    new HomeCardItem(
                        r.getTitle(),
                        stageOf(own.get(r.getId())) + " · " + r.getOwnerName() + " 요청",
                        r.getDueDate(),
                        urgencyOf(r, today),
                        OpinionNotifier.linkOf(r)))
            .toList();
    Urgency urgency = worst(waiting, today);
    return HomeContribution.of(
        new HomeCard(
            key(),
            "회신할 의견 요청",
            HomeCardSource.REAL,
            urgency,
            waiting.isEmpty() ? "회신할 요청 없음" : "회신할 요청 " + waiting.size() + "건",
            items,
            "/" + business.getSlug() + "/" + key().getFeature()));
  }

  private Optional<OpinionReply> ownReply(CurrentUser user, OpinionRequest request) {
    Map<Long, OpinionReply> replies =
        replyRepository.findByTarget_Request_Id(request.getId()).stream()
            .collect(Collectors.toMap(r -> r.getTarget().getId(), Function.identity()));
    return OpinionRequestService.ownTarget(user, request).map(t -> replies.get(t.getId()));
  }

  /** 기관이 할 일의 단계. 할 일이 없으면(제출했고 수정 중이 아님) null. */
  private static String stageOf(Optional<OpinionReply> reply) {
    if (reply.isEmpty()) {
      return "미작성";
    }
    OpinionReply r = reply.get();
    if (r.getStatus() == OpinionReplyStatus.REVISING) {
      return "수정 중 · 다시 제출";
    }
    if (r.isReplied()) {
      return null;
    }
    return r.getStatus() == OpinionReplyStatus.DRAFT ? "임시저장" : "미작성";
  }

  private static Urgency urgencyOf(OpinionRequest request, LocalDate today) {
    long daysLeft = ChronoUnit.DAYS.between(today, request.getDueDate());
    if (daysLeft < 0) {
      return Urgency.OVERDUE;
    }
    return daysLeft <= SOON_DAYS ? Urgency.SOON : Urgency.NONE;
  }

  private static Urgency worst(List<OpinionRequest> requests, LocalDate today) {
    return requests.stream()
        .map(r -> urgencyOf(r, today))
        .max(Enum::compareTo)
        .orElse(Urgency.NONE);
  }
}
