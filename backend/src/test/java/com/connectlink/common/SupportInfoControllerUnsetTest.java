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

/** 주소를 설정하지 않으면 null 이라 화면이 문의하기 칸을 뺀다. */
@SpringBootTest(properties = "app.support.inquiry-email=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportInfoControllerUnsetTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 설정이_없으면_주소가_null_이다() throws Exception {
    mockMvc
        .perform(get("/api/v1/support-info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inquiryEmail").doesNotExist());
  }
}
