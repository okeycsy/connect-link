package com.connectlink.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 비밀번호 변경(내 정보)과 비밀번호 찾기(로그인 화면). 계정 비밀번호를 실제로 바꾸므로 따로 쓰는 DB 다. */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:password-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordControllerTest {

  private static final String PASSWORD = "Welcome1!";
  private static final String NEW_PASSWORD = "Changed2@";
  private static final Pattern CODE = Pattern.compile("인증 코드: (\\d{6})");
  private static final AtomicInteger SEQ = new AtomicInteger();

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserAccountRepository accountRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private OrganizationService organizationService;

  /** KB국민은행 계좌통합관리 담당자. 사용 중 계정을 새로 만들고 이메일을 돌려준다. */
  private String activeAccount() {
    String email = "pw" + SEQ.incrementAndGet() + "." + System.nanoTime() + "@kb.example.com";
    UserAccount account =
        UserAccount.apply(
            email,
            passwordEncoder.encode(PASSWORD),
            "김비번",
            organizationService.getByCode("004"),
            JobRole.BUSINESS,
            Set.of(),
            Map.of(Business.ACCOUNT_INFO, "본인계좌 일괄지급정지"),
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

  private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body)
      throws Exception {
    return request
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf());
  }

  private ResultActions login(String email, String password) throws Exception {
    return mockMvc.perform(
        json(post("/api/v1/auth/login"), Map.of("email", email, "password", password)));
  }

  private MockHttpSession session(String email) throws Exception {
    return (MockHttpSession)
        login(email, PASSWORD)
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  private ResultActions change(MockHttpSession session, String current, String next)
      throws Exception {
    return mockMvc.perform(
        json(put("/api/v1/me/password"), Map.of("currentPassword", current, "newPassword", next))
            .session(session));
  }

  private ResultActions requestReset(String email) throws Exception {
    return mockMvc.perform(json(post("/api/v1/password-resets"), Map.of("email", email)));
  }

  private ResultActions confirmReset(String email, String code, String password) throws Exception {
    return mockMvc.perform(
        json(
            post("/api/v1/password-resets/confirm"),
            Map.of("email", email, "code", code, "newPassword", password)));
  }

  private JsonNode mailbox(String email) throws Exception {
    String json =
        mockMvc
            .perform(get("/api/v1/demo/mailbox").param("email", email))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return objectMapper.readTree(json);
  }

  private String latestCode(String email) throws Exception {
    JsonNode mails = mailbox(email);
    assertThat(mails.get(0).get("subject").asText()).isEqualTo("[금융결제원] (연결이) 비밀번호 찾기 인증 코드");
    Matcher matcher = CODE.matcher(mails.get(0).get("body").asText());
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }

  @Test
  void 지금_비밀번호를_확인하고_바꾸면_새_비밀번호로_로그인한다() throws Exception {
    String email = activeAccount();
    MockHttpSession session = session(email);

    change(session, PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

    login(email, PASSWORD).andExpect(status().isUnauthorized());
    login(email, NEW_PASSWORD).andExpect(status().isOk());
    assertThat(mailbox(email).get(0).get("subject").asText())
        .isEqualTo("[금융결제원] (연결이) 비밀번호가 바뀌었습니다");
  }

  @Test
  void 비밀번호를_바꾸면_이_세션은_남고_다른_세션은_끝난다() throws Exception {
    String email = activeAccount();
    MockHttpSession other = session(email);
    Thread.sleep(5);
    MockHttpSession mine = session(email);
    Thread.sleep(5);

    change(mine, PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/v1/me").session(mine)).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/me").session(other)).andExpect(status().isUnauthorized());
  }

  @Test
  void 지금_비밀번호가_틀리면_바꾸지_않는다() throws Exception {
    String email = activeAccount();
    change(session(email), "Wrong123!", NEW_PASSWORD)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));
    login(email, PASSWORD).andExpect(status().isOk());
  }

  @Test
  void 새_비밀번호가_지금과_같거나_규칙에_맞지_않으면_400_이다() throws Exception {
    MockHttpSession session = session(activeAccount());
    change(session, PASSWORD, PASSWORD)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));
    change(session, PASSWORD, "short")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
  }

  @Test
  void 로그인하지_않으면_비밀번호를_바꿀_수_없다() throws Exception {
    mockMvc
        .perform(
            json(
                put("/api/v1/me/password"),
                Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void 비밀번호_찾기_코드로_새_비밀번호를_정하면_모든_세션이_끝난다() throws Exception {
    String email = activeAccount();
    MockHttpSession before = session(email);
    Thread.sleep(5);

    requestReset(email.toUpperCase())
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.email").value(email));
    confirmReset(email, latestCode(email), NEW_PASSWORD).andExpect(status().isNoContent());

    login(email, NEW_PASSWORD).andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/me").session(before)).andExpect(status().isUnauthorized());
  }

  @Test
  void 쓴_코드는_다시_쓸_수_없다() throws Exception {
    String email = activeAccount();
    requestReset(email).andExpect(status().isAccepted());
    String code = latestCode(email);
    confirmReset(email, code, NEW_PASSWORD).andExpect(status().isNoContent());
    confirmReset(email, code, "Another3#")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_EXPIRED"));
  }

  @Test
  void 코드가_틀리면_바꾸지_않고_남은_횟수를_알린다() throws Exception {
    String email = activeAccount();
    requestReset(email).andExpect(status().isAccepted());
    String code = latestCode(email);
    String wrong = code.equals("000000") ? "111111" : "000000";
    confirmReset(email, wrong, NEW_PASSWORD)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_MISMATCH"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
    login(email, PASSWORD).andExpect(status().isOk());
  }

  @Test
  void 계정이_없는_이메일도_같은_답이지만_메일은_가지_않는다() throws Exception {
    String email = "nobody" + System.nanoTime() + "@kb.example.com";
    requestReset(email)
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.email").value(email));
    assertThat(mailbox(email)).isEmpty();
    confirmReset(email, "123456", NEW_PASSWORD)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_MISMATCH"));
  }

  @Test
  void 코드를_곧바로_다시_받으면_429_이다() throws Exception {
    String email = activeAccount();
    requestReset(email).andExpect(status().isAccepted());
    requestReset(email)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("VERIFICATION_RESEND_TOO_SOON"));
  }
}
