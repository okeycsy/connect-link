package com.connectlink.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 문의 안내는 로그인 없이 보고, 설정한 주소와 개인정보 관리부서 연락처를 그대로 준다. */
@SpringBootTest(
    properties = {
      "app.support.inquiry-email= help@connect-link.example.com ",
      "app.support.privacy-department=연결이 운영팀",
      "app.support.privacy-phone=02-0000-0000"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportInfoControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 로그인_없이_문의_메일_주소를_본다() throws Exception {
    mockMvc
        .perform(get("/api/v1/support-info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inquiryEmail").value("help@connect-link.example.com"))
        .andExpect(jsonPath("$.privacyDepartment").value("연결이 운영팀"))
        .andExpect(jsonPath("$.privacyPhone").value("02-0000-0000"));
  }
}
