package com.connectlink.home;

import static org.hamcrest.Matchers.hasSize;
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
  void 업무_영역의_카드_여섯_장과_KPI_네_개를_자리_순서대로_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "account-info").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.business").value("ACCOUNT_INFO"))
        .andExpect(jsonPath("$.businessLabel").value("계좌통합관리"))
        .andExpect(jsonPath("$.userName").value("데모 관리자"))
        .andExpect(jsonPath("$.cards", hasSize(6)))
        .andExpect(jsonPath("$.cards[0].key").value("CONTACT_CONFIRM"))
        .andExpect(jsonPath("$.cards[0].source").value("SAMPLE"))
        .andExpect(jsonPath("$.cards[0].urgency").value("OVERDUE"))
        .andExpect(jsonPath("$.cards[0].link").value("/account-info/contacts"))
        .andExpect(jsonPath("$.cards[5].key").value("SIGNUP_APPROVAL"))
        .andExpect(jsonPath("$.kpis", hasSize(4)))
        .andExpect(jsonPath("$.kpis[0].label").value("확인 필요 담당자"))
        .andExpect(jsonPath("$.kpis[0].urgent").value(true));
  }

  @Test
  void 업무_영역에_따라_숫자가_다르다() throws Exception {
    mockMvc
        .perform(get("/api/v1/home").param("business", "cms").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.businessLabel").value("CMS"))
        .andExpect(jsonPath("$.kpis[0].value").value(4))
        .andExpect(jsonPath("$.cards[1].link").value("/cms/contacts"));
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
}
