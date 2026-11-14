package com.connectlink.auth.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** 개인정보를 다룬 요청은 접속기록으로 남고, 관리자만 내부망에서 본다. 보관 기간이 지난 기록은 지운다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessLogControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AccessLogRepository repository;
  @Autowired private AccessLogService service;

  @Test
  void 담당자_연락처를_보면_계정_IP_수행업무_대상이_남고_관리자가_내부망에서_본다() throws Exception {
    mockMvc
        .perform(get("/api/v1/contacts?business=account-info").with(TestUsers.ganaMember()))
        .andExpect(status().is2xxSuccessful());

    mockMvc
        .perform(
            get("/api/v1/access-logs")
                .param("email", TestUsers.GANA_MEMBER.email())
                .with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].action", hasItem("담당자 연락처 목록 조회")))
        .andExpect(jsonPath("$.content[0].accountEmail").value(TestUsers.GANA_MEMBER.email()))
        .andExpect(jsonPath("$.content[0].ipAddress").value("127.0.0.1"))
        .andExpect(jsonPath("$.content[0].target").value("business=account-info"));
  }

  @Test
  void 경로의_대상_번호는_처리한_정보주체로_남는다() {
    AccessLogRule rule = AccessLogRule.match("GET", "/api/v1/contacts/12").orElseThrow();
    assertThat(rule.action()).isEqualTo("담당자 연락처 상세 조회");
    assertThat(rule.targetOf("/api/v1/contacts/12", null)).isEqualTo("담당자 연락처 #12");
    // 정확히 같은 경로가 패턴보다 먼저다.
    assertThat(AccessLogRule.match("GET", "/api/v1/contacts/excel").orElseThrow().action())
        .isEqualTo("담당자 연락처 엑셀 다운로드");
    // 개인정보와 상관없는 요청은 남기지 않는다.
    assertThat(AccessLogRule.match("GET", "/api/v1/health")).isEmpty();
  }

  @Test
  void 관리자가_아니거나_인터넷망이면_접속기록을_볼_수_없다() throws Exception {
    mockMvc
        .perform(get("/api/v1/access-logs").with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/api/v1/access-logs").header("X-Network-Zone", "EXTERNAL").with(TestUsers.admin()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
  }

  @Test
  void 보관_기간이_지난_기록은_지운다() {
    AccessLogRule rule = AccessLogRule.match("GET", "/api/v1/contacts").orElseThrow();
    AccessLog old =
        repository.save(
            new AccessLog(
                LocalDateTime.now().minusDays(800),
                "old@kb.example.com",
                "옛기록",
                "KB국민은행",
                "127.0.0.1",
                rule.action(),
                null,
                "GET",
                "/api/v1/contacts",
                200));
    assertThat(service.deleteExpired()).isGreaterThanOrEqualTo(1);
    assertThat(repository.findById(old.getId())).isEmpty();
  }
}
