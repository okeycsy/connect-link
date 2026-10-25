package com.connectlink.account;

import com.connectlink.account.dto.PasswordResetResponse;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationSender;
import com.connectlink.notification.OutboundMessage;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 변경(내 정보)과 비밀번호 찾기(로그인 화면). 찾기는 가입 때 이메일 인증과 같은 숫자 6자리 코드를 회사 이메일로 보내고, 유효 시간·틀린 횟수·재발송 간격도
 * 같은 설정({@link EmailVerificationProperties})을 쓴다. 바꾸면 그 계정의 다른 세션은 다음 요청에서 끝난다({@code
 * ActiveAccountFilter}).
 */
@Service
@Transactional
public class PasswordService {

  private static final int CODE_DIGITS = 6;

  private final UserAccountRepository accountRepository;
  private final PasswordResetRepository resetRepository;
  private final NotificationSender notificationSender;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationProperties properties;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public PasswordService(
      UserAccountRepository accountRepository,
      PasswordResetRepository resetRepository,
      NotificationSender notificationSender,
      PasswordEncoder passwordEncoder,
      EmailVerificationProperties properties,
      Clock clock) {
    this.accountRepository = accountRepository;
    this.resetRepository = resetRepository;
    this.notificationSender = notificationSender;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * 로그인한 본인의 비밀번호를 바꾼다. 지금 비밀번호가 틀리면 400 {@code PASSWORD_MISMATCH}(currentPassword 칸), 새 비밀번호가 지금과
   * 같으면 400 {@code PASSWORD_UNCHANGED}(newPassword 칸)다. 바꾼 시각을 돌려주며, 부른 쪽이 이 세션의 로그인 시각을 그것으로 고쳐 이
   * 세션은 남긴다.
   */
  public LocalDateTime change(CurrentUser user, String currentPassword, String newPassword) {
    UserAccount account =
        accountRepository
            .findByEmail(user.email())
            .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."));
    if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "currentPassword", "지금 비밀번호가 맞지 않습니다.");
    }
    if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "PASSWORD_UNCHANGED",
          "newPassword",
          "새 비밀번호가 지금 비밀번호와 같습니다. 다른 비밀번호로 정해 주세요.");
    }
    LocalDateTime now = now();
    account.changePassword(passwordEncoder.encode(newPassword), now);
    sendChangedMail(account, "내 정보에서");
    return now;
  }

  /**
   * 비밀번호 찾기 코드를 보낸다. 사용 중 계정이 있는 이메일에만 메일이 가지만, 계정이 있는지 드러나지 않게 답은 언제나 같다. 직전 코드를 보낸 지 재발송 간격이 안
   * 지났으면 429 {@code VERIFICATION_RESEND_TOO_SOON} 이다.
   */
  public PasswordResetResponse request(String rawEmail) {
    String email = AccountService.normalizeEmail(rawEmail);
    LocalDateTime now = now();
    resetRepository
        .findFirstByEmailOrderByIdDesc(email)
        .map(last -> last.getIssuedAt().plus(properties.getResendInterval()))
        .filter(now::isBefore)
        .ifPresent(
            resendAt -> {
              long seconds = (Duration.between(now, resendAt).toMillis() + 999) / 1000;
              throw BusinessException.onField(
                  HttpStatus.TOO_MANY_REQUESTS,
                  "VERIFICATION_RESEND_TOO_SOON",
                  "email",
                  "인증 코드는 " + seconds + "초 뒤에 다시 받을 수 있습니다.");
            });

    String code = newCode();
    // 계정이 없어도 한 줄을 남긴다. 확인 단계의 답(코드가 맞지 않습니다)이 계정 유무와 상관없이 같아진다.
    resetRepository.save(
        new PasswordReset(
            email, passwordEncoder.encode(code), now, now.plus(properties.getCodeTtl())));
    if (activeAccount(email).isPresent()) {
      notificationSender.send(
          new OutboundMessage(
              NotificationChannel.EMAIL,
              email,
              "[연결이] 비밀번호 찾기 인증 코드",
              resetMailBody(code, properties.getCodeTtl()),
              null));
    }
    return new PasswordResetResponse(email, properties.getResendInterval().toSeconds());
  }

  /**
   * 코드를 확인하고 새 비밀번호로 바꾼다. 틀린 횟수를 남기므로 업무 예외에도 되돌리지 않는다. 코드를 받지 않았으면 400 {@code
   * VERIFICATION_NOT_REQUESTED}, 유효 시간이 지났거나 이미 썼으면 {@code VERIFICATION_EXPIRED}, 틀리면 {@code
   * VERIFICATION_CODE_MISMATCH}, 정해진 횟수를 넘기면 {@code VERIFICATION_ATTEMPTS_EXCEEDED} 다(모두 code 칸).
   */
  @Transactional(noRollbackFor = BusinessException.class)
  public void confirm(String rawEmail, String code, String newPassword) {
    String email = AccountService.normalizeEmail(rawEmail);
    LocalDateTime now = now();
    PasswordReset reset =
        resetRepository
            .findFirstByEmailOrderByIdDesc(email)
            .orElseThrow(() -> codeError("VERIFICATION_NOT_REQUESTED", "인증 코드를 먼저 받아 주세요."));
    if (reset.isUsed() || reset.isExpired(now)) {
      throw codeError("VERIFICATION_EXPIRED", "인증 코드의 유효 시간이 지났습니다. 코드를 다시 받아 주세요.");
    }
    int maxAttempts = properties.getMaxAttempts();
    if (reset.getFailedAttempts() >= maxAttempts) {
      throw attemptsExceeded(maxAttempts);
    }
    UserAccount account = activeAccount(email).orElse(null);
    if (account == null || !passwordEncoder.matches(code, reset.getCodeHash())) {
      reset.recordFailedAttempt();
      int left = maxAttempts - reset.getFailedAttempts();
      if (left <= 0) {
        throw attemptsExceeded(maxAttempts);
      }
      throw codeError(
          "VERIFICATION_CODE_MISMATCH", "인증 코드가 맞지 않습니다. " + left + "번 더 틀리면 코드를 다시 받아야 합니다.");
    }
    reset.use(now);
    account.changePassword(passwordEncoder.encode(newPassword), now);
    sendChangedMail(account, "비밀번호 찾기로");
  }

  private Optional<UserAccount> activeAccount(String email) {
    return accountRepository.findByEmail(email).filter(a -> a.getStatus() == AccountStatus.ACTIVE);
  }

  private void sendChangedMail(UserAccount account, String how) {
    notificationSender.send(
        new OutboundMessage(
            NotificationChannel.EMAIL,
            account.getEmail(),
            "[연결이] 비밀번호가 바뀌었습니다",
            account.getName()
                + " 님의 연결이 비밀번호를 "
                + how
                + " 바꿨습니다. 다른 곳에서 로그인해 있던 화면은 다시 로그인해야 합니다.\n\n직접 바꾸지 않으셨다면 로그인 화면의 '비밀번호 찾기'로 바로"
                + " 바꾸고 결제원 담당자에게 알려 주세요.",
            null));
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  private String newCode() {
    int bound = (int) Math.pow(10, CODE_DIGITS);
    return String.format("%0" + CODE_DIGITS + "d", random.nextInt(bound));
  }

  private static String resetMailBody(String code, Duration ttl) {
    return """
        연결이 비밀번호 찾기 인증 코드입니다.

        인증 코드: %s

        %d분 안에 비밀번호 찾기 화면에 입력하고 새 비밀번호를 정해 주세요. 직접 요청하지 않았다면 이 메일은 무시해 주세요. 비밀번호는 바뀌지 않습니다.
        """
        .formatted(code, ttl.toMinutes());
  }

  private static BusinessException codeError(String code, String message) {
    return BusinessException.onField(HttpStatus.BAD_REQUEST, code, "code", message);
  }

  private static BusinessException attemptsExceeded(int maxAttempts) {
    return codeError(
        "VERIFICATION_ATTEMPTS_EXCEEDED", "인증 코드를 " + maxAttempts + "번 틀렸습니다. 코드를 다시 받아 주세요.");
  }
}
