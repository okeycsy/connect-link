package com.connectlink.history;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.organization.OrganizationRepository;
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
class ChangeHistoryControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OrganizationRepository organizationRepository;

  @Test
  void 관리자는_기관의_변경_이력을_최신순으로_본다() throws Exception {
    Long id = organizationRepository.findByCode("088").orElseThrow().getId();
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "ORGANIZATION")
                .param("targetId", String.valueOf(id))
                .with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].action").value("UPDATED"))
        .andExpect(jsonPath("$.content[0].actionLabel").value("수정"))
        .andExpect(jsonPath("$.content[0].summary").value("신한은행 수정"))
        .andExpect(jsonPath("$.content[0].changedBy").value("admin@kftc.example.com"))
        .andExpect(jsonPath("$.content[0].changedByName").value("데모 관리자"))
        .andExpect(jsonPath("$.content[0].changedAt").value("2026-07-14T14:05:00"))
        .andExpect(jsonPath("$.content[0].changes", hasSize(1)))
        .andExpect(jsonPath("$.content[0].changes[0].label").value("참여 업무"))
        .andExpect(jsonPath("$.content[0].changes[0].before").value("계좌통합관리, 자동이체통합관리"))
        .andExpect(jsonPath("$.content[0].changes[0].after").value("계좌통합관리, 자동이체통합관리, CMS"))
        .andExpect(jsonPath("$.content[1].action").value("CREATED"))
        .andExpect(jsonPath("$.content[1].changes", hasSize(5)))
        .andExpect(jsonPath("$.content[1].changes[0].before").doesNotExist());
  }

  @Test
  void 이력이_없는_대상은_빈_목록이다() throws Exception {
    Long id = organizationRepository.findByCode("004").orElseThrow().getId();
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "ORGANIZATION")
                .param("targetId", String.valueOf(id))
                .with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void 관리자가_아니면_기관_이력은_403이다() throws Exception {
    Long id = organizationRepository.findByCode("088").orElseThrow().getId();
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "ORGANIZATION")
                .param("targetId", String.valueOf(id))
                .with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
