package com.connectlink.home;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
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
  void 기관_담당자_홈은_요청받은_일과_우리_기관을_부른_대화를_보이고_일시중단은_없다() throws Exception {
    // SHY 요청(2026-10-24): 기관 홈은 요청받은 일 위주이고 줄마다 할 일 단계를 보인다.
    mockMvc
        .perform(get("/api/v1/home").param("business", "account-info").with(TestUsers.ganaMember()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[*].key").value(not(hasItem("SUSPENSIONS"))))
        .andExpect(
            jsonPath("$.cards[?(@.key == 'UPDATE_CAMPAIGN')].title").value(hasItem("연락처 변경·확인 요청")))
        .andExpect(
            jsonPath("$.cards[?(@.key == 'OPINION_REPLIES')].items[0].subtitle")
                .value(hasItem(startsWith("미작성"))))
        .andExpect(
            jsonPath("$.cards[?(@.key == 'TEST_PROGRESS')].title").value(hasItem("요청받은 기관 테스트")))
        // 시드: 홍성민이 김민준이 마지막으로 읽은 뒤에 KB국민은행을 멘션했다.
        .andExpect(
            jsonPath("$.cards[?(@.key == 'CHAT_MENTIONS')].items[0].link")
                .value(hasItem(startsWith("/account-info/chat/"))));
  }

  @Test
  void 결제원_담당자_홈에는_우리_기관을_부른_대화_카드가_없다() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/home")
                .param("business", "account-info")
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[*].key").value(not(hasItem("CHAT_MENTIONS"))))
        .andExpect(jsonPath("$.cards[*].key").value(hasItem("SUSPENSIONS")))
        // 기관 하나를 가리키는 줄은 기관 코드를 실어 화면이 로고를 붙인다.
        .andExpect(
            jsonPath("$.cards[?(@.key == 'SUSPENSIONS')].items[0].organizationCode")
                .value(hasItem(org.hamcrest.Matchers.notNullValue())));
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
