package com.connectlink.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationOutbox;
import com.connectlink.notification.NotificationOutboxRepository;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 내 정보 > 탈퇴: 가능 여부 조회, 비밀번호 확인, 막는 규칙(마지막 관리자, 기능 모듈의 확인), 탈퇴 뒤 정보 삭제·세션 종료·확인 메일·다시 가입. 탈퇴는 이
 * 테스트에서 새로 만든 계정으로만 하고, 계정을 실제로 바꾸므로 따로 쓰는 DB 다.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:withdrawal-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
@Import(WithdrawalControllerTest.BlockingCheck.class)
class WithdrawalControllerTest {

  private static final String PASSWORD = "Welcome1!";
  private static final AtomicInteger SEQ = new AtomicInteger();

  /** 기능 모듈의 확인 대신: 이메일이 blocked 로 시작하면 후임자를 먼저 등록하라고 막는다. */
  @TestConfiguration
  static class BlockingCheck {
    @Bean
    WithdrawalCheck successorCheck() {
      return user ->
          user.email().startsWith("blocked")
              ? List.of(
                  new WithdrawalBlocker(
                      "SUCCESSOR_REQUIRED",
                      "KB국민은행 계좌통합관리 담당자가 혼자입니다. 후임자를 연락처에 먼저 등록해 주세요.",
                      "/account-info/contacts"))
              : List.of();
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserAccountRepository accountRepository;
  @Autowired private EmailVerificationRepository verificationRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NotificationOutboxRepository outboxRepository;
  @Autowired private NotificationService notificationService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private OrganizationService organizationService;
  @Autowired private ApplicationEvents events;

  /** KB국민은행 계좌통합관리 현업 담당자. 가입 승인을 거친 사용 중 계정을 새로 만들고 이메일을 돌려준다. */
  private String activeAccount(String prefix) {
    String email = prefix + SEQ.incrementAndGet() + "." + System.nanoTime() + "@kb.example.com";
    UserAccount account =
        UserAccount.apply(
            email,
            passwordEncoder.encode(PASSWORD),
            "이탈퇴",
            organizationService.getByCode("004"),
            JobRole.BUSINESS,
            Set.of(),
            Map.of(Business.ACCOUNT_INFO, "계좌정보 통합조회"),
            "디지털금융부",
            "계좌서비스팀",
            "대리",
            "1234",
            "010-0000-1234",
            LocalDateTime.now().minusDays(3));
    account.approve(TestUsers.ADMIN.email(), TestUsers.ADMIN.name(), LocalDateTime.now());
    accountRepository.save(account);
    return email;
  }

  private ResultActions perform(MockHttpServletRequestBuilder request, MockHttpSession session)
      throws Exception {
    return mockMvc.perform(request.session(session));
  }

  private MockHttpServletRequestBuilder postJson(String path, Object body) throws Exception {
    return post(path)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf());
  }

  /** 로그인 API 로 진짜 세션을 만든다. */
  private MockHttpSession login(String email) throws Exception {
    return (MockHttpSession)
        mockMvc
            .perform(postJson("/api/v1/auth/login", Map.of("email", email, "password", PASSWORD)))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  private ResultActions withdraw(MockHttpSession session, String password) throws Exception {
    return perform(postJson("/api/v1/me/withdrawal", Map.of("password", password)), session);
  }

  @Test
  void 막는_까닭이_없으면_탈퇴할_수_있다고_답한다() throws Exception {
    MockHttpSession session = login(activeAccount("free"));
    perform(get("/api/v1/me/withdrawal"), session)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(true))
        .andExpect(jsonPath("$.blockers.length()").value(0));
  }

  @Test
  void 로그인하지_않으면_탈퇴_API_는_401_이다() throws Exception {
    mockMvc.perform(get("/api/v1/me/withdrawal")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(postJson("/api/v1/me/withdrawal", Map.of("password", PASSWORD)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void 관리자가_한_명뿐이면_관리자는_탈퇴하지_못한다() throws Exception {
    mockMvc
        .perform(get("/api/v1/me/withdrawal").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(false))
        .andExpect(jsonPath("$.blockers[0].code").value("LAST_ADMIN"))
        .andExpect(jsonPath("$.blockers[0].link").doesNotExist());
    mockMvc
        .perform(
            postJson("/api/v1/me/withdrawal", Map.of("password", "demo1234!"))
                .with(TestUsers.admin()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("WITHDRAWAL_BLOCKED"))
        .andExpect(jsonPath("$.detail").value(containsString("관리자가 한 명뿐")));
    assertThat(accountRepository.findByEmail(TestUsers.ADMIN.email()).orElseThrow().getStatus())
        .isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  void 기능_모듈의_확인이_막으면_까닭과_화면_주소를_보여_주고_탈퇴도_막는다() throws Exception {
    String email = activeAccount("blocked");
    MockHttpSession session = login(email);
    perform(get("/api/v1/me/withdrawal"), session)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(false))
        .andExpect(jsonPath("$.blockers[0].code").value("SUCCESSOR_REQUIRED"))
        .andExpect(jsonPath("$.blockers[0].link").value("/account-info/contacts"));
    withdraw(session, PASSWORD)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("WITHDRAWAL_BLOCKED"))
        .andExpect(jsonPath("$.detail").value(startsWith("KB국민은행 계좌통합관리")));
    assertThat(accountRepository.findByEmail(email).orElseThrow().getStatus())
        .isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  void 비밀번호가_틀리거나_비면_탈퇴하지_않고_그_칸에_알린다() throws Exception {
    String email = activeAccount("wrong");
    MockHttpSession session = login(email);
    withdraw(session, "Wrong123!")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("비밀번호가 맞지 않습니다."));
    withdraw(session, " ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("비밀번호를 입력해 주세요."));
    assertThat(accountRepository.findByEmail(email).orElseThrow().getStatus())
        .isEqualTo(AccountStatus.ACTIVE);
    perform(get("/api/v1/me"), session).andExpect(status().isOk());
  }

  @Test
  void 탈퇴하면_계정_정보와_알림을_지우고_모든_세션을_끝내며_확인_메일을_보낸다() throws Exception {
    String email = activeAccount("leave");
    Long id = accountRepository.findByEmail(email).orElseThrow().getId();
    MockHttpSession here = login(email);
    MockHttpSession elsewhere = login(email);
    notificationService.publish(
        new NotificationCommand(
            email,
            NotificationType.SYSTEM,
            Business.ACCOUNT_INFO,
            "점검 안내",
            "주말 점검이 있습니다.",
            null,
            Set.of(NotificationChannel.EMAIL)));
    perform(get("/api/v1/notifications/unread-count"), here).andExpect(status().isOk());

    withdraw(here, PASSWORD).andExpect(status().isNoContent());

    // 이 세션도, 다른 브라우저의 세션도 끝났다.
    perform(get("/api/v1/me"), here).andExpect(status().isUnauthorized());
    perform(get("/api/v1/me"), elsewhere).andExpect(status().isUnauthorized());
    assertThat(elsewhere.isInvalid()).isTrue();
    // 같은 이메일·비밀번호로 로그인할 수 없다.
    mockMvc
        .perform(postJson("/api/v1/auth/login", Map.of("email", email, "password", PASSWORD)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

    // 계정 행은 남고 개인 정보는 지워진다.
    UserAccount withdrawn = accountRepository.findById(id).orElseThrow();
    assertThat(withdrawn.getStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    assertThat(withdrawn.getEmail()).isEqualTo("withdrawn-" + id + "@withdrawn.invalid");
    assertThat(withdrawn.getName()).isEqualTo("탈퇴한 사용자");
    assertThat(withdrawn.getMobilePhone()).isNull();
    assertThat(withdrawn.getExtensionPhone()).isEmpty();
    assertThat(withdrawn.getDepartment()).isEmpty();
    assertThat(withdrawn.getWithdrawnAt()).isNotNull();
    assertThat(withdrawn.getReviewerName()).isEqualTo(TestUsers.ADMIN.name());
    assertThat(accountRepository.findByEmail(email)).isEmpty();
    // 알림 센터 알림은 지우고, 발송함 기록은 연결만 끊고 남긴다.
    assertThat(notificationRepository.findIdsByRecipientEmail(email)).isEmpty();
    List<NotificationOutbox> sent =
        outboxRepository.findTop5ByChannelAndRecipientOrderByIdDesc(
            NotificationChannel.EMAIL, email);
    assertThat(sent)
        .extracting(NotificationOutbox::getSubject)
        .containsExactly("[연결고리] 탈퇴가 완료되었습니다", "점검 안내");
    assertThat(sent).extracting(NotificationOutbox::getNotificationId).containsOnlyNulls();
    assertThat(sent.get(0).getBody()).contains("KB국민은행 이탈퇴 님의 연결고리 탈퇴가 완료되었습니다");
    // 기능 모듈이 자기 데이터를 지우도록 탈퇴 전 이메일과 담당 업무를 알린다.
    assertThat(events.stream(AccountWithdrawnEvent.class))
        .containsExactly(
            new AccountWithdrawnEvent(id, email, "004", Set.of(Business.ACCOUNT_INFO), null));

    // 같은 회사 이메일로 다시 가입할 수 있다(인증 코드를 새로 받는다).
    mockMvc
        .perform(postJson("/api/v1/email-verifications", Map.of("email", email)))
        .andExpect(status().isCreated());
    assertThat(verificationRepository.findFirstByEmailOrderByIdDesc(email)).isPresent();
  }

  @Test
  void 탈퇴한_계정의_세션으로는_탈퇴를_다시_할_수_없다() throws Exception {
    String email = activeAccount("twice");
    MockHttpSession first = login(email);
    MockHttpSession second = login(email);
    withdraw(first, PASSWORD).andExpect(status().isNoContent());
    withdraw(second, PASSWORD).andExpect(status().isUnauthorized());
  }

  /** 세션 확인은 사용 중 계정의 요청을 그대로 둔다(위 테스트의 거부와 짝인 허용). */
  @Test
  void 사용_중_계정의_요청은_세션_확인을_그대로_지나간다() throws Exception {
    CurrentUser member = TestUsers.GANA_MEMBER;
    mockMvc
        .perform(get("/api/v1/me").with(TestUsers.as(member)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(member.email()));
  }

  // 결제원 담당자의 탈퇴 처리

  private Long idOf(String email) {
    return accountRepository.findByEmail(email).orElseThrow().getId();
  }

  private ResultActions withdrawByStaff(Long id, String reason, CurrentUser actor)
      throws Exception {
    return mockMvc.perform(
        postJson("/api/v1/accounts/" + id + "/withdrawal", Map.of("reason", reason))
            .with(TestUsers.as(actor)));
  }

  @Test
  void 결제원_담당자는_맡은_업무의_참가기관_담당자를_사유와_함께_탈퇴_처리한다() throws Exception {
    String email = activeAccount("staff");
    Long id = idOf(email);
    MockHttpSession target = login(email);
    mockMvc
        .perform(
            get("/api/v1/accounts/" + id + "/withdrawal")
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(true));

    withdrawByStaff(id, "  인사이동으로 담당이 끝났습니다.  ", TestUsers.KFTC_ACCOUNT_INFO)
        .andExpect(status().isNoContent());

    // 탈퇴 처리된 사람의 세션은 다음 요청에서 끝난다.
    perform(get("/api/v1/me"), target).andExpect(status().isUnauthorized());
    UserAccount withdrawn = accountRepository.findById(id).orElseThrow();
    assertThat(withdrawn.getStatus()).isEqualTo(AccountStatus.WITHDRAWN);
    assertThat(withdrawn.getName()).isEqualTo(UserAccount.WITHDRAWN_NAME);
    assertThat(withdrawn.getWithdrawnByName()).isEqualTo("홍성민");
    assertThat(withdrawn.getWithdrawnByEmail()).isEqualTo(TestUsers.KFTC_ACCOUNT_INFO.email());
    assertThat(withdrawn.getWithdrawalReason()).isEqualTo("인사이동으로 담당이 끝났습니다.");
    NotificationOutbox mail =
        outboxRepository
            .findTop5ByChannelAndRecipientOrderByIdDesc(NotificationChannel.EMAIL, email)
            .get(0);
    assertThat(mail.getSubject()).isEqualTo("[연결고리] 계정이 탈퇴 처리되었습니다");
    assertThat(mail.getBody())
        .contains("결제원 담당자 홍성민 님이 탈퇴 처리했습니다")
        .contains("사유: 인사이동으로 담당이 끝났습니다.");
    assertThat(events.stream(AccountWithdrawnEvent.class))
        .containsExactly(
            new AccountWithdrawnEvent(
                id,
                email,
                "004",
                Set.of(Business.ACCOUNT_INFO),
                TestUsers.KFTC_ACCOUNT_INFO.email()));

    // 이미 탈퇴한 계정은 다시 처리하지 않는다.
    withdrawByStaff(id, "다시", TestUsers.KFTC_ACCOUNT_INFO)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
  }

  @Test
  void 다른_업무만_맡은_결제원_담당자와_참가기관_담당자는_탈퇴_처리하지_못한다() throws Exception {
    Long id = idOf(activeAccount("deny"));
    withdrawByStaff(id, "사유", TestUsers.KFTC_CMS).andExpect(status().isForbidden());
    withdrawByStaff(id, "사유", TestUsers.GANA_MEMBER).andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/v1/accounts/" + id + "/withdrawal").with(TestUsers.as(TestUsers.KFTC_CMS)))
        .andExpect(status().isForbidden());
    assertThat(accountRepository.findById(id).orElseThrow().getStatus())
        .isEqualTo(AccountStatus.ACTIVE);
  }

  @Test
  void 관리자는_업무와_상관없이_탈퇴_처리하고_결제원_계정은_처리하지_못한다() throws Exception {
    Long id = idOf(activeAccount("admin"));
    withdrawByStaff(id, "퇴사", TestUsers.ADMIN).andExpect(status().isNoContent());
    withdrawByStaff(idOf(TestUsers.KFTC_CMS.email()), "사유", TestUsers.ADMIN)
        .andExpect(status().isForbidden());
  }

  @Test
  void 탈퇴_처리_사유가_없거나_승인_대기_계정이면_처리하지_않는다() throws Exception {
    Long id = idOf(activeAccount("reason"));
    withdrawByStaff(id, " ", TestUsers.KFTC_ACCOUNT_INFO)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("탈퇴 처리 사유를 적어 주세요."));
    withdrawByStaff(idOf("han.yerin@kb.example.com"), "사유", TestUsers.KFTC_ACCOUNT_INFO)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
    mockMvc
        .perform(
            post("/api/v1/accounts/999999/withdrawal")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"사유\"}")
                .with(csrf())
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO)))
        .andExpect(status().isNotFound());
  }

  @Test
  void 기능_모듈의_확인은_탈퇴_처리도_막는다() throws Exception {
    Long id = idOf(activeAccount("blocked"));
    mockMvc
        .perform(
            get("/api/v1/accounts/" + id + "/withdrawal")
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(false))
        .andExpect(jsonPath("$.blockers[0].code").value("SUCCESSOR_REQUIRED"));
    withdrawByStaff(id, "사유", TestUsers.KFTC_ACCOUNT_INFO)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("WITHDRAWAL_BLOCKED"));
  }
}
