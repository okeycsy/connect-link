package com.connectlink.account;

import com.connectlink.account.dto.SignUpApplicationBusinessResponse;
import com.connectlink.account.dto.SignUpApplicationCountsResponse;
import com.connectlink.account.dto.SignUpApplicationResponse;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationSender;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import com.connectlink.notification.OutboundMessage;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가입 승인. 신청 목록 조회, 승인·반려, 승인자에게 가는 "가입 신청" 알림을 맡는다. 누가 처리하는지는 {@link SignUpReviewAccessPolicy} 가
 * 정한다. 승인 메일은 알림 센터 알림과 함께 메일로, 반려 메일은 로그인할 수 없는 사람에게 가므로 메일로만 보낸다.
 */
@Service
@Transactional
public class SignUpReviewService {

  /** 최근 신청이 위로 온다. 신청 일시가 같으면 나중에 만든 계정이 위다. */
  private static final Sort LATEST_FIRST =
      Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.desc("id"));

  private final UserAccountRepository repository;
  private final SignUpReviewAccessPolicy accessPolicy;
  private final NotificationService notificationService;
  private final NotificationSender notificationSender;
  private final Clock clock;

  public SignUpReviewService(
      UserAccountRepository repository,
      SignUpReviewAccessPolicy accessPolicy,
      NotificationService notificationService,
      NotificationSender notificationSender,
      Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.notificationService = notificationService;
    this.notificationSender = notificationSender;
    this.clock = clock;
  }

  /** 한 업무 영역의 가입 신청 중 한 상태. 승인 대기가 아니면 가입 승인을 거친 계정만 나온다. */
  @Transactional(readOnly = true)
  public PageResponse<SignUpApplicationResponse> list(
      CurrentUser user, Business business, AccountStatus status, Pageable pageable) {
    accessPolicy.ensureCanReview(user, business);
    return PageResponse.from(
        repository.findApplications(
            business,
            status,
            reviewedOnly(status),
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LATEST_FIRST)),
        SignUpReviewService::toResponse);
  }

  @Transactional(readOnly = true)
  public SignUpApplicationCountsResponse counts(CurrentUser user, Business business) {
    accessPolicy.ensureCanReview(user, business);
    return new SignUpApplicationCountsResponse(
        repository.countApplications(business, AccountStatus.PENDING_APPROVAL, false),
        repository.countApplications(business, AccountStatus.ACTIVE, true),
        repository.countApplications(business, AccountStatus.REJECTED, true));
  }

  @Transactional(readOnly = true)
  public SignUpApplicationResponse get(CurrentUser user, Long id) {
    UserAccount account = find(id);
    accessPolicy.ensureCanReview(user, account.getBusinesses());
    return toResponse(account);
  }

  /** 승인한다. 신청자에게 알림 센터 알림과 승인 메일이 간다. 승인 대기가 아니면 409 다. */
  public SignUpApplicationResponse approve(CurrentUser user, Long id) {
    UserAccount account = findPending(user, id);
    account.approve(user.email(), user.name(), now());
    Business home = account.getBusinesses().iterator().next();
    notificationService.publish(
        new NotificationCommand(
            account.getEmail(),
            NotificationType.SIGNUP_APPROVED,
            home,
            "연결고리 가입이 승인되었습니다",
            account.getOrganization().getName()
                + " "
                + account.getName()
                + " 님, 연결고리 가입이 승인되었습니다. 회사 이메일과 가입 때 정한 비밀번호로 로그인할 수 있습니다. 담당 업무: "
                + labelsOf(account.getBusinesses())
                + ".",
            "/" + home.getSlug() + "/home",
            Set.of(NotificationChannel.EMAIL)));
    return toResponse(account);
  }

  /** 반려한다. 신청자에게 사유를 담은 반려 메일이 간다. 반려된 이메일은 다시 가입 신청할 수 있다. 승인 대기가 아니면 409 다. */
  public SignUpApplicationResponse reject(CurrentUser user, Long id, String reason) {
    UserAccount account = findPending(user, id);
    String trimmed = reason.trim();
    account.reject(user.email(), user.name(), trimmed, now());
    notificationSender.send(
        new OutboundMessage(
            NotificationChannel.EMAIL,
            account.getEmail(),
            "[연결고리] 가입 신청이 반려되었습니다",
            account.getOrganization().getName()
                + " "
                + account.getName()
                + " 님의 연결고리 가입 신청이 반려되었습니다.\n\n반려 사유: "
                + trimmed
                + "\n\n신청 내용을 고쳐 같은 회사 이메일로 다시 가입 신청할 수 있습니다. 궁금한 점은 결제원 담당자 "
                + user.name()
                + " 님에게 문의해 주세요.",
            null));
    return toResponse(account);
  }

  /**
   * 가입 신청이 들어왔다고 승인할 수 있는 사람(관리자와 신청 업무를 맡은 결제원 담당자) 모두에게 알림 센터 알림을 남긴다. 메일·매터모스트로 보낼지는 알림 대상 설정이
   * 생기면 그것을 따른다(docs/decisions.md).
   */
  void notifyReviewers(UserAccount applicant) {
    Set<Business> requested = applicant.getBusinesses();
    String title =
        applicant.getOrganization().getName() + " " + applicant.getName() + " 님이 가입을 신청했습니다";
    String body =
        labelsOf(requested)
            + " · "
            + applicant.getJobRole().describe(applicant.getItRoles())
            + " 담당자. "
            + (applicant.emailDomainMatches()
                ? "이메일 도메인이 기관과 일치합니다."
                : "이메일 도메인이 기관과 다릅니다. 소속을 확인해 주세요.");
    for (UserAccount candidate : repository.findByStatus(AccountStatus.ACTIVE)) {
      CurrentUser reviewer = AccountService.toCurrentUser(candidate);
      if (!accessPolicy.canReview(reviewer, requested)) {
        continue;
      }
      Business business =
          requested.stream()
              .filter(b -> accessPolicy.canReview(reviewer, b))
              .findFirst()
              .orElseThrow();
      notificationService.publish(
          NotificationCommand.inAppOnly(
              reviewer.email(),
              NotificationType.SIGNUP_REQUESTED,
              business,
              title,
              body,
              linkOf(business, applicant.getId())));
    }
  }

  /** 가입 승인 화면에서 이 신청의 상세 패널을 여는 주소. */
  static String linkOf(Business business, Long accountId) {
    return "/" + business.getSlug() + "/admin/sign-ups?id=" + accountId;
  }

  private UserAccount find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("SIGN_UP_NOT_FOUND", "가입 신청을 찾을 수 없습니다."));
  }

  private UserAccount findPending(CurrentUser user, Long id) {
    UserAccount account = find(id);
    accessPolicy.ensureCanReview(user, account.getBusinesses());
    if (account.getStatus() != AccountStatus.PENDING_APPROVAL) {
      throw BusinessException.conflict(
          "SIGN_UP_ALREADY_REVIEWED", "이미 처리한 가입 신청입니다(" + account.getStatus().getLabel() + ").");
    }
    return account;
  }

  private static boolean reviewedOnly(AccountStatus status) {
    return status != AccountStatus.PENDING_APPROVAL;
  }

  private static String labelsOf(Set<Business> businesses) {
    return businesses.stream().map(Business::getLabel).collect(Collectors.joining(", "));
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  static SignUpApplicationResponse toResponse(UserAccount account) {
    Organization organization = account.getOrganization();
    List<SignUpApplicationBusinessResponse> businesses =
        account.getBusinessDetails().stream()
            .map(
                b ->
                    new SignUpApplicationBusinessResponse(
                        b.getBusiness(),
                        b.getBusiness().getSlug(),
                        b.getBusiness().getLabel(),
                        b.getBusiness().getShortLabel(),
                        b.getSubBusiness()))
            .toList();
    return new SignUpApplicationResponse(
        account.getId(),
        account.getEmail(),
        account.getName(),
        organization.getId(),
        organization.getName(),
        organization.getType(),
        organization.getType().getLabel(),
        organization.getEmailDomain(),
        account.emailDomainMatches(),
        businesses,
        account.getJobRole(),
        account.getJobRole().describe(account.getItRoles()),
        account.getDepartment(),
        account.getTeam(),
        account.getJobTitle(),
        account.getExtensionPhone(),
        account.getMobilePhone(),
        account.getStatus(),
        account.getStatus().getLabel(),
        account.getAppliedAt(),
        account.getReviewedAt(),
        account.getReviewerName(),
        account.getRejectionReason());
  }
}
