package com.connectlink.attachment;

import com.connectlink.account.AccountService;
import com.connectlink.account.TeamLeader;
import com.connectlink.attachment.dto.AttachmentApprovalBatchResponse;
import com.connectlink.attachment.dto.AttachmentApprovalResponse;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.NetworkZoneResolver;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 파일 반출 결재. 결제원 담당자가 내부망에서 올린 파일은 참가기관이 외부망에서 보게 되므로 외부망 반출이고, 결제원 규정대로 같은 부서·팀의 팀장(직급 "팀장")이 승인해야
 * 결제원 밖 계정에 보인다. 팀장 본인이 올린 파일은 바로 승인이다. 참가기관이 올린 파일은 결재가 없다.
 *
 * <p>결재자는 올릴 때 정해진다. 결재는 등록 한 건(대상 하나)에 붙인 파일을 한꺼번에 하고, 팀장은 알림 센터에서 결재 창을 연다. 승인·반려는 그 결재자만, 파일을 열어
 * 볼 수 있는 내부망에서만 한다. 반려된 파일은 결제원 계정에만 남고 올린 사람이 지우고 다시 올린다.
 */
@Service
@Transactional(readOnly = true)
public class AttachmentApprovalService {

  private final AttachmentRepository repository;
  private final AttachmentAccessPolicy accessPolicy;
  private final AccountService accountService;
  private final NotificationService notificationService;
  private final NetworkZoneResolver networkZoneResolver;
  private final Clock clock;

  public AttachmentApprovalService(
      AttachmentRepository repository,
      AttachmentAccessPolicy accessPolicy,
      AccountService accountService,
      NotificationService notificationService,
      NetworkZoneResolver networkZoneResolver,
      Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.accountService = accountService;
    this.notificationService = notificationService;
    this.networkZoneResolver = networkZoneResolver;
    this.clock = clock;
  }

  /** 결재가 필요한 업로드인가. 결제원 계정이 올린 파일이다. */
  static boolean requiresApproval(CurrentUser uploader) {
    return uploader.organizationType() == OrganizationType.KFTC;
  }

  /**
   * 결제원 담당자가 올리는 파일의 결재자(같은 팀 팀장). 파일을 저장하기 전에 부른다. 팀에 팀장 계정이 없으면 400 {@code
   * ATTACHMENT_APPROVER_NOT_FOUND} 로 올리지 못한다.
   */
  TeamLeader approverFor(CurrentUser uploader) {
    return accountService
        .findTeamLeaderOf(uploader.email())
        .orElseThrow(
            () ->
                new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "ATTACHMENT_APPROVER_NOT_FOUND",
                    "같은 팀에 팀장 계정이 없어 파일 반출 결재를 요청할 수 없습니다. 팀장님이 연결이에 가입한 뒤 올려 주세요."));
  }

  /** 저장 전 첨부에 결재자를 적는다. 팀장 본인이면 바로 승인이다. */
  void assign(Attachment attachment, CurrentUser uploader, TeamLeader leader) {
    attachment.requireApproval(
        uploader.email(),
        uploader.name(),
        leader.teamLabel(),
        leader.email(),
        leader.name(),
        LocalDateTime.now(clock));
  }

  /**
   * 저장한 파일이 결재 대기면 팀장에게 알린다(알림 센터와 메일). 결재는 등록 한 건 단위라, 같은 대상에 이미 결재 대기 중인 파일이 있으면(등록하며 붙인 둘째 파일부터)
   * 다시 알리지 않는다. 팀장은 처음 알림에서 그 건의 파일을 모두 본다.
   */
  void notifyRequested(Attachment attachment) {
    if (attachment.getApprovalStatus() != AttachmentApprovalStatus.PENDING
        || repository.existsByOwnerTypeAndOwnerIdAndApproverEmailAndApprovalStatusAndIdNot(
            attachment.getOwnerType(),
            attachment.getOwnerId(),
            attachment.getApproverEmail(),
            AttachmentApprovalStatus.PENDING,
            attachment.getId())) {
      return;
    }
    AttachmentOwnerInfo owner =
        accessPolicy.describe(attachment.getOwnerType(), attachment.getOwnerId());
    notificationService.publish(
        new NotificationCommand(
            attachment.getApproverEmail(),
            NotificationType.ATTACHMENT_APPROVAL_REQUESTED,
            owner.business(),
            attachment.getUploaderName() + " 님이 첨부파일 반출 결재를 요청했습니다",
            attachment.getOwnerType().getLabel()
                + " \""
                + owner.title()
                + "\" 의 첨부파일입니다. 승인하면 참가기관에 보입니다. 내부망 연결이 알림 센터에서 파일을 열어 보고 결재해 주세요.",
            approverLink(
                attachment.getOwnerType(),
                attachment.getOwnerId(),
                owner,
                attachment.getApproverEmail()),
            Set.of(NotificationChannel.EMAIL)));
  }

  /** 알림을 누르면 열 주소. 알림 센터가 결재 창을 연다. 업무 영역은 대상 업무를 맡았으면 그 업무, 아니면 맡은 업무 중 첫째. */
  private String approverLink(
      AttachmentOwnerType ownerType,
      Long ownerId,
      AttachmentOwnerInfo owner,
      String approverEmail) {
    Set<Business> businesses =
        accountService.findTeamLeaderOf(approverEmail).map(TeamLeader::businesses).orElse(Set.of());
    Business business =
        owner.business() != null && businesses.contains(owner.business())
            ? owner.business()
            : businesses.stream().sorted().findFirst().orElse(Business.ACCOUNT_INFO);
    return "/"
        + business.getSlug()
        + "/notifications?approvalOwnerType="
        + ownerType.name()
        + "&approvalOwnerId="
        + ownerId;
  }

  /** 결재 한 건. 그 대상에서 내가 결재자인 파일이다. 없으면 404. */
  public AttachmentApprovalBatchResponse get(
      CurrentUser user, AttachmentOwnerType ownerType, Long ownerId) {
    ensureKftc(user);
    List<Attachment> files = repository.findForApprover(ownerType, ownerId, user.email());
    if (files.isEmpty()) {
      throw BusinessException.notFound("ATTACHMENT_APPROVAL_NOT_FOUND", "결재할 첨부파일이 없습니다.");
    }
    return toResponse(ownerType, ownerId, files);
  }

  /** 결재 대기 파일을 모두 승인한다. 올린 사람에게 한 번 알린다. */
  @Transactional
  public AttachmentApprovalBatchResponse approve(
      CurrentUser user, AttachmentOwnerType ownerType, Long ownerId) {
    List<Attachment> pending = loadPendingFor(user, ownerType, ownerId);
    LocalDateTime now = LocalDateTime.now(clock);
    pending.forEach(a -> a.approve(now));
    notifyDecided(pending, "첨부파일 반출이 승인되었습니다", "이제 참가기관에서도 첨부파일을 볼 수 있습니다.");
    return get(user, ownerType, ownerId);
  }

  /** 결재 대기 파일을 모두 반려한다. 사유는 올린 사람에게 알림으로 간다. */
  @Transactional
  public AttachmentApprovalBatchResponse reject(
      CurrentUser user, AttachmentOwnerType ownerType, Long ownerId, String reason) {
    List<Attachment> pending = loadPendingFor(user, ownerType, ownerId);
    LocalDateTime now = LocalDateTime.now(clock);
    String trimmed = reason.trim();
    pending.forEach(a -> a.reject(trimmed, now));
    notifyDecided(pending, "첨부파일 반출이 반려되었습니다", "반려 사유: " + trimmed + ". 파일을 고쳐 다시 올려 주세요.");
    return get(user, ownerType, ownerId);
  }

  private List<Attachment> loadPendingFor(
      CurrentUser user, AttachmentOwnerType ownerType, Long ownerId) {
    ensureKftc(user);
    networkZoneResolver.ensureInternal("파일 반출 결재");
    List<Attachment> files = repository.findForApprover(ownerType, ownerId, user.email());
    if (files.isEmpty()) {
      throw BusinessException.notFound("ATTACHMENT_APPROVAL_NOT_FOUND", "결재할 첨부파일이 없습니다.");
    }
    List<Attachment> pending =
        files.stream()
            .filter(a -> a.getApprovalStatus() == AttachmentApprovalStatus.PENDING)
            .collect(Collectors.toList());
    if (pending.isEmpty()) {
      throw BusinessException.conflict("ATTACHMENT_ALREADY_DECIDED", "이미 결재한 첨부파일입니다.");
    }
    return pending;
  }

  /** 올린 사람마다 한 번 알린다(보통 등록한 사람 한 명). */
  private void notifyDecided(List<Attachment> decided, String title, String detail) {
    Attachment first = decided.get(0);
    AttachmentOwnerInfo owner = accessPolicy.describe(first.getOwnerType(), first.getOwnerId());
    String link =
        owner.path() == null || owner.business() == null
            ? null
            : "/" + owner.business().getSlug() + "/" + owner.path();
    Map<String, List<Attachment>> byUploader =
        decided.stream()
            .collect(
                Collectors.groupingBy(
                    Attachment::getCreatedBy, LinkedHashMap::new, Collectors.toList()));
    byUploader.forEach(
        (uploader, files) ->
            notificationService.publish(
                NotificationCommand.inAppOnly(
                    uploader,
                    NotificationType.ATTACHMENT_APPROVAL_DECIDED,
                    owner.business(),
                    title,
                    first.getOwnerType().getLabel()
                        + " \""
                        + owner.title()
                        + "\" 의 첨부파일 "
                        + files.size()
                        + "개. "
                        + detail,
                    link)));
  }

  private static void ensureKftc(CurrentUser user) {
    if (user.organizationType() != OrganizationType.KFTC) {
      throw new AccessDeniedException("파일 반출 결재는 결제원 담당자만 씁니다.");
    }
  }

  private AttachmentApprovalBatchResponse toResponse(
      AttachmentOwnerType ownerType, Long ownerId, List<Attachment> files) {
    AttachmentOwnerInfo owner = accessPolicy.describe(ownerType, ownerId);
    return new AttachmentApprovalBatchResponse(
        ownerType,
        ownerType.getLabel(),
        ownerId,
        owner.title(),
        owner.business() == null ? null : owner.business().getSlug(),
        owner.path(),
        files.stream().map(AttachmentApprovalService::toFile).collect(Collectors.toList()));
  }

  private static AttachmentApprovalResponse toFile(Attachment a) {
    return new AttachmentApprovalResponse(
        a.getId(),
        a.getOriginalName(),
        a.getContentType(),
        a.getSizeBytes(),
        a.getCreatedBy(),
        a.getUploaderName(),
        a.getUploaderTeam(),
        a.getCreatedAt(),
        a.getApprovalStatus(),
        a.getApproverName(),
        a.getDecidedAt(),
        a.getRejectionReason());
  }
}
