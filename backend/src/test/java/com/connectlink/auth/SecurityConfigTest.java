package com.connectlink.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 공개되지_않은_API는_로그인_없이_401_JSON을_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/contacts"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.detail").value("로그인이 필요합니다."));
  }

  @Test
  void 상태_API는_로그인_없이_열린다() throws Exception {
    mockMvc
        .perform(get("/api/v1/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.profile").value("test"));
  }

  @Test
  void 정적_자원_주소는_index_html로_넘기지_않는다() throws Exception {
    mockMvc.perform(get("/assets/index-abc.js")).andExpect(status().isNotFound());
  }

  @Test
  void 화면_주소는_index_html로_넘긴다() throws Exception {
    mockMvc
        .perform(get("/account-info/home"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }
}
