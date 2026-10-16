package com.connectlink.account;

import com.connectlink.account.dto.EmailVerificationConfirmResponse;
import com.connectlink.account.dto.EmailVerificationResponse;
import com.connectlink.common.BusinessException;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationSender;
import com.connectlink.notification.OutboundMessage;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가입 때 회사 이메일 인증. 숫자 6자리 코드를 메일로 보내고({@link NotificationSender}, 프로토타입은 발송함 기록), 맞는 코드를 넣으면 가입 신청에
 * 쓸 인증 토큰을 내준다. 유효 시간·틀린 횟수·재발송 간격은 {@link EmailVerificationProperties}.
 */
@Service
@Transactional
public class EmailVerificationService {

  private static final int CODE_DIGITS = 6;

  private final EmailVerificationRepository repository;
  private final UserAccountRepository accountRepository;
  private final OrganizationService organizationService;
  private final NotificationSender notificationSender;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationProperties properties;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public EmailVerificationService(
      EmailVerificationRepository repository,
      UserAccountRepository accountRepository,
      OrganizationService organizationService,
      NotificationSender notificationSender,
      PasswordEncoder passwordEncoder,
      EmailVerificationProperties properties,
      Clock clock) {
    this.repository = repository;
    this.accountRepository = accountRepository;
    this.organizationService = organizationService;
    this.notificationSender = notificationSender;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  /** 인증 코드를 새로 보낸다. 이미 계정이 있는 이메일이면 409, 직전 코드를 보낸 지 재발송 간격이 안 지났으면 429 다. */
  public EmailVerificationResponse request(String rawEmail) {
    String email = AccountService.normalizeEmail(rawEmail);
    ensureNotRegistered(email);
    LocalDateTime now = now();
    repository
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
    EmailVerification verification =
        repository.save(
            new EmailVerification(
                email, passwordEncoder.encode(code), now, now.plus(properties.getCodeTtl())));
    notificationSender.send(
        new OutboundMessage(
            NotificationChannel.EMAIL,
            email,
            "[연결고리] 회사 이메일 인증 코드",
            mailBody(code, properties.getCodeTtl()),
            null));
    return new EmailVerificationResponse(
        email, verification.getExpiresAt(), now.plus(properties.getResendInterval()));
  }

  /** 받은 코드를 확인하고 인증 토큰을 내준다. 틀리면 틀린 횟수를 남기므로(그래서 업무 예외에도 되돌리지 않는다) 정해진 횟수를 넘기면 그 코드는 더 쓸 수 없다. */
  @Transactional(noRollbackFor = BusinessException.class)
  public EmailVerificationConfirmResponse confirm(String rawEmail, String code) {
    String email = AccountService.normalizeEmail(rawEmail);
    LocalDateTime now = now();
    EmailVerification verification =
        repository
            .findFirstByEmailOrderByIdDesc(email)
            .orElseThrow(() -> codeError("VERIFICATION_NOT_REQUESTED", "인증 코드를 먼저 받아 주세요."));
    if (verification.isCodeExpired(now)) {
      throw codeError("VERIFICATION_EXPIRED", "인증 코드의 유효 시간이 지났습니다. 코드를 다시 받아 주세요.");
    }
    int maxAttempts = properties.getMaxAttempts();
    if (verification.getFailedAttempts() >= maxAttempts) {
      throw attemptsExceeded(maxAttempts);
    }
    if (!passwordEncoder.matches(code, verification.getCodeHash())) {
      verification.recordFailedAttempt();
      int left = maxAttempts - verification.getFailedAttempts();
      if (left <= 0) {
        throw attemptsExceeded(maxAttempts);
      }
      throw codeError(
          "VERIFICATION_CODE_MISMATCH", "인증 코드가 맞지 않습니다. " + left + "번 더 틀리면 코드를 다시 받아야 합니다.");
    }

    String token = newToken();
    verification.verify(sha256(token), now, now.plus(properties.getTokenTtl()));
    return new EmailVerificationConfirmResponse(
        email, token, verification.getTokenExpiresAt(), suggestedOrganizationIds(email));
  }

  /**
   * 가입 신청에 인증 토큰을 쓴다. 그 이메일로 인증했고, 아직 안 썼고, 유효 시간 안이어야 한다. 가입 신청과 같은 트랜잭션이라 신청이 실패하면 토큰도 쓰지 않은 채로
   * 남는다.
   */
  void consume(String token, String email) {
    LocalDateTime now = now();
    EmailVerification verification =
        repository
            .findByTokenHash(sha256(token))
            .filter(v -> v.getEmail().equals(email) && v.isTokenUsable(now))
            .orElseThrow(
                () ->
                    BusinessException.onField(
                        HttpStatus.BAD_REQUEST,
                        "VERIFICATION_REQUIRED",
                        "email",
                        "이메일 인증이 끝나지 않았거나 유효 시간이 지났습니다. 이메일 인증을 다시 해 주세요."));
    verification.consume(now);
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. 유효 시각 경계가 저장할 때 반올림돼 어긋나지 않게 한다. */
  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  void ensureNotRegistered(String email) {
    if (accountRepository.existsByEmail(email)) {
      throw BusinessException.onField(
          HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "email", "이미 가입했거나 가입을 신청한 이메일입니다.");
    }
  }

  /** 이메일 도메인이 같은 기관 중 가입할 수 있는 기관(이용기관은 아직 가입을 받지 않는다). */
  private List<Long> suggestedOrganizationIds(String email) {
    String domain = email.substring(email.indexOf('@') + 1);
    return organizationService.findActiveByEmailDomain(domain).stream()
        .filter(o -> o.getType() != OrganizationType.USE_ORGANIZATION)
        .map(Organization::getId)
        .sorted()
        .toList();
  }

  private String newCode() {
    int bound = (int) Math.pow(10, CODE_DIGITS);
    return String.format("%0" + CODE_DIGITS + "d", random.nextInt(bound));
  }

  private String newToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 을 쓸 수 없습니다.", e);
    }
  }

  private static String mailBody(String code, Duration ttl) {
    return """
        연결고리 가입을 위한 회사 이메일 인증 코드입니다.

        인증 코드: %s

        %d분 안에 가입 화면에 입력해 주세요. 직접 요청하지 않았다면 이 메일은 무시해 주세요.
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
