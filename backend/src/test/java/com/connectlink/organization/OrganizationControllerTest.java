package com.connectlink.organization;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 기관_목록은_로그인_없이_조회되고_사용_중인_기관만_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/organizations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(8)))
        .andExpect(jsonPath("$[0].code").value("KFTC"))
        .andExpect(jsonPath("$[0].typeLabel").value("결제원"))
        .andExpect(jsonPath("$[*].code", not(org.hamcrest.Matchers.hasItem("OLD"))));
  }

  @Test
  void 업무_영역은_네_개를_주소_slug와_함께_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/businesses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)))
        .andExpect(jsonPath("$[0].code").value("ACCOUNT_INFO"))
        .andExpect(jsonPath("$[0].slug").value("account-info"))
        .andExpect(jsonPath("$[0].label").value("계좌통합관리"));
  }
}
