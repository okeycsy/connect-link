package com.connectlink.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private NotificationRepository notificationRepository;

  @Test
  void 내_알림만_최신순으로_돌려준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/notifications").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(10))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.content[0].type").value("SIGNUP_REQUESTED"))
        .andExpect(jsonPath("$.content[0].typeLabel").value("가입 신청"))
        .andExpect(jsonPath("$.content[0].read").value(false))
        .andExpect(jsonPath("$.content[1].businessSlug").value("account-info"));

    mockMvc
        .perform(get("/api/v1/notifications").with(TestUsers.ganaMember()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].title").value("의견 요청 \"계좌통합관리 수수료 체계 개편\" 에 회신해 주세요"));
  }

  @Test
  void 읽지_않은_알림만_걸러_볼_수_있다() throws Exception {
    mockMvc
        .perform(get("/api/v1/notifications").param("unreadOnly", "true").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(5));
    mockMvc
        .perform(get("/api/v1/notifications/unread-count").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(5));
  }

  @Test
  void 본인_알림은_읽음_처리된다() throws Exception {
    Long id = unreadIdOf("admin@kftc.example.com");
    mockMvc
        .perform(post("/api/v1/notifications/{id}/read", id).with(TestUsers.admin()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.read").value(true))
        .andExpect(jsonPath("$.readAt").isNotEmpty());
    mockMvc
        .perform(get("/api/v1/notifications/unread-count").with(TestUsers.admin()))
        .andExpect(jsonPath("$.count").value(4));
  }

  @Test
  void 다른_사람의_알림은_읽음_처리할_수_없다() throws Exception {
    Long adminsNotification = unreadIdOf("admin@kftc.example.com");
    mockMvc
        .perform(
            post("/api/v1/notifications/{id}/read", adminsNotification)
                .with(TestUsers.ganaMember())
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void 없는_알림은_404다() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/notifications/{id}/read", 999_999).with(TestUsers.admin()).with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
  }

  @Test
  void 모두_읽음_처리하면_읽지_않은_알림이_없어진다() throws Exception {
    mockMvc
        .perform(post("/api/v1/notifications/read-all").with(TestUsers.admin()).with(csrf()))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/v1/notifications/unread-count").with(TestUsers.admin()))
        .andExpect(jsonPath("$.count").value(0));
    // 다른 사람 것은 그대로다.
    mockMvc
        .perform(get("/api/v1/notifications/unread-count").with(TestUsers.ganaMember()))
        .andExpect(jsonPath("$.count").value(1));
  }

  @Test
  void 로그인_없이는_401이다() throws Exception {
    mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
  }

  private Long unreadIdOf(String email) {
    return notificationRepository.findByRecipientEmailAndReadAtIsNull(email).get(0).getId();
  }
}
