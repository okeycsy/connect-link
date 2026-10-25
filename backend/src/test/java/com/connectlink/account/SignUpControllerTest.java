package com.connectlink.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.common.ClockConfig;
import com.connectlink.common.MutableClock;
import com.connectlink.organization.OrganizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * 회원가입: 회사 이메일 인증 코드 받기·확인과 가입 신청. 모두 로그인 없이 부른다(데모 메일함으로 코드를 읽는다). 시계를 바꾸고 계정·발송함을 실제로 쌓으므로 다른
 * 테스트와 나눠 쓰지 않는 DB 를 쓴다.
 */
@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:sign-up-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SignUpControllerTest.ClockOverride.class)
class SignUpControllerTest {

  @TestConfiguration
  static class ClockOverride {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(ClockConfig.SERVER_ZONE);
    }
  }

  private static final Pattern CODE = Pattern.compile("인증 코드: (\\d{6})");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private MutableClock clock;
  @Autowired private OrganizationService organizationService;

  private ResultActions postJson(String path, Object body) throws Exception {
    return mockMvc.perform(
        post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body))
            .with(csrf()));
  }

  private ResultActions requestCode(String email) throws Exception {
    return postJson("/api/v1/email-verifications", Map.of("email", email));
  }

  private ResultActions confirm(String email, String code) throws Exception {
    return postJson("/api/v1/email-verifications/confirm", Map.of("email", email, "code", code));
  }

  /** 데모 메일함에서 가장 최근 인증 메일의 코드를 읽는다. */
  private String latestCode(String email) throws Exception {
    String json =
        mockMvc
            .perform(get("/api/v1/demo/mailbox").param("email", email))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode mails = objectMapper.readTree(json);
    assertThat(mails).isNotEmpty();
    assertThat(mails.get(0).get("subject").asText()).isEqualTo("[연결이] 회사 이메일 인증 코드");
    Matcher matcher = CODE.matcher(mails.get(0).get("body").asText());
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }

  /** 코드를 받아 확인까지 하고 인증 토큰을 돌려준다. */
  private String verify(String email) throws Exception {
    requestCode(email).andExpect(status().isCreated());
    String json =
        confirm(email, latestCode(email))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return objectMapper.readTree(json).get("verificationToken").asText();
  }

  private Long organizationId(String code) {
    return organizationService.getByCode(code).getId();
  }

  private Long inactiveOrganizationId() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/v1/organizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"code":"OLDBANK","name":"옛은행","type":"FINANCE_ORGANIZATION","businesses":["ACCOUNT_INFO"],"sector":"BANK"}
                        """)
                    .with(TestUsers.admin())
                    .with(csrf()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = objectMapper.readTree(created).get("id").asLong();
    mockMvc
        .perform(
            put("/api/v1/organizations/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"옛은행","active":false,"businesses":["ACCOUNT_INFO"],"sector":"BANK"}
                    """)
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isOk());
    return id;
  }

  /** KB국민은행 현업 담당자의 가입 신청 본문. 테스트마다 필요한 칸만 바꾼다. */
  private Map<String, Object> signUpBody(String token, String email) {
    Map<String, Object> body = new HashMap<>();
    body.put("verificationToken", token);
    body.put("email", email);
    body.put("organizationId", organizationId("004"));
    body.put(
        "businesses",
        List.of(
            Map.of("business", "ACCOUNT_INFO", "subBusiness", "어카운트인포 앱"),
            Map.of("business", "CMS")));
    body.put("jobRole", "BUSINESS");
    body.put("itRoles", List.of());
    body.put("name", "가입자");
    body.put("department", "디지털금융부");
    body.put("team", "플랫폼팀");
    body.put("jobTitle", "대리");
    body.put("extensionPhone", "1234");
    body.put("mobilePhone", "010-0000-9999");
    body.put("password", "Welcome1!");
    return body;
  }

  @Test
  void 인증부터_가입_신청까지_로그인_없이_되고_승인_전에는_로그인할_수_없다() throws Exception {
    String email = "Flow.Tester@kb.example.com";
    requestCode(email)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("flow.tester@kb.example.com"))
        .andExpect(jsonPath("$.expiresAt").exists())
        .andExpect(jsonPath("$.resendAfterSeconds").value(60));

    String code = latestCode("flow.tester@kb.example.com");
    String json =
        confirm(email, code)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.verificationToken").isNotEmpty())
            .andExpect(jsonPath("$.suggestedOrganizationIds[0]").value(organizationId("004")))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    String token = objectMapper.readTree(json).get("verificationToken").asText();

    postJson("/api/v1/sign-ups", signUpBody(token, email))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("flow.tester@kb.example.com"))
        .andExpect(jsonPath("$.organizationName").value("KB국민은행"))
        .andExpect(jsonPath("$.businesses.length()").value(2))
        .andExpect(jsonPath("$.businesses[0].code").value("ACCOUNT_INFO"))
        .andExpect(jsonPath("$.jobRoleLabel").value("현업"))
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.statusLabel").value("승인 대기"));

    postJson(
            "/api/v1/auth/login",
            Map.of("email", "flow.tester@kb.example.com", "password", "Welcome1!"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING_APPROVAL"));
    // 신청이 끝난 이메일은 다시 코드를 받을 수 없다.
    requestCode(email)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
  }

  @Test
  void 이미_가입한_이메일에는_코드를_보내지_않는다() throws Exception {
    requestCode("kim.minjun@kb.example.com")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
  }

  @Test
  void 이메일_형식이_아니면_필드_오류다() throws Exception {
    requestCode("not-an-email")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
  }

  @Test
  void 재발송_간격_전에는_다시_받을_수_없고_지나면_새_코드가_간다() throws Exception {
    String email = "resend@shinhan.example.com";
    requestCode(email).andExpect(status().isCreated());
    String first = latestCode(email);
    clock.advance(Duration.ofSeconds(30));
    requestCode(email)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("VERIFICATION_RESEND_TOO_SOON"))
        .andExpect(jsonPath("$.detail").value("인증 코드는 30초 뒤에 다시 받을 수 있습니다."));

    clock.advance(Duration.ofSeconds(31));
    requestCode(email).andExpect(status().isCreated());
    String second = latestCode(email);
    // 확인은 가장 최근 코드로만 한다(같은 숫자가 나올 수 있어 다를 때만 앞 코드를 확인한다).
    if (!first.equals(second)) {
      confirm(email, first)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_MISMATCH"));
    }
    confirm(email, second).andExpect(status().isOk());
  }

  @Test
  void 틀린_코드는_남은_횟수를_알리고_다섯_번_틀리면_새_코드를_받아야_한다() throws Exception {
    String email = "wrong-code@samsung.example.com";
    requestCode(email).andExpect(status().isCreated());
    String code = latestCode(email);
    String wrong = code.equals("000000") ? "111111" : "000000";

    confirm(email, wrong)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_MISMATCH"))
        .andExpect(jsonPath("$.detail").value(containsString("4번 더 틀리면")))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
    for (int i = 0; i < 3; i++) {
      confirm(email, wrong).andExpect(jsonPath("$.code").value("VERIFICATION_CODE_MISMATCH"));
    }
    confirm(email, wrong).andExpect(jsonPath("$.code").value("VERIFICATION_ATTEMPTS_EXCEEDED"));
    // 다섯 번 틀린 코드는 맞게 넣어도 안 된다.
    confirm(email, code)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_ATTEMPTS_EXCEEDED"));
  }

  @Test
  void 유효_시간이_지난_코드는_거부한다() throws Exception {
    String email = "expired@woori.example.com";
    requestCode(email).andExpect(status().isCreated());
    String code = latestCode(email);
    clock.advance(Duration.ofMinutes(10));
    confirm(email, code)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_EXPIRED"));
  }

  @Test
  void 코드를_받지_않은_이메일은_확인할_수_없다() throws Exception {
    confirm("never-requested@kb.example.com", "123456")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_NOT_REQUESTED"));
  }

  @Test
  void 인증_토큰은_그_이메일에만_한_번_쓸_수_있고_유효_시간이_있다() throws Exception {
    String email = "token-owner@kb.example.com";
    String token = verify(email);

    postJson("/api/v1/sign-ups", signUpBody("made-up-token", email))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_REQUIRED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    postJson("/api/v1/sign-ups", signUpBody(token, "someone-else@kb.example.com"))
        .andExpect(jsonPath("$.code").value("VERIFICATION_REQUIRED"));

    postJson("/api/v1/sign-ups", signUpBody(token, email)).andExpect(status().isCreated());
    postJson("/api/v1/sign-ups", signUpBody(token, email))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_REQUIRED"));

    String late = "token-late@kb.example.com";
    String lateToken = verify(late);
    clock.advance(Duration.ofMinutes(30));
    postJson("/api/v1/sign-ups", signUpBody(lateToken, late))
        .andExpect(jsonPath("$.code").value("VERIFICATION_REQUIRED"));
  }

  @Test
  void 신청이_실패하면_토큰은_쓰지_않은_채로_남아_고쳐서_다시_보낼_수_있다() throws Exception {
    String email = "retry@shinhan.example.com";
    String token = verify(email);
    Map<String, Object> body = signUpBody(token, email);
    body.put("organizationId", organizationId("088"));
    body.put("businesses", List.of(Map.of("business", "LOAN_TRANSFER")));
    postJson("/api/v1/sign-ups", body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BUSINESS_NOT_PARTICIPATED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("businesses"))
        .andExpect(jsonPath("$.detail").value("신한은행 은(는) 대출이동 에 참여하지 않는 기관입니다."));

    body.put("businesses", List.of(Map.of("business", "AUTO_TRANSFER")));
    postJson("/api/v1/sign-ups", body).andExpect(status().isCreated());
  }

  @Test
  void 전산이면_세부_직무가_하나_이상_있어야_하고_현업이면_세부_직무를_버린다() throws Exception {
    String email = "it-role@epost.example.com";
    String token = verify(email);
    Map<String, Object> body = signUpBody(token, email);
    body.put("organizationId", organizationId("071"));
    body.put("businesses", List.of(Map.of("business", "CMS")));
    body.put("jobRole", "IT");
    postJson("/api/v1/sign-ups", body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IT_ROLE_REQUIRED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("itRoles"));

    body.put("itRoles", List.of("OPS", "DEV"));
    postJson("/api/v1/sign-ups", body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.jobRoleLabel").value("전산(개발, 운영)"));

    String business = "business-role@epost.example.com";
    Map<String, Object> businessBody = signUpBody(verify(business), business);
    businessBody.put("organizationId", organizationId("071"));
    businessBody.put("businesses", List.of(Map.of("business", "CMS")));
    businessBody.put("itRoles", List.of("OPS"));
    postJson("/api/v1/sign-ups", businessBody)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.jobRoleLabel").value("현업"));
  }

  @Test
  void 필수_칸과_비밀번호_규칙은_칸별로_알린다() throws Exception {
    String email = "fields@kb.example.com";
    Map<String, Object> body = signUpBody("any-token", email);
    body.put("businesses", List.of());
    body.put("name", " ");
    body.put("extensionPhone", "");
    body.put("mobilePhone", "02-123");
    body.put("password", "password");
    postJson("/api/v1/sign-ups", body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("businesses")))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("extensionPhone")))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("mobilePhone")))
        .andExpect(
            jsonPath(
                "$.fieldErrors[?(@.field == 'password')].message",
                hasItem("비밀번호는 영문, 숫자, 특수문자를 모두 넣어 8~64자로 정해 주세요.")));
  }

  @Test
  void 이용기관과_쓰지_않는_기관은_고를_수_없다() throws Exception {
    String email = "use-org@haha.example.com";
    String json =
        confirmAfterRequest(email)
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    // 이용기관은 이메일 도메인이 같아도 미리 골라 주지 않는다.
    assertThat(objectMapper.readTree(json).get("suggestedOrganizationIds")).isEmpty();
    String token = objectMapper.readTree(json).get("verificationToken").asText();

    Map<String, Object> body = signUpBody(token, email);
    body.put("organizationId", organizationId("HAHA"));
    body.put("businesses", List.of(Map.of("business", "CMS")));
    postJson("/api/v1/sign-ups", body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_ALLOWED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("organizationId"));

    // 사용 중지된 기관. 시드에는 없어 하나 만들어 끈다.
    body.put("organizationId", inactiveOrganizationId());
    body.put("businesses", List.of(Map.of("business", "ACCOUNT_INFO")));
    postJson("/api/v1/sign-ups", body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("organizationId"));
  }

  private ResultActions confirmAfterRequest(String email) throws Exception {
    requestCode(email).andExpect(status().isCreated());
    return confirm(email, latestCode(email)).andExpect(status().isOk());
  }
}
