package com.connectlink.auth;

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
class MeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 로그인_사용자_정보를_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/me").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("admin@kftc.example.com"))
        .andExpect(jsonPath("$.name").value("데모 관리자"))
        .andExpect(jsonPath("$.organizationTypeLabel").value("결제원"))
        .andExpect(jsonPath("$.admin").value(true));
  }

  @Test
  void 로그인_없이는_401이다() throws Exception {
    mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
  }
}
