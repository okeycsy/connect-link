package com.connectlink.account;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.notification.MailSubject;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationSender;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.OutboundMessage;
import com.connectlink.organization.Business;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 탈퇴. 본인이 비밀번호를 다시 넣어 탈퇴하거나, 참가기관 담당자의 업무를 맡은 결제원 담당자·관리자가 사유를 적어 탈퇴 처리한다({@link
 * WithdrawalAccessPolicy}). 탈퇴를 막는 규칙은 {@link WithdrawalCheck} 빈들이 정하고(계정 모듈은 마지막 관리자, 연락처 모듈은 후임자
 * 규칙), 탈퇴하면 계정의 개인 정보와 알림 센터 알림, 이메일 인증 기록을 지우고 탈퇴 메일을 보낸다. 기능 모듈은 {@link AccountWithdrawnEvent} 로
 * 자기 데이터를 함께 지운다.
 */
@Service
@Transactional
public class WithdrawalService {

  private final UserAccountRepository repository;
  private final EmailVerificationRepository verificationRepository;
  private final PasswordResetRepository passwordResetRepository;
  private final List<WithdrawalCheck> checks;
  private final WithdrawalAccessPolicy accessPolicy;
  private final NotificationService notificationService;
  private final NotificationSender notificationSender;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public WithdrawalService(
      UserAccountRepository repository,
      EmailVerificationRepository verificationRepository,
      PasswordResetRepository passwordResetRepository,
      List<WithdrawalCheck> checks,
      WithdrawalAccessPolicy accessPolicy,
      NotificationService notificationService,
      NotificationSender notificationSender,
      PasswordEncoder passwordEncoder,
      ApplicationEventPublisher events,
      Clock clock) {
    this.repository = repository;
    this.verificationRepository = verificationRepository;
    this.passwordResetRepository = passwordResetRepository;
    this.checks = checks;
    this.accessPolicy = accessPolicy;
    this.notificationService = notificationService;
    this.notificationSender = notificationSender;
    this.passwordEncoder = passwordEncoder;
    this.events = events;
    this.clock = clock;
  }

  /** 이 사용자의 탈퇴를 막는 까닭 전부. 비어 있으면 탈퇴할 수 있다. */
  @Transactional(readOnly = true)
  public List<WithdrawalBlocker> blockers(CurrentUser user) {
    return checks.stream().flatMap(check -> check.check(user).stream()).toList();
  }

  /**
   * 결제원 담당자가 이 계정을 탈퇴 처리할 때 막는 까닭. 처리할 수 없는 사람이면 403, 사용 중 계정이 아니면 409 {@code ACCOUNT_NOT_ACTIVE}
   * 다.
   */
  @Transactional(readOnly = true)
  public List<WithdrawalBlocker> blockersFor(CurrentUser actor, Long accountId) {
    return blockers(AccountService.toCurrentUser(findTarget(actor, accountId)));
  }

  /**
   * 본인 탈퇴. 막는 까닭이 있으면 409 {@code WITHDRAWAL_BLOCKED}, 비밀번호가 틀리면 400 {@code
   * PASSWORD_MISMATCH}(password 칸)다. 세션은 부른 쪽(컨트롤러)이 끝낸다.
   */
  public void withdraw(CurrentUser user, String password) {
    UserAccount account =
        repository
            .findByEmail(user.email())
            .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."));
    ensureNotBlocked(user);
    if (!passwordEncoder.matches(password, account.getPasswordHash())) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "password", "비밀번호가 맞지 않습니다.");
    }
    String organizationName = account.getOrganization().getName();
    String name = account.getName();
    complete(
        account,
        null,
        MailSubject.service("탈퇴가 완료되었습니다"),
        organizationName
            + " "
            + name
            + " 님의 연결이 탈퇴가 완료되었습니다.\n\n이름, 부서, 연락처, 담당 업무 같은 계정 정보는 지웠고 이 계정으로는 더 로그인할 수"
            + " 없습니다. 같은 회사 이메일로 다시 가입 신청할 수 있습니다.\n\n직접 탈퇴하지 않으셨다면 결제원 담당자에게 바로 알려 주세요.",
        null);
  }

  /**
   * 결제원 담당자·관리자의 탈퇴 처리. 탈퇴 처리된 담당자에게 사유를 담은 메일이 가고, 그 사람의 세션은 다음 요청에서 끝난다. 처리할 수 없는 사람이면 403, 사용 중
   * 계정이 아니면 409 {@code ACCOUNT_NOT_ACTIVE}, 막는 까닭이 있으면 409 {@code WITHDRAWAL_BLOCKED} 다.
   */
  public void withdrawByStaff(CurrentUser actor, Long accountId, String reason) {
    UserAccount account = findTarget(actor, accountId);
    ensureNotBlocked(AccountService.toCurrentUser(account));
    String trimmed = reason.trim();
    complete(
        account,
        actor,
        MailSubject.service("계정이 탈퇴 처리되었습니다"),
        account.getOrganization().getName()
            + " "
            + account.getName()
            + " 님의 연결이 계정을 결제원 담당자 "
            + actor.name()
            + " 님이 탈퇴 처리했습니다.\n\n사유: "
            + trimmed
            + "\n\n이름, 부서, 연락처, 담당 업무 같은 계정 정보는 지웠고 이 계정으로는 더 로그인할 수 없습니다. 계속 담당하신다면 같은 회사 이메일로 다시"
            + " 가입 신청해 주세요. 궁금한 점은 "
            + actor.name()
            + " 님에게 문의해 주세요.",
        trimmed);
  }

  private UserAccount findTarget(CurrentUser actor, Long accountId) {
    UserAccount account =
        repository
            .findById(accountId)
            .orElseThrow(() -> BusinessException.notFound("ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
    if (account.getStatus() != AccountStatus.ACTIVE) {
      if (account.getStatus() == AccountStatus.WITHDRAWN) {
        throw BusinessException.conflict("ACCOUNT_NOT_ACTIVE", "이미 탈퇴한 계정입니다.");
      }
      throw BusinessException.conflict(
          "ACCOUNT_NOT_ACTIVE", "사용 중인 계정만 탈퇴 처리할 수 있습니다(" + account.getStatus().getLabel() + ").");
    }
    accessPolicy.ensureCanWithdraw(actor, account);
    return account;
  }

  private void ensureNotBlocked(CurrentUser account) {
    List<WithdrawalBlocker> blockers = blockers(account);
    if (!blockers.isEmpty()) {
      throw BusinessException.conflict(
          "WITHDRAWAL_BLOCKED",
          blockers.stream().map(WithdrawalBlocker::message).collect(Collectors.joining(" ")));
    }
  }

  /**
   * 탈퇴를 마친다: 개인 정보를 지우고, 알림·인증 기록을 지우고, 기능 모듈에 알리고, 원래 회사 이메일로 메일을 보낸다.
   *
   * @param actor 탈퇴 처리한 결제원 담당자. 본인 탈퇴면 null
   */
  private void complete(
      UserAccount account, CurrentUser actor, String subject, String body, String reason) {
    String email = account.getEmail();
    String organizationCode = account.getOrganization().getCode();
    Set<Business> businesses = account.getBusinesses();
    account.withdraw(
        now(), actor == null ? null : actor.email(), actor == null ? null : actor.name(), reason);

    verificationRepository.deleteByEmail(email);
    passwordResetRepository.deleteByEmail(email);
    notificationService.deleteAllFor(email);
    events.publishEvent(
        new AccountWithdrawnEvent(
            account.getId(),
            email,
            organizationCode,
            businesses,
            actor == null ? null : actor.email()));
    notificationSender.send(
        new OutboundMessage(NotificationChannel.EMAIL, email, subject, body, null));
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }
}
