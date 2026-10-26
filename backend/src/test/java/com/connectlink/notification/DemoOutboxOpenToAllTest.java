package com.connectlink.notification;

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

/** 시연(local·demo)처럼 {@code app.demo.outbox-open-to-all} 을 켜면 관리자가 아닌 계정도 발송함을 본다. */
@SpringBootTest(properties = "app.demo.outbox-open-to-all=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoOutboxOpenToAllTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 시연_설정이면_참가기관_담당자도_발송함을_본다() throws Exception {
    mockMvc
        .perform(get("/api/v1/notification-outbox").with(TestUsers.ganaMember()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(7));
  }
}
