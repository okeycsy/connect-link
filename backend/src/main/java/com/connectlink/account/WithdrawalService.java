package com.connectlink.account;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
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
 * 탈퇴. 본인이 비밀번호를 다시 넣어 탈퇴한다. 탈퇴를 막는 규칙은 {@link WithdrawalCheck} 빈들이 정하고(계정 모듈은 마지막 관리자, 연락처 모듈은 후임자
 * 규칙), 탈퇴하면 계정의 개인 정보와 알림 센터 알림, 이메일 인증 기록을 지우고 탈퇴 확인 메일을 보낸다. 기능 모듈은 {@link AccountWithdrawnEvent}
 * 로 자기 데이터를 함께 지운다.
 */
@Service
@Transactional
public class WithdrawalService {

  private final UserAccountRepository repository;
  private final EmailVerificationRepository verificationRepository;
  private final List<WithdrawalCheck> checks;
  private final NotificationService notificationService;
  private final NotificationSender notificationSender;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public WithdrawalService(
      UserAccountRepository repository,
      EmailVerificationRepository verificationRepository,
      List<WithdrawalCheck> checks,
      NotificationService notificationService,
      NotificationSender notificationSender,
      PasswordEncoder passwordEncoder,
      ApplicationEventPublisher events,
      Clock clock) {
    this.repository = repository;
    this.verificationRepository = verificationRepository;
    this.checks = checks;
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
   * 탈퇴한다. 막는 까닭이 있으면 409 {@code WITHDRAWAL_BLOCKED}, 비밀번호가 틀리면 400 {@code
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
    List<WithdrawalBlocker> blockers = blockers(user);
    if (!blockers.isEmpty()) {
      throw BusinessException.conflict(
          "WITHDRAWAL_BLOCKED",
          blockers.stream().map(WithdrawalBlocker::message).collect(Collectors.joining(" ")));
    }
    if (!passwordEncoder.matches(password, account.getPasswordHash())) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "password", "비밀번호가 맞지 않습니다.");
    }

    String email = account.getEmail();
    String name = account.getName();
    String organizationName = account.getOrganization().getName();
    Set<Business> businesses = account.getBusinesses();
    account.withdraw(now());

    verificationRepository.deleteByEmail(email);
    notificationService.deleteAllFor(email);
    events.publishEvent(
        new AccountWithdrawnEvent(
            account.getId(), email, account.getOrganization().getCode(), businesses));
    notificationSender.send(
        new OutboundMessage(
            NotificationChannel.EMAIL,
            email,
            "[연결고리] 탈퇴가 완료되었습니다",
            organizationName
                + " "
                + name
                + " 님의 연결고리 탈퇴가 완료되었습니다.\n\n이름, 부서, 연락처, 담당 업무 같은 계정 정보는 지웠고 이 계정으로는 더 로그인할 수"
                + " 없습니다. 같은 회사 이메일로 다시 가입 신청할 수 있습니다.\n\n직접 탈퇴하지 않으셨다면 결제원 담당자에게 바로 알려 주세요.",
            null));
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }
}
