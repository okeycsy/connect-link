package com.connectlink.organization;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 기관 한눈에 보기: 결제원 담당자만, 맡은 업무에 참여하는 참가기관만 보고, 칸마다 그 기능의 권한을 따른다. 의견 요청을 새로 만들므로 따로 쓰는 DB 다. */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:organization-overview-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationOverviewControllerTest {

  /** 결제원 전산(운영). 전 업무를 조회만 한다. */
  private static final CurrentUser KFTC_OPS =
      new CurrentUser(
          "kim.ops@kftc.example.com",
          "김운영",
          "KFTC",
          "금융결제원",
          OrganizationType.KFTC,
          false,
          EnumSet.allOf(Business.class),
          JobRole.IT,
          Set.of(ItRole.OPS));

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OrganizationService organizationService;

  private Long orgId(String code) {
    return organizationService.getByCode(code).getId();
  }

  private ResultActions overview(String code, String business, RequestPostProcessor user)
      throws Exception {
    return mockMvc.perform(
        get("/api/v1/organizations/" + orgId(code) + "/overview")
            .param("business", business)
            .with(user));
  }

  /** 홍성민이 KB국민은행(김민준)에 회신 요청을 보낸다. */
  private String openOpinionRequest() throws Exception {
    String title = "한눈에 보기 확인 " + System.nanoTime();
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("title", title);
    body.put("content", "의견을 회신해 주세요.");
    body.put("dueDate", LocalDate.now().plusDays(5).toString());
    body.put("questions", List.of(Map.of("type", "TEXT", "title", "의견", "required", false)));
    body.put(
        "targets",
        List.of(
            Map.of(
                "organizationId",
                orgId("004"),
                "recipientEmails",
                List.of("kim.minjun@kb.example.com"))));
    String json =
        mockMvc
            .perform(
                post("/api/v1/opinion-requests")
                    .with(csrf())
                    .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode created = objectMapper.readTree(json);
    mockMvc
        .perform(
            post("/api/v1/opinion-requests/" + created.get("id").asLong() + "/opening")
                .with(csrf())
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO)))
        .andExpect(status().isOk());
    return title;
  }

  @Test
  void 결제원_담당자는_맡은_업무의_참가기관을_칸마다_본다() throws Exception {
    String title = openOpinionRequest();
    overview("004", "account-info", TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationName").value("KB국민은행"))
        .andExpect(jsonPath("$.sectorLabel").value("은행"))
        .andExpect(jsonPath("$.sections[0].key").value("CONTACT"))
        .andExpect(jsonPath("$.sections[0].items[*].title", hasItem("김민준 대리")))
        .andExpect(jsonPath("$.sections[0].items[0].email").exists())
        .andExpect(jsonPath("$.sections[0].link").value("/account-info/contacts"))
        .andExpect(jsonPath("$.sections[1].key").value("OPINION_REQUEST"))
        .andExpect(jsonPath("$.sections[1].items[*].title", hasItem(title)))
        .andExpect(jsonPath("$.sections[2].key").value("TEST_PLAN"))
        .andExpect(jsonPath("$.sections[3].key").value("SUSPENSION"));
  }

  @Test
  void 전_업무를_보는_운영은_어느_업무의_참가기관이든_본다() throws Exception {
    overview("071", "cms", TestUsers.as(KFTC_OPS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].key").value("CONTACT"));
  }

  @Test
  void 참가기관_담당자는_보지_못한다() throws Exception {
    overview("004", "account-info", TestUsers.ganaMember()).andExpect(status().isForbidden());
  }

  @Test
  void 담당_업무가_아니면_보지_못한다() throws Exception {
    overview("004", "cms", TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO))
        .andExpect(status().isForbidden());
  }

  @Test
  void 그_업무에_참여하지_않는_기관이나_결제원은_없다고_답한다() throws Exception {
    overview("240", "cms", TestUsers.as(TestUsers.KFTC_CMS))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
    overview("KFTC", "account-info", TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO))
        .andExpect(status().isNotFound());
  }
}
