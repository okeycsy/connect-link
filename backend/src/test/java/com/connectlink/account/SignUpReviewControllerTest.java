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
import com.connectlink.common.ClockConfig;
import com.connectlink.common.MutableClock;
import com.connectlink.notification.Notification;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.OrganizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 관리 > 가입 승인: 신청 목록·건수·상세, 승인·반려, 승인자 알림, 반려 뒤 다시 신청. 시드의 계좌통합관리 신청(승인 대기 한예린·오승현, 승인 김민준·최유진, 반려
 * 서민재)은 읽기만 하고, 승인·반려 테스트는 자동이체통합관리만 신청한 새 계정을 만들어 쓴다. 계정과 발송함을 실제로 쌓으므로 따로 쓰는 DB 다.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:sign-up-review-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SignUpReviewControllerTest.ClockOverride.class)
class SignUpReviewControllerTest {

  @TestConfiguration
  static class ClockOverride {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(ClockConfig.SERVER_ZONE);
    }
  }

  private static final Pattern CODE = Pattern.compile("인증 코드: (\\d{6})");
  private static final AtomicInteger SEQ = new AtomicInteger();

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private MutableClock clock;
  @Autowired private OrganizationService organizationService;
  @Autowired private UserAccountRepository accountRepository;
  @Autowired private NotificationRepository notificationRepository;

  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  private static RequestPostProcessor moon() {
    return TestUsers.as(TestUsers.KFTC_AUTO_TRANSFER);
  }

  private static RequestPostProcessor park() {
    return TestUsers.as(TestUsers.KFTC_CMS);
  }

  private ResultActions postJson(String path, Object body, RequestPostProcessor user)
      throws Exception {
    var request =
        post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .with(csrf());
    return mockMvc.perform(user == null ? request : request.with(user));
  }

  private JsonNode json(ResultActions result) throws Exception {
    return objectMapper.readTree(
        result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  /** 데모 메일함에 온 이 사람의 최근 메일(최신순). */
  private JsonNode mailbox(String email) throws Exception {
    return json(
        mockMvc
            .perform(get("/api/v1/demo/mailbox").param("email", email))
            .andExpect(status().isOk()));
  }

  private String verify(String email) throws Exception {
    postJson("/api/v1/email-verifications", Map.of("email", email), null)
        .andExpect(status().isCreated());
    Matcher matcher = CODE.matcher(mailbox(email).get(0).get("body").asText());
    assertThat(matcher.find()).isTrue();
    return json(postJson(
                "/api/v1/email-verifications/confirm",
                Map.of("email", email, "code", matcher.group(1)),
                null)
            .andExpect(status().isOk()))
        .get("verificationToken")
        .asText();
  }

  /** 자동이체통합관리만 신청한 현업 담당자. 새 이메일로 인증부터 가입 신청까지 하고 계정 ID 를 돌려준다. */
  private Long apply(String email, String organizationCode, String name) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("verificationToken", verify(email));
    body.put("email", email);
    body.put("organizationId", organizationService.getByCode(organizationCode).getId());
    body.put("businesses", List.of(Map.of("business", "AUTO_TRANSFER", "subBusiness", "출금이체")));
    body.put("jobRole", "BUSINESS");
    body.put("itRoles", List.of());
    body.put("name", name);
    body.put("department", "디지털금융부");
    body.put("team", "플랫폼팀");
    body.put("jobTitle", "대리");
    body.put("extensionPhone", "1234");
    body.put("mobilePhone", null);
    body.put("password", "Welcome1!");
    postJson("/api/v1/sign-ups", body, null).andExpect(status().isCreated());
    return accountRepository.findByEmail(email).orElseThrow().getId();
  }

  private String newEmail() {
    return "review" + SEQ.incrementAndGet() + "." + System.nanoTime() + "@kb.example.com";
  }

  private ResultActions login(String email) throws Exception {
    return postJson("/api/v1/auth/login", Map.of("email", email, "password", "Welcome1!"), null);
  }

  private List<Notification> unreadOf(CurrentUser user) {
    return notificationRepository.findByRecipientEmailAndReadAtIsNull(user.email());
  }

  @Test
  void 그_업무를_맡은_결제원_담당자는_최근_신청_순으로_목록을_본다() throws Exception {
    mockMvc
        .perform(get("/api/v1/sign-ups").param("business", "account-info").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].name").value("오승현"))
        .andExpect(jsonPath("$.content[0].organizationName").value("신한은행"))
        .andExpect(jsonPath("$.content[0].emailDomainMatches").value(false))
        .andExpect(jsonPath("$.content[0].organizationEmailDomain").value("shinhan.example.com"))
        .andExpect(jsonPath("$.content[0].jobRoleLabel").value("전산(개발)"))
        .andExpect(jsonPath("$.content[0].businesses[1].subBusiness").value("자동이체 전문 개발"))
        .andExpect(jsonPath("$.content[0].statusLabel").value("승인 대기"))
        .andExpect(jsonPath("$.content[0].reviewerName").doesNotExist())
        .andExpect(jsonPath("$.content[1].name").value("한예린"))
        .andExpect(jsonPath("$.content[1].emailDomainMatches").value(true));
  }

  @Test
  void 승인_반려_칩은_가입_승인을_거친_계정만_센다() throws Exception {
    mockMvc
        .perform(get("/api/v1/sign-ups/counts").param("business", "account-info").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pending").value(2))
        .andExpect(jsonPath("$.approved").value(2))
        .andExpect(jsonPath("$.rejected").value(1));
    mockMvc
        .perform(
            get("/api/v1/sign-ups")
                .param("business", "account-info")
                .param("status", "ACTIVE")
                .with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].name").value("최유진"))
        .andExpect(jsonPath("$.content[0].reviewerName").value("데모 관리자"))
        .andExpect(jsonPath("$.content[1].name").value("김민준"));
    mockMvc
        .perform(
            get("/api/v1/sign-ups")
                .param("business", "account-info")
                .param("status", "REJECTED")
                .with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("서민재"))
        .andExpect(jsonPath("$.content[0].rejectionReason").value(containsString("부서장 확인")));
  }

  @Test
  void 담당하지_않는_업무와_기관_담당자는_신청_목록을_볼_수_없다() throws Exception {
    mockMvc
        .perform(get("/api/v1/sign-ups").param("business", "cms").with(hong()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/v1/sign-ups").param("business", "account-info").with(park()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/v1/sign-ups").param("business", "account-info").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/v1/sign-ups/counts")
                .param("business", "account-info")
                .with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/v1/sign-ups").param("business", "loan-transfer").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("정다은"));
  }

  @Test
  void 신청하면_승인할_수_있는_사람에게만_가입_신청_알림이_간다() throws Exception {
    String email = newEmail();
    Long id = apply(email, "004", "알림대상");
    String title = "KB국민은행 알림대상 님이 가입을 신청했습니다";

    for (CurrentUser reviewer : List.of(TestUsers.ADMIN, TestUsers.KFTC_AUTO_TRANSFER)) {
      assertThat(unreadOf(reviewer))
          .filteredOn(n -> n.getTitle().equals(title))
          .singleElement()
          .satisfies(
              n -> {
                assertThat(n.getType()).isEqualTo(NotificationType.SIGNUP_REQUESTED);
                assertThat(n.getLink()).isEqualTo("/auto-transfer/admin/sign-ups?id=" + id);
                assertThat(n.getBody()).contains("이메일 도메인이 기관과 일치합니다.");
              });
    }
    for (CurrentUser other : List.of(TestUsers.KFTC_CMS, TestUsers.GANA_MEMBER)) {
      assertThat(unreadOf(other)).noneMatch(n -> n.getTitle().equals(title));
    }
  }

  @Test
  void 승인하면_로그인할_수_있고_신청자에게_승인_알림과_메일이_간다() throws Exception {
    String email = newEmail();
    Long id = apply(email, "004", "승인대상");
    login(email)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING_APPROVAL"));

    postJson("/api/v1/sign-ups/" + id + "/approval", Map.of(), moon())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.reviewerName").value("문서준"))
        .andExpect(jsonPath("$.reviewedAt").exists());

    login(email).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("승인대상"));
    JsonNode mail = mailbox(email).get(0);
    assertThat(mail.get("subject").asText()).isEqualTo("연결이 가입이 승인되었습니다");
    assertThat(mail.get("body").asText()).contains("담당 업무: 자동이체통합관리");
    assertThat(notificationRepository.findByRecipientEmailAndReadAtIsNull(email))
        .singleElement()
        .satisfies(
            n -> {
              assertThat(n.getType()).isEqualTo(NotificationType.SIGNUP_APPROVED);
              assertThat(n.getLink()).isEqualTo("/auto-transfer/home");
            });

    // 이미 처리한 신청은 다시 처리할 수 없다.
    postJson("/api/v1/sign-ups/" + id + "/approval", Map.of(), TestUsers.admin())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SIGN_UP_ALREADY_REVIEWED"));
    postJson("/api/v1/sign-ups/" + id + "/rejection", Map.of("reason", "늦은 반려"), moon())
        .andExpect(status().isConflict());
  }

  @Test
  void 신청한_업무를_맡지_않은_사람은_상세를_보거나_처리할_수_없다() throws Exception {
    Long id = apply(newEmail(), "004", "권한확인");

    mockMvc
        .perform(get("/api/v1/sign-ups/" + id).with(moon()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.department").value("디지털금융부"))
        .andExpect(jsonPath("$.extensionPhone").value("1234"))
        .andExpect(jsonPath("$.mobilePhone").doesNotExist());
    mockMvc.perform(get("/api/v1/sign-ups/" + id).with(park())).andExpect(status().isForbidden());
    postJson("/api/v1/sign-ups/" + id + "/approval", Map.of(), park())
        .andExpect(status().isForbidden());
    postJson("/api/v1/sign-ups/" + id + "/approval", Map.of(), TestUsers.ganaMember())
        .andExpect(status().isForbidden());
    postJson("/api/v1/sign-ups/" + id + "/rejection", Map.of("reason", "권한 없음"), park())
        .andExpect(status().isForbidden());
    assertThat(accountRepository.findById(id).orElseThrow().getStatus())
        .isEqualTo(AccountStatus.PENDING_APPROVAL);
  }

  @Test
  void 반려는_사유가_있어야_하고_사유가_메일로_가며_같은_이메일로_다시_신청할_수_있다() throws Exception {
    String email = newEmail();
    Long id = apply(email, "004", "반려대상");

    postJson("/api/v1/sign-ups/" + id + "/rejection", Map.of("reason", " "), moon())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("반려 사유를 적어 주세요."));
    postJson(
            "/api/v1/sign-ups/" + id + "/rejection",
            Map.of("reason", "  소속 기관을 다시 골라 주세요.  "),
            moon())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.rejectionReason").value("소속 기관을 다시 골라 주세요."));

    JsonNode mail = mailbox(email).get(0);
    assertThat(mail.get("subject").asText()).isEqualTo("[연결이] 가입 신청이 반려되었습니다");
    assertThat(mail.get("body").asText())
        .contains("반려 사유: 소속 기관을 다시 골라 주세요.")
        .contains("결제원 담당자 문서준 님");
    login(email)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_REJECTED"));

    // 다시 신청하면 같은 계정이 새 신청 내용의 승인 대기가 되고 지난 반려 기록은 지워진다.
    clock.advance(Duration.ofMinutes(2));
    Long again = apply(email, "088", "반려대상");
    assertThat(again).isEqualTo(id);
    mockMvc
        .perform(get("/api/v1/sign-ups/" + id).with(moon()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.organizationName").value("신한은행"))
        .andExpect(jsonPath("$.emailDomainMatches").value(false))
        .andExpect(jsonPath("$.businesses[0].subBusiness").value("출금이체"))
        .andExpect(jsonPath("$.reviewerName").doesNotExist())
        .andExpect(jsonPath("$.rejectionReason").doesNotExist());
  }

  @Test
  void 없는_신청과_모르는_업무_영역은_404다() throws Exception {
    mockMvc
        .perform(get("/api/v1/sign-ups/999999").with(hong()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SIGN_UP_NOT_FOUND"));
    mockMvc
        .perform(get("/api/v1/sign-ups").param("business", "nope").with(hong()))
        .andExpect(status().isNotFound());
  }

  @Test
  void 홈_가입_승인_카드는_승인할_수_있는_사람에게만_실제_신청으로_보인다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "account-info").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[-1].key").value("SIGNUP_APPROVAL"))
        .andExpect(jsonPath("$.cards[-1].source").value("REAL"))
        .andExpect(jsonPath("$.cards[-1].summary").value("승인 대기 2건"))
        .andExpect(jsonPath("$.cards[-1].items[0].title").value("신한은행 · 오승현"))
        .andExpect(jsonPath("$.cards[-1].items[0].subtitle").value("전산(개발) · 도메인 다름"))
        .andExpect(
            jsonPath("$.cards[-1].items[0].link")
                .value(startsWith("/account-info/admin/sign-ups?id=")))
        .andExpect(jsonPath("$.cards[-1].link").value("/account-info/admin/sign-ups"));
    mockMvc
        .perform(get("/api/v1/home").param("business", "account-info").with(TestUsers.ganaMember()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[?(@.key == 'SIGNUP_APPROVAL')]").isEmpty());
  }
}
