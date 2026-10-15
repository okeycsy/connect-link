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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationOutboxControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 관리자는_발송함을_최신순으로_본다() throws Exception {
    mockMvc
        .perform(get("/api/v1/notification-outbox").param("size", "3").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(7))
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.content[0].channel").value("EMAIL"))
        .andExpect(jsonPath("$.content[0].channelLabel").value("메일"))
        .andExpect(jsonPath("$.content[0].statusLabel").value("발송 대기"));
  }

  @Test
  void 관리자가_아니면_발송함을_볼_수_없다() throws Exception {
    mockMvc
        .perform(get("/api/v1/notification-outbox").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
