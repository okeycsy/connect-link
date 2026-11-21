package com.connectlink.common.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 샘플 데이터 다시 넣기: 관리자만 부르고, 부르면 기한이 오늘 기준으로 돌아오며 샘플 첨부와 업무 단체방도 다시 준비된다. DB 를 통째로 비우므로 따로 쓰는 메모리 DB
 * 에서 돈다.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:sample-refresh-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
class SampleDataDemoControllerTest {

  private static final String BASE = "/api/v1/demo/sample-data-refreshes";
  private static final String FEE_REVIEW = "계좌통합관리 수수료 체계 개편 의견 요청";

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void 관리자가_부르면_지난_기한이_오늘_기준으로_돌아온다() throws Exception {
    jdbc.update("UPDATE opinion_request SET due_date = DATE '2000-01-01'");

    mockMvc
        .perform(post(BASE).with(csrf()).with(TestUsers.admin()))
        .andExpect(status().isNoContent());

    LocalDate today = jdbc.queryForObject("SELECT CURRENT_DATE", LocalDate.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT due_date FROM opinion_request WHERE title = ?",
                LocalDate.class,
                FEE_REVIEW))
        .isEqualTo(today.plusDays(3));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM attachment WHERE storage_key LIKE 'seed:%'", Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM chat_room WHERE type = 'BUSINESS'", Integer.class))
        .isEqualTo(4);
  }

  @Test
  void 관리자가_아니면_403이고_데이터는_그대로다() throws Exception {
    jdbc.update(
        "UPDATE opinion_request SET due_date = DATE '2000-01-01' WHERE title = ?", FEE_REVIEW);

    mockMvc
        .perform(post(BASE).with(csrf()).with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SAMPLE_DATA_REFRESH_ADMIN_ONLY"));

    assertThat(
            jdbc.queryForObject(
                "SELECT due_date FROM opinion_request WHERE title = ?",
                LocalDate.class,
                FEE_REVIEW))
        .isEqualTo(LocalDate.of(2000, 1, 1));
  }
}
