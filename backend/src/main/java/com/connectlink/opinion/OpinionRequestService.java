package com.connectlink.opinion;

import com.connectlink.account.AccountService;
import com.connectlink.account.BusinessMember;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.attachment.AttachmentService;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.history.ChangeHistoryCommand;
import com.connectlink.history.ChangeHistoryService;
import com.connectlink.history.FieldChanges;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.history.dto.FieldChange;
import com.connectlink.opinion.OpinionTarget.RecipientSnapshot;
import com.connectlink.opinion.dto.OpinionRecipientCandidateResponse;
import com.connectlink.opinion.dto.OpinionRecipientResponse;
import com.connectlink.opinion.dto.OpinionReplyFormRequest;
import com.connectlink.opinion.dto.OpinionReplyFormResponse;
import com.connectlink.opinion.dto.OpinionRequestCountsResponse;
import com.connectlink.opinion.dto.OpinionRequestCreateRequest;
import com.connectlink.opinion.dto.OpinionRequestDetailResponse;
import com.connectlink.opinion.dto.OpinionRequestPermissions;
import com.connectlink.opinion.dto.OpinionRequestSummaryResponse;
import com.connectlink.opinion.dto.OpinionRequestUpdateRequest;
import com.connectlink.opinion.dto.OpinionTargetRequest;
import com.connectlink.opinion.dto.OpinionTargetResponse;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의견 요청. 결제원 담당자의 요청 작성·수정·취소·상태 관리(작성중 → 회신중 → 마감)와, 양쪽이 보는 목록·상세를 맡는다. 기관 회신은 {@link
 * OpinionReplyService}, 회신 현황·취합·리마인드는 {@link OpinionCollationService} 가 한다. 누가 무엇을 하는지는 {@link
 * OpinionAccessPolicy} 가 정한다.
 */
@Service
@Transactional
public class OpinionRequestService {

  private final OpinionRequestRepository repository;
  private final OpinionReplyRepository replyRepository;
  private final OpinionAccessPolicy accessPolicy;
  private final OrganizationService organizationService;
  private final AccountService accountService;
  private final AttachmentService attachmentService;
  private final ChangeHistoryService changeHistoryService;
  private final OpinionNotifier notifier;
  private final Clock clock;

  public OpinionRequestService(
      OpinionRequestRepository repository,
      OpinionReplyRepository replyRepository,
      OpinionAccessPolicy accessPolicy,
      OrganizationService organizationService,
      AccountService accountService,
      AttachmentService attachmentService,
      ChangeHistoryService changeHistoryService,
      OpinionNotifier notifier,
      Clock clock) {
    this.repository = repository;
    this.replyRepository = replyRepository;
    this.accessPolicy = accessPolicy;
    this.organizationService = organizationService;
    this.accountService = accountService;
    this.attachmentService = attachmentService;
    this.changeHistoryService = changeHistoryService;
    this.notifier = notifier;
    this.clock = clock;
  }

  /**
   * 한 업무 영역의 요청 목록 중 한 상태. 결제원 담당자는 그 업무의 모든 요청을, 기관 담당자는 자기 기관이 대상인 회신중·마감 요청만 본다. 회신중은 기한이 가까운 순,
   * 작성중은 최근 고친 순, 마감은 최근 마감한 순이다.
   */
  @Transactional(readOnly = true)
  public PageResponse<OpinionRequestSummaryResponse> list(
      CurrentUser user, Business business, OpinionRequestStatus status, Pageable pageable) {
    Pageable sorted =
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOf(status));
    Page<OpinionRequest> page;
    if (accessPolicy.canManage(user, business)) {
      page = repository.findByBusinessAndStatus(business, status, sorted);
    } else if (accessPolicy.isReplier(user, business)) {
      page =
          status == OpinionRequestStatus.DRAFT
              ? Page.empty(sorted)
              : repository.findTargeted(business, status, user.organizationCode(), sorted);
    } else {
      throw new AccessDeniedException(OpinionAccessPolicy.VIEW_DENIED);
    }
    LocalDate today = today();
    return PageResponse.from(page, r -> toSummary(user, r, today));
  }

  @Transactional(readOnly = true)
  public OpinionRequestCountsResponse counts(CurrentUser user, Business business) {
    if (accessPolicy.canManage(user, business)) {
      return new OpinionRequestCountsResponse(
          repository.countByBusinessAndStatus(business, OpinionRequestStatus.DRAFT),
          repository.countByBusinessAndStatus(business, OpinionRequestStatus.OPEN),
          repository.countByBusinessAndStatus(business, OpinionRequestStatus.CLOSED));
    }
    if (accessPolicy.isReplier(user, business)) {
      String code = user.organizationCode();
      return new OpinionRequestCountsResponse(
          0,
          repository.countTargeted(business, OpinionRequestStatus.OPEN, code),
          repository.countTargeted(business, OpinionRequestStatus.CLOSED, code));
    }
    throw new AccessDeniedException(OpinionAccessPolicy.VIEW_DENIED);
  }

  @Transactional(readOnly = true)
  public OpinionRequestDetailResponse get(CurrentUser user, Long id) {
    OpinionRequest request = load(id);
    accessPolicy.ensureCanView(user, request);
    return toDetail(user, request);
  }

  /** 요청을 작성중으로 만든다. 첨부자료와 양식 파일은 저장한 뒤 첨부 API 로 올린다. */
  public OpinionRequestDetailResponse create(CurrentUser user, OpinionRequestCreateRequest body) {
    accessPolicy.ensureCanManage(user, body.business());
    ensureDueDateNotPast(body.dueDate());
    OpinionRequest request =
        repository.saveAndFlush(
            OpinionRequest.draft(
                body.business(),
                body.title().trim(),
                body.content().trim(),
                body.dueDate(),
                user.email(),
                user.name()));
    applyForms(request, body.forms());
    applyTargets(request, body.targets());
    repository.flush();
    Snapshot after = Snapshot.of(request);
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.created(
            HistoryTargetType.OPINION_REQUEST,
            request.getId(),
            request.getBusiness(),
            request.getTitle() + " 작성",
            after.diff(Snapshot.EMPTY, true)));
    return toDetail(user, request);
  }

  /**
   * 요청명·내용·기한·양식·대상을 고친다. 작성중과 회신중에 고친다. 회신중이면 새로 들어간 회신 담당자에게 의견 요청 메일이, 남은 담당자에게는 바뀐 내용 알림이 간다.
   * 회신을 저장한 기관은 대상에서 뺄 수 없다.
   */
  public OpinionRequestDetailResponse update(
      CurrentUser user, Long id, OpinionRequestUpdateRequest body) {
    OpinionRequest request = loadManaged(user, id);
    ensureNotClosed(request);
    if (!body.dueDate().equals(request.getDueDate())) {
      ensureDueDateNotPast(body.dueDate());
    }
    Snapshot before = Snapshot.of(request);
    Set<String> recipientsBefore = recipientEmails(request);
    request.change(body.title().trim(), body.content().trim(), body.dueDate());
    applyForms(request, body.forms());
    applyTargets(request, body.targets());
    repository.flush();
    Snapshot after = Snapshot.of(request);
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.OPINION_REQUEST,
            request.getId(),
            request.getBusiness(),
            request.getTitle() + " 수정",
            after.diff(before, false)));
    if (request.getStatus() == OpinionRequestStatus.OPEN) {
      Set<String> recipientsAfter = recipientEmails(request);
      Set<String> added = new LinkedHashSet<>(recipientsAfter);
      added.removeAll(recipientsBefore);
      notifier.opened(request, added);
      String what = before.changedSummary(after);
      if (what != null) {
        Set<String> kept = new LinkedHashSet<>(recipientsAfter);
        kept.removeAll(added);
        notifier.changed(request, kept, what);
      }
    }
    return toDetail(user, request);
  }

  /** 작성중 요청을 회신중으로 연다. 대상 기관이 하나 이상 있어야 하고 기한이 지나지 않아야 한다. 회신 담당자에게 의견 요청 메일이 간다. */
  public OpinionRequestDetailResponse open(CurrentUser user, Long id) {
    OpinionRequest request = loadManaged(user, id);
    ensureStatus(request, OpinionRequestStatus.DRAFT, "작성중 요청만 회신 요청을 보낼 수 있습니다.");
    if (request.getTargets().isEmpty()) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "OPINION_TARGETS_REQUIRED", "targets", "대상 기관을 하나 이상 골라 주세요.");
    }
    ensureDueDateNotPast(request.getDueDate());
    request.open(now());
    recordStatus(user, request, OpinionRequestStatus.DRAFT, "회신 요청 보냄", null);
    notifier.opened(request, recipientEmails(request));
    return toDetail(user, request);
  }

  /** 회신중 요청을 마감한다. 수정 요청·수정 중이던 회신은 제출한 내용으로 돌아간다. */
  public OpinionRequestDetailResponse close(CurrentUser user, Long id) {
    OpinionRequest request = loadManaged(user, id);
    ensureStatus(request, OpinionRequestStatus.OPEN, "회신중 요청만 마감할 수 있습니다.");
    request.close(now());
    replyRepository.findByTarget_Request_Id(request.getId()).forEach(OpinionReply::closeOut);
    recordStatus(user, request, OpinionRequestStatus.OPEN, "마감", null);
    return toDetail(user, request);
  }

  /** 회신중 요청을 취소한다. 마감 상태가 되고 사유를 담은 알림과 메일이 회신 담당자에게 간다. */
  public OpinionRequestDetailResponse cancel(CurrentUser user, Long id, String reason) {
    OpinionRequest request = loadManaged(user, id);
    ensureStatus(request, OpinionRequestStatus.OPEN, "회신중 요청만 취소할 수 있습니다. 작성중 요청은 지웁니다.");
    request.cancel(reason.trim(), now());
    replyRepository.findByTarget_Request_Id(request.getId()).forEach(OpinionReply::closeOut);
    recordStatus(user, request, OpinionRequestStatus.OPEN, "취소", request.getCancelReason());
    notifier.cancelled(request, recipientEmails(request));
    return toDetail(user, request);
  }

  /** 작성중 요청을 지운다. 첨부자료와 양식 파일도 함께 지운다. 아직 대상 기관에 보이지 않았으므로 알림은 없다. */
  public void delete(CurrentUser user, Long id) {
    OpinionRequest request = loadManaged(user, id);
    ensureStatus(request, OpinionRequestStatus.DRAFT, "작성중 요청만 지울 수 있습니다. 회신중 요청은 취소합니다.");
    attachmentService.deleteAllOf(AttachmentOwnerType.OPINION_REQUEST, request.getId());
    request
        .getForms()
        .forEach(
            f -> attachmentService.deleteAllOf(AttachmentOwnerType.OPINION_REPLY_FORM, f.getId()));
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.deleted(
            HistoryTargetType.OPINION_REQUEST,
            request.getId(),
            request.getBusiness(),
            request.getTitle() + " 삭제",
            FieldChanges.compare().value("title", "요청명", request.getTitle(), null).toList()));
    repository.delete(request);
  }

  /**
   * 대상으로 고를 수 있는 기관과 그 기관의 같은 업무 담당자. 그 업무에 참여하는 사용 중 참가기관이 기관 이름순으로 나오고, 담당자는 이름순이다. 담당자가 없는 기관도
   * 나온다(회신 담당자 없이 대상에 넣을 수 있다).
   */
  @Transactional(readOnly = true)
  public List<OpinionRecipientCandidateResponse> candidates(CurrentUser user, Business business) {
    accessPolicy.ensureCanManage(user, business);
    Map<Long, List<BusinessMember>> membersByOrganization =
        accountService.listActiveMembers(business).stream()
            .collect(Collectors.groupingBy(BusinessMember::organizationId));
    return organizationService.listActive().stream()
        .filter(o -> o.type() == OrganizationType.FINANCE_ORGANIZATION)
        .filter(o -> o.businesses().contains(business))
        .map(
            o ->
                new OpinionRecipientCandidateResponse(
                    o.id(),
                    o.code(),
                    o.name(),
                    o.sector(),
                    o.sectorLabel(),
                    membersByOrganization.getOrDefault(o.id(), List.of()).stream()
                        .map(
                            m ->
                                new OpinionRecipientCandidateResponse.Member(
                                    m.accountId(),
                                    m.name(),
                                    m.email(),
                                    m.subBusiness(),
                                    m.jobRoleLabel(),
                                    m.department(),
                                    m.team(),
                                    m.jobTitle()))
                        .toList()))
        .toList();
  }

  // ---- 양식·대상 반영 ----

  /**
   * 양식을 요청 목록대로 맞춘다. 빠진 양식은 그 양식을 고른 대상에서 먼저 떼고 파일과 함께 지운다. 이름은 요청 안에서 겹치지 않는다. 지운 뒤 바꾸고 더하는 순서라 지울
   * 양식의 이름을 다른 양식이 받아도 유일 제약에 걸리지 않는다.
   */
  private void applyForms(OpinionRequest request, List<OpinionReplyFormRequest> forms) {
    Set<String> names = new HashSet<>();
    for (OpinionReplyFormRequest form : forms) {
      if (!names.add(form.name().trim())) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST,
            "OPINION_FORM_DUPLICATE",
            "forms",
            "양식 이름이 겹칩니다: " + form.name().trim());
      }
    }
    Map<Long, OpinionReplyForm> existing =
        request.getForms().stream()
            .collect(Collectors.toMap(OpinionReplyForm::getId, Function.identity()));
    Set<Long> keep =
        forms.stream()
            .map(OpinionReplyFormRequest::id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    for (Long id : keep) {
      if (!existing.containsKey(id)) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST, "OPINION_FORM_NOT_FOUND", "forms", "이 요청의 양식이 아닙니다: " + id);
      }
    }
    for (OpinionReplyForm form : existing.values()) {
      if (!keep.contains(form.getId())) {
        request.getTargets().stream()
            .filter(t -> t.getForm() == form)
            .forEach(t -> t.changeForm(null));
        request.removeForm(form);
        attachmentService.deleteAllOf(AttachmentOwnerType.OPINION_REPLY_FORM, form.getId());
      }
    }
    repository.flush();
    int order = 0;
    for (OpinionReplyFormRequest form : forms) {
      String description = blankToNull(form.description());
      if (form.id() != null) {
        existing.get(form.id()).change(form.name().trim(), description, order);
      } else {
        request.addForm(form.name().trim(), description, order);
      }
      order++;
    }
    repository.flush();
  }

  /**
   * 대상 기관과 회신 담당자를 요청 목록대로 맞춘다. 대상은 그 업무에 참여하는 사용 중 참가기관이고, 회신 담당자는 그 기관의 같은 업무 담당자(사용 중 계정)다. 회신을
   * 저장한 기관은 뺄 수 없다.
   */
  private void applyTargets(OpinionRequest request, List<OpinionTargetRequest> targets) {
    Business business = request.getBusiness();
    Set<Long> organizationIds = new HashSet<>();
    for (OpinionTargetRequest target : targets) {
      if (!organizationIds.add(target.organizationId())) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST, "OPINION_TARGET_DUPLICATE", "targets", "같은 기관을 두 번 골랐습니다.");
      }
    }
    for (OpinionTarget target : request.getTargets()) {
      if (!organizationIds.contains(target.getOrganization().getId())) {
        if (replyRepository.existsByTarget_Id(target.getId())) {
          throw BusinessException.conflict(
              "OPINION_TARGET_HAS_REPLY",
              target.getOrganization().getName() + " 은(는) 회신을 저장해서 대상에서 뺄 수 없습니다.");
        }
        request.removeTarget(target);
      }
    }
    Map<String, OpinionReplyForm> formsByName =
        request.getForms().stream()
            .collect(Collectors.toMap(OpinionReplyForm::getName, Function.identity()));
    Map<Long, Map<Long, BusinessMember>> members = new HashMap<>();
    accountService
        .listActiveMembers(business)
        .forEach(
            m ->
                members
                    .computeIfAbsent(m.organizationId(), k -> new HashMap<>())
                    .put(m.accountId(), m));
    for (OpinionTargetRequest body : targets) {
      Organization organization = loadTargetOrganization(body.organizationId(), business);
      OpinionReplyForm form = null;
      if (body.formName() != null && !body.formName().isBlank()) {
        form = formsByName.get(body.formName().trim());
        if (form == null) {
          throw BusinessException.onField(
              HttpStatus.BAD_REQUEST,
              "OPINION_FORM_NOT_FOUND",
              "targets",
              organization.getName() + " 의 회신 양식이 없습니다: " + body.formName());
        }
      }
      Map<Long, BusinessMember> candidates = members.getOrDefault(organization.getId(), Map.of());
      Map<Long, RecipientSnapshot> recipients = new LinkedHashMap<>();
      for (Long accountId : body.recipientAccountIds()) {
        BusinessMember member = candidates.get(accountId);
        if (member == null) {
          throw BusinessException.onField(
              HttpStatus.BAD_REQUEST,
              "OPINION_RECIPIENT_INVALID",
              "targets",
              organization.getName() + " 의 " + business.getLabel() + " 담당자가 아닌 사람을 회신 담당자로 골랐습니다.");
        }
        recipients.put(
            accountId, new RecipientSnapshot(member.email(), member.name(), member.subBusiness()));
      }
      OpinionTarget target =
          request.findTarget(organization.getId()).orElseGet(() -> request.addTarget(organization));
      target.changeForm(form);
      target.replaceRecipients(recipients);
    }
  }

  private Organization loadTargetOrganization(Long organizationId, Business business) {
    Organization organization;
    try {
      organization = organizationService.getActive(organizationId);
    } catch (BusinessException e) {
      throw invalidTarget(business);
    }
    if (organization.getType() != OrganizationType.FINANCE_ORGANIZATION
        || !organization.participatesIn(business)) {
      throw invalidTarget(business);
    }
    return organization;
  }

  private static BusinessException invalidTarget(Business business) {
    return BusinessException.onField(
        HttpStatus.BAD_REQUEST,
        "OPINION_TARGET_INVALID",
        "targets",
        "대상 기관은 " + business.getLabel() + " 에 참여하는 사용 중 참가기관이어야 합니다.");
  }

  // ---- 응답 ----

  OpinionRequestSummaryResponse toSummary(
      CurrentUser user, OpinionRequest request, LocalDate today) {
    boolean manage = accessPolicy.canManage(user, request.getBusiness());
    Integer targetCount = null;
    Integer repliedCount = null;
    OpinionReplyStatus myStatus = null;
    if (manage) {
      targetCount = request.getTargets().size();
      repliedCount =
          (int)
              replyRepository.findByTarget_Request_Id(request.getId()).stream()
                  .filter(OpinionReply::isReplied)
                  .count();
    } else {
      myStatus =
          ownTarget(user, request)
              .flatMap(t -> replyRepository.findByTarget_Id(t.getId()))
              .map(OpinionReply::getStatus)
              .orElse(null);
    }
    return new OpinionRequestSummaryResponse(
        request.getId(),
        request.getTitle(),
        request.getStatus(),
        request.getStatus().getLabel(),
        request.isCancelled(),
        request.getDueDate(),
        ChronoUnit.DAYS.between(today, request.getDueDate()),
        request.getOwnerName(),
        targetCount,
        repliedCount,
        myStatus,
        myStatus == null ? null : myStatus.getLabel(),
        request.getOpenedAt(),
        request.getUpdatedAt());
  }

  OpinionRequestDetailResponse toDetail(CurrentUser user, OpinionRequest request) {
    boolean manage = accessPolicy.canManage(user, request.getBusiness());
    boolean member = accessPolicy.isTargetMember(user, request);
    OpinionRequestStatus status = request.getStatus();
    Map<Long, OpinionReply> replies =
        replyRepository.findByTarget_Request_Id(request.getId()).stream()
            .collect(Collectors.toMap(r -> r.getTarget().getId(), Function.identity()));
    List<OpinionTarget> targets =
        manage ? request.getTargets() : ownTarget(user, request).stream().toList();
    List<OpinionReplyForm> forms =
        manage
            ? request.getForms()
            : targets.stream().map(OpinionTarget::getForm).filter(Objects::nonNull).toList();
    return new OpinionRequestDetailResponse(
        request.getId(),
        request.getBusiness(),
        request.getBusiness().getSlug(),
        request.getBusiness().getLabel(),
        request.getTitle(),
        request.getContent(),
        request.getDueDate(),
        ChronoUnit.DAYS.between(today(), request.getDueDate()),
        status,
        status.getLabel(),
        request.isCancelled(),
        request.getCancelReason(),
        request.getOwnerName(),
        request.getOpenedAt(),
        request.getClosedAt(),
        request.getCreatedAt(),
        request.getUpdatedAt(),
        forms.stream().map(OpinionRequestService::toFormResponse).toList(),
        targets.stream().map(t -> toTargetResponse(t, replies.get(t.getId()))).toList(),
        new OpinionRequestPermissions(
            manage,
            manage && status != OpinionRequestStatus.CLOSED,
            manage && status == OpinionRequestStatus.DRAFT,
            manage && status == OpinionRequestStatus.OPEN,
            manage && status == OpinionRequestStatus.OPEN,
            manage && status == OpinionRequestStatus.DRAFT,
            member));
  }

  static OpinionReplyFormResponse toFormResponse(OpinionReplyForm form) {
    return new OpinionReplyFormResponse(form.getId(), form.getName(), form.getDescription());
  }

  private static OpinionTargetResponse toTargetResponse(OpinionTarget target, OpinionReply reply) {
    Organization organization = target.getOrganization();
    OpinionReplyForm form = target.getForm();
    return new OpinionTargetResponse(
        target.getId(),
        organization.getId(),
        organization.getCode(),
        organization.getName(),
        form == null ? null : form.getId(),
        form == null ? null : form.getName(),
        target.getRecipients().stream()
            .map(
                r ->
                    new OpinionRecipientResponse(
                        r.getAccountId(), r.getName(), r.getEmail(), r.getSubBusiness()))
            .toList(),
        reply != null && reply.isReplied(),
        reply == null ? null : reply.getStatus());
  }

  // ---- 공통 ----

  OpinionRequest load(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> BusinessException.notFound("OPINION_REQUEST_NOT_FOUND", "의견 요청을 찾을 수 없습니다."));
  }

  /** 관리할 수 있는 요청. 없으면 404, 그 업무의 결제원 담당자가 아니면 403 이다. */
  OpinionRequest loadManaged(CurrentUser user, Long id) {
    OpinionRequest request = load(id);
    accessPolicy.ensureCanManage(user, request.getBusiness());
    return request;
  }

  /** 이 사용자(기관 담당자) 기관의 대상 행. */
  static Optional<OpinionTarget> ownTarget(CurrentUser user, OpinionRequest request) {
    return request.getTargets().stream()
        .filter(t -> t.getOrganization().getCode().equals(user.organizationCode()))
        .findFirst();
  }

  static Set<String> recipientEmails(OpinionRequest request) {
    Set<String> emails = new LinkedHashSet<>();
    request.getTargets().forEach(t -> t.getRecipients().forEach(r -> emails.add(r.getEmail())));
    return emails;
  }

  private void recordStatus(
      CurrentUser user,
      OpinionRequest request,
      OpinionRequestStatus before,
      String what,
      String cancelReason) {
    FieldChanges changes =
        FieldChanges.compare().field("status", "상태", before, request.getStatus());
    if (cancelReason != null) {
      changes.value("cancelReason", "취소 사유", null, cancelReason);
    }
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.OPINION_REQUEST,
            request.getId(),
            request.getBusiness(),
            request.getTitle() + " " + what,
            changes.toList()));
  }

  private static void ensureStatus(
      OpinionRequest request, OpinionRequestStatus expected, String message) {
    if (request.getStatus() != expected) {
      throw BusinessException.conflict("OPINION_REQUEST_STATUS", message);
    }
  }

  private static void ensureNotClosed(OpinionRequest request) {
    if (request.getStatus() == OpinionRequestStatus.CLOSED) {
      throw BusinessException.conflict("OPINION_REQUEST_STATUS", "마감한 요청은 고칠 수 없습니다.");
    }
  }

  private void ensureDueDateNotPast(LocalDate dueDate) {
    if (dueDate.isBefore(today())) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "OPINION_DUE_DATE_PAST", "dueDate", "회신 기한은 오늘이나 그 뒤로 골라 주세요.");
    }
  }

  private static Sort sortOf(OpinionRequestStatus status) {
    return switch (status) {
      case OPEN -> Sort.by(Sort.Order.asc("dueDate"), Sort.Order.desc("id"));
      case DRAFT -> Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
      case CLOSED -> Sort.by(Sort.Order.desc("closedAt"), Sort.Order.desc("id"));
    };
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  LocalDate today() {
    return LocalDate.now(clock);
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  /** 이력 비교용 요청 값. 대상과 회신 담당자는 "기관: 담당자" 한 줄로 늘어놓는다. */
  private record Snapshot(
      String title,
      String content,
      LocalDate dueDate,
      String forms,
      String targets,
      String recipients) {

    static final Snapshot EMPTY = new Snapshot(null, null, null, null, null, null);

    static Snapshot of(OpinionRequest request) {
      List<String> targetNames = new ArrayList<>();
      List<String> recipientLines = new ArrayList<>();
      for (OpinionTarget target : request.getTargets()) {
        String name = target.getOrganization().getName();
        targetNames.add(
            target.getForm() == null ? name : name + "(" + target.getForm().getName() + ")");
        if (!target.getRecipients().isEmpty()) {
          recipientLines.add(
              name
                  + ": "
                  + target.getRecipients().stream()
                      .map(OpinionTargetRecipient::getName)
                      .collect(Collectors.joining(", ")));
        }
      }
      return new Snapshot(
          request.getTitle(),
          request.getContent(),
          request.getDueDate(),
          FieldChanges.join(request.getForms().stream().map(OpinionReplyForm::getName).toList()),
          FieldChanges.join(targetNames),
          recipientLines.isEmpty() ? null : String.join(" / ", recipientLines));
    }

    /** {@code before} 에서 이것으로 바뀐 항목. {@code all} 이면 같은 값도 남긴다(작성 이력). */
    List<FieldChange> diff(Snapshot before, boolean all) {
      FieldChanges changes = FieldChanges.compare();
      add(changes, all, "title", "요청명", before.title, title);
      add(changes, all, "content", "요청 내용", before.content, content);
      add(changes, all, "dueDate", "회신 기한", before.dueDate, dueDate);
      add(changes, all, "forms", "회신 양식", before.forms, forms);
      add(changes, all, "targets", "대상 기관", before.targets, targets);
      add(changes, all, "recipients", "회신 담당자", before.recipients, recipients);
      return changes.toList();
    }

    private static void add(
        FieldChanges changes,
        boolean all,
        String field,
        String label,
        Object before,
        Object after) {
      if (all) {
        if (after != null) {
          changes.value(field, label, null, after);
        }
      } else {
        changes.field(field, label, before, after);
      }
    }

    /** 기관에 알릴 만한 변경(요청명·내용·기한)을 "요청명, 회신 기한" 꼴로. 없으면 null. */
    String changedSummary(Snapshot after) {
      List<String> what = new ArrayList<>();
      if (!Objects.equals(title, after.title)) what.add("요청명");
      if (!Objects.equals(content, after.content)) what.add("요청 내용");
      if (!Objects.equals(dueDate, after.dueDate)) what.add("회신 기한");
      return what.isEmpty() ? null : String.join(", ", what);
    }
  }
}
