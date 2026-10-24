package com.connectlink.home;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HomeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 할_일이_있는_카드만_자리_순서대로_돌려주고_건수_요약은_없다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "account-info").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.business").value("ACCOUNT_INFO"))
        .andExpect(jsonPath("$.businessLabel").value("계좌통합관리"))
        .andExpect(jsonPath("$.userName").value("데모 관리자"))
        .andExpect(jsonPath("$.kpis").doesNotExist())
        .andExpect(jsonPath("$.cards[0].key").value("UPDATE_CAMPAIGN"))
        .andExpect(jsonPath("$.cards[0].source").value("REAL"))
        .andExpect(jsonPath("$.cards[?(@.items.length() == 0)]").isEmpty())
        .andExpect(
            jsonPath("$.cards[?(@.key == 'SUSPENSIONS')].title").value(hasItem("다가오는 일시중단")));
  }

  @Test
  void 할_일이_없는_카드는_뺀다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "cms").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.businessLabel").value("CMS"))
        // CMS 는 진행 중인 정기 변경 주간이 없다(연락처 시드).
        .andExpect(jsonPath("$.cards[?(@.key == 'UPDATE_CAMPAIGN')]").isEmpty());
  }

  @Test
  void 확인이_필요한_담당자_카드는_홈에_없다() throws Exception {
    // 6개월 확인은 로그인 뒤 확인 창이 맡는다. SHY 요청(2026-10-24).
    mockMvc
        .perform(get("/api/v1/home").param("business", "account-info").with(TestUsers.ganaMember()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[*].key").value(not(hasItem("CONTACT_CONFIRM"))));
  }

  @Test
  void 모르는_업무_영역은_404다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "nope").with(TestUsers.admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BUSINESS_NOT_FOUND"));
  }

  @Test
  void 로그인_없이는_401이다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "cms"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void 담당_업무가_아닌_업무_영역의_홈은_403이다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "cms").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void 담당_업무의_홈은_관리자가_아니어도_열린다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "account-info").with(TestUsers.ganaMember()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userName").value("김민준"));
  }
}
