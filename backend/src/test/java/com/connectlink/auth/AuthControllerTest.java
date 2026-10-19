package com.connectlink.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  private static final String PASSWORD = "demo1234!";

  @Autowired private MockMvc mockMvc;

  private ResultActions login(String email, String password, MockHttpSession session)
      throws Exception {
    String body =
        "{\"email\":\"%s\",\"password\":\"%s\"}"
            .formatted(email == null ? "" : email, password == null ? "" : password);
    var request =
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .with(csrf());
    if (session != null) {
      request.session(session);
    }
    return mockMvc.perform(request);
  }

  @Test
  void 맞는_비밀번호면_로그인되고_같은_세션으로_내_정보를_본다() throws Exception {
    MvcResult result =
        login("kim.gana@kb.example.com", PASSWORD, null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("kim.gana@kb.example.com"))
            .andExpect(jsonPath("$.organizationName").value("KB국민은행"))
            .andExpect(jsonPath("$.admin").value(false))
            .andExpect(jsonPath("$.businesses.length()").value(1))
            .andExpect(jsonPath("$.businesses[0].code").value("ACCOUNT_INFO"))
            .andExpect(jsonPath("$.roleLabel").value("참가기관 · 현업"))
            .andReturn();
    MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
    assertThat(session).isNotNull();

    mockMvc
        .perform(get("/api/v1/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("kim.gana@kb.example.com"));
  }

  @Test
  void 로그인하면_세션_ID를_새로_발급한다() throws Exception {
    MockHttpSession before = new MockHttpSession();
    String beforeId = before.getId();
    MvcResult result =
        login("kim.gana@kb.example.com", PASSWORD, before).andExpect(status().isOk()).andReturn();
    assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(beforeId);
  }

  @Test
  void 이메일은_대소문자와_앞뒤_공백을_가리지_않는다() throws Exception {
    login("  Admin@KFTC.example.com ", PASSWORD, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@kftc.example.com"))
        .andExpect(jsonPath("$.admin").value(true))
        .andExpect(jsonPath("$.businesses.length()").value(4))
        .andExpect(jsonPath("$.jobRoleLabel").value("전산(운영)"));
  }

  @Test
  void 비밀번호가_틀리면_401이다() throws Exception {
    login("kim.gana@kb.example.com", "wrong-password", null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.detail").value("이메일 또는 비밀번호가 맞지 않습니다."));
  }

  @Test
  void 없는_이메일도_같은_401이다() throws Exception {
    login("nobody@kb.example.com", PASSWORD, null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void 승인_대기_계정은_비밀번호가_맞으면_승인_전이라고_알려_준다() throws Exception {
    login("jung.maba@sbi.example.com", PASSWORD, null)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING_APPROVAL"));
  }

  @Test
  void 승인_대기_계정도_비밀번호가_틀리면_상태를_알리지_않는다() throws Exception {
    login("jung.maba@sbi.example.com", "wrong-password", null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void 빈_값은_필드별_메시지로_400이다() throws Exception {
    login("", "", null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(2));
  }

  @Test
  void CSRF_토큰이_없으면_로그인할_수_없다() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"kim.gana@kb.example.com\",\"password\":\"demo1234!\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void 로그아웃하면_세션이_끝난다() throws Exception {
    MvcResult result =
        login("kim.gana@kb.example.com", PASSWORD, null).andExpect(status().isOk()).andReturn();
    MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

    mockMvc
        .perform(post("/api/v1/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(session.isInvalid()).isTrue();

    mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void 로그인하지_않아도_로그아웃은_204다() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout").with(csrf())).andExpect(status().isNoContent());
  }
}
