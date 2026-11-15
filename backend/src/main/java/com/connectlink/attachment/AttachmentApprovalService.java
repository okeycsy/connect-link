package com.connectlink.attachment;

import com.connectlink.account.AccountService;
import com.connectlink.account.TeamLeader;
import com.connectlink.attachment.dto.AttachmentApprovalResponse;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.NetworkZoneResolver;
import com.connectlink.common.PageResponse;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 파일 반출 결재. 결제원 담당자가 내부망에서 올린 파일은 참가기관이 외부망에서 보게 되므로 외부망 반출이고, 결제원 규정대로 같은 부서·팀의 팀장(직급 "팀장")이 승인해야
 * 결제원 밖 계정에 보인다. 팀장 본인이 올린 파일은 바로 승인이다. 참가기관이 올린 파일은 결재가 없다.
 *
 * <p>결재자는 올릴 때 정해진다. 승인·반려는 그 결재자만, 파일을 열어 볼 수 있는 내부망에서만 한다. 반려된 파일은 결제원 계정에만 남고 올린 사람이 지우고 다시 올린다.
 */
@Service
@Transactional(readOnly = true)
public class AttachmentApprovalService {

  private static final Set<AttachmentApprovalStatus> DECIDABLE_STATUSES =
      EnumSet.of(
          AttachmentApprovalStatus.PENDING,
          AttachmentApprovalStatus.APPROVED,
          AttachmentApprovalStatus.REJECTED);

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

  /** 저장한 파일이 결재 대기면 팀장에게 알린다(알림 센터와 메일). */
  void notifyRequested(Attachment attachment) {
    if (attachment.getApprovalStatus() != AttachmentApprovalStatus.PENDING) {
      return;
    }
    AttachmentOwnerInfo owner =
        accessPolicy.describe(attachment.getOwnerType(), attachment.getOwnerId());
    notificationService.publish(
        new NotificationCommand(
            attachment.getApproverEmail(),
            NotificationType.ATTACHMENT_APPROVAL_REQUESTED,
            owner.business(),
            attachment.getUploaderName() + " 님이 파일 반출 결재를 요청했습니다",
            attachment.getOriginalName()
                + " · "
                + attachment.getOwnerType().getLabel()
                + " \""
                + owner.title()
                + "\". 승인하면 참가기관에 보입니다. 내부망 연결이에서 파일을 열어 보고 결재해 주세요.",
            approverLink(owner, attachment.getApproverEmail()),
            Set.of(NotificationChannel.EMAIL)));
  }

  /** 결재자가 들어갈 업무 영역의 결재함. 대상 업무를 맡았으면 그 업무, 아니면 맡은 업무 중 첫째. */
  private String approverLink(AttachmentOwnerInfo owner, String approverEmail) {
    Set<Business> businesses =
        accountService.findTeamLeaderOf(approverEmail).map(TeamLeader::businesses).orElse(Set.of());
    Business business =
        owner.business() != null && businesses.contains(owner.business())
            ? owner.business()
            : businesses.stream().sorted().findFirst().orElse(Business.ACCOUNT_INFO);
    return "/" + business.getSlug() + "/file-approvals";
  }

  /** 결재함. status 가 없으면 대기·승인·반려 모두. */
  public PageResponse<AttachmentApprovalResponse> list(
      CurrentUser user,
      AttachmentApprovalBox box,
      AttachmentApprovalStatus status,
      Pageable pageable) {
    ensureKftc(user);
    Set<AttachmentApprovalStatus> statuses =
        status == null || status == AttachmentApprovalStatus.NOT_REQUIRED
            ? DECIDABLE_STATUSES
            : EnumSet.of(status);
    Page<Attachment> page =
        box == AttachmentApprovalBox.REQUESTED
            ? repository.findRequestedBy(user.email(), statuses, pageable)
            : repository.findToApprove(user.email(), statuses, pageable);
    return PageResponse.from(page, this::toResponse);
  }

  /** 내가 결재할 대기 파일 수. */
  public long pendingCount(CurrentUser user) {
    if (user.organizationType() != OrganizationType.KFTC) {
      return 0;
    }
    return repository.countByApproverEmailAndApprovalStatus(
        user.email(), AttachmentApprovalStatus.PENDING);
  }

  @Transactional
  public AttachmentApprovalResponse approve(CurrentUser user, Long id) {
    Attachment attachment = loadPendingFor(user, id);
    attachment.approve(LocalDateTime.now(clock));
    notifyDecided(attachment, "파일 반출이 승인되었습니다", "이제 참가기관에서도 이 파일을 볼 수 있습니다.");
    return toResponse(attachment);
  }

  @Transactional
  public AttachmentApprovalResponse reject(CurrentUser user, Long id, String reason) {
    Attachment attachment = loadPendingFor(user, id);
    attachment.reject(reason.trim(), LocalDateTime.now(clock));
    notifyDecided(attachment, "파일 반출이 반려되었습니다", "반려 사유: " + reason.trim() + ". 파일을 고쳐 다시 올려 주세요.");
    return toResponse(attachment);
  }

  private Attachment loadPendingFor(CurrentUser user, Long id) {
    ensureKftc(user);
    networkZoneResolver.ensureInternal("파일 반출 결재");
    Attachment attachment =
        repository
            .findById(id)
            .orElseThrow(
                () -> BusinessException.notFound("ATTACHMENT_NOT_FOUND", "첨부파일을 찾을 수 없습니다."));
    if (!user.email().equals(attachment.getApproverEmail())) {
      throw new AccessDeniedException("이 파일의 결재자가 아닙니다.");
    }
    if (attachment.getApprovalStatus() != AttachmentApprovalStatus.PENDING) {
      throw BusinessException.conflict(
          "ATTACHMENT_ALREADY_DECIDED",
          "이미 " + attachment.getApprovalStatus().getLabel() + "된 파일입니다.");
    }
    return attachment;
  }

  private void notifyDecided(Attachment attachment, String title, String detail) {
    AttachmentOwnerInfo owner =
        accessPolicy.describe(attachment.getOwnerType(), attachment.getOwnerId());
    String link =
        owner.path() == null || owner.business() == null
            ? null
            : "/" + owner.business().getSlug() + "/" + owner.path();
    notificationService.publish(
        NotificationCommand.inAppOnly(
            attachment.getCreatedBy(),
            NotificationType.ATTACHMENT_APPROVAL_DECIDED,
            owner.business(),
            title,
            attachment.getOriginalName() + " (" + owner.title() + "). " + detail,
            link));
  }

  private static void ensureKftc(CurrentUser user) {
    if (user.organizationType() != OrganizationType.KFTC) {
      throw new AccessDeniedException("파일 반출 결재는 결제원 담당자만 씁니다.");
    }
  }

  private AttachmentApprovalResponse toResponse(Attachment a) {
    AttachmentOwnerInfo owner = accessPolicy.describe(a.getOwnerType(), a.getOwnerId());
    return new AttachmentApprovalResponse(
        a.getId(),
        a.getOriginalName(),
        a.getContentType(),
        a.getSizeBytes(),
        a.getOwnerType(),
        a.getOwnerType().getLabel(),
        a.getOwnerId(),
        owner.title(),
        owner.business() == null ? null : owner.business().getSlug(),
        owner.path(),
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
