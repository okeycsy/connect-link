package com.connectlink.notification;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
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
        .andExpect(jsonPath("$.totalElements").value(12))
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.content[0].channel").value("EMAIL"))
        .andExpect(jsonPath("$.content[0].channelLabel").value("메일"))
        .andExpect(jsonPath("$.content[0].statusLabel").value("발송 대기"));
  }

  @Test
  void 샘플_리마인드_메일은_실제_리마인드_문구와_같다() throws Exception {
    mockMvc
        .perform(get("/api/v1/notification-outbox").param("size", "100").with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.content[?(@.recipient == 'kim.minjun@kb.example.com' && @.subject =~ /\\[금융결제원\\] 계좌통합관리 수수료 체계 개편 의견 요청 회신 리마인드 \\(~\\d{4}-\\d{2}-\\d{2}까지\\)/)].body")
                .value(
                    hasItem(
                        matchesPattern(
                            "(?s)회신 기한\\(\\d{4}-\\d{2}-\\d{2}\\)까지 5일 남았습니다\\. 연결이에서 회신해 주세요\\.\nhttp://localhost:8080/account-info/opinions/\\d+"))))
        .andExpect(
            jsonPath(
                    "$.content[?(@.subject =~ /\\[금융결제원\\] \\(계좌통합관리\\) 정기 변경 리마인드: 하반기 담당자 정기 변경 .*/)].recipient")
                .value(
                    containsInAnyOrder("kim.minjun@kb.example.com", "namgung.min@kb.example.com")));
  }

  @Test
  void 관리자가_아니면_발송함을_볼_수_없다() throws Exception {
    mockMvc
        .perform(get("/api/v1/notification-outbox").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
