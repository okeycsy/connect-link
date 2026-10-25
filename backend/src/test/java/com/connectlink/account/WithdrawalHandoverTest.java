package com.connectlink.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.CurrentUserAuthentication;
import com.connectlink.auth.TestUsers;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 탈퇴 뒤 후임자 인계: 의견 요청의 회신 담당자와 기관 테스트의 결과 등록 알림 대상이 아무도 남지 않으면 후임자를 넣는다. 탈퇴 이벤트를 직접 내서 기능 모듈의 처리만
 * 본다. 연락처·계정을 지우므로 따로 쓰는 DB 다.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:withdrawal-handover-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WithdrawalHandoverTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OrganizationService organizationService;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private TransactionTemplate transactionTemplate;

  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  private ResultActions send(
      MockHttpServletRequestBuilder request, Object body, RequestPostProcessor user)
      throws Exception {
    request.with(csrf()).with(user);
    if (body != null) {
      request
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(body));
    }
    return mockMvc.perform(request);
  }

  private JsonNode json(ResultActions result) throws Exception {
    return objectMapper.readTree(
        result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  private Long orgId(String code) {
    return organizationService.getByCode(code).getId();
  }

  /** 관리자가 탈퇴 처리한 것처럼 이벤트를 낸다(연락처 모듈은 처리한 사람을 이력에 남긴다). */
  private void withdraw(Long accountId, String email, String orgCode) {
    SecurityContextHolder.getContext()
        .setAuthentication(CurrentUserAuthentication.of(TestUsers.ADMIN));
    try {
      transactionTemplate.executeWithoutResult(
          s ->
              events.publishEvent(
                  new AccountWithdrawnEvent(
                      accountId, email, orgCode, Set.of(Business.ACCOUNT_INFO), email)));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /** KB국민은행 회신 담당자를 고른 계좌통합관리 의견 요청. open 이면 회신 요청까지 보낸다. */
  private Long opinionRequest(String title, List<String> kbRecipients, boolean open)
      throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("title", title);
    body.put("content", "의견을 회신해 주세요.");
    body.put("dueDate", LocalDate.now().plusDays(5).toString());
    body.put("questions", List.of(Map.of("type", "TEXT", "title", "의견", "required", false)));
    body.put(
        "targets",
        List.of(Map.of("organizationId", orgId("004"), "recipientEmails", kbRecipients)));
    Long id =
        json(send(post("/api/v1/opinion-requests"), body, hong()).andExpect(status().isCreated()))
            .get("id")
            .asLong();
    if (open) {
      send(post("/api/v1/opinion-requests/" + id + "/opening"), null, hong())
          .andExpect(status().isOk());
    }
    return id;
  }

  private List<String> kbRecipients(Long requestId) throws Exception {
    JsonNode target =
        json(send(get("/api/v1/opinion-requests/" + requestId), null, hong()))
            .get("targets")
            .get(0);
    List<String> emails = new ArrayList<>();
    target.get("recipients").forEach(r -> emails.add(r.get("email").asText()));
    return emails;
  }

  /** 이 요청 화면으로 가는 알림 수. 시드 요청에도 같은 사람이 회신 담당자라 요청 주소로 거른다. */
  private long notifications(Long requestId, String email, NotificationType type) {
    String link = "/account-info/opinions/" + requestId;
    return notificationRepository.findAll().stream()
        .filter(n -> n.getRecipientEmail().equals(email) && n.getType() == type)
        .filter(n -> link.equals(n.getLink()))
        .count();
  }

  @Test
  void 회신_담당자가_모두_탈퇴하면_같은_세부업무_담당자를_넣고_메일과_알림을_보낸다() throws Exception {
    Long id = opinionRequest("인계 확인", List.of("kim.minjun@kb.example.com"), true);
    long openedBefore =
        notifications(id, "namgung.min@kb.example.com", NotificationType.OPINION_REQUEST_OPENED);
    long ownerBefore =
        notifications(
            id, TestUsers.KFTC_ACCOUNT_INFO.email(), NotificationType.OPINION_REQUEST_CHANGED);

    withdraw(-1L, "kim.minjun@kb.example.com", "004");

    // 김민준과 같은 세부업무(계좌정보 통합조회)인 남궁민만 들어온다. 회비 담당 배수아는 아니다.
    assertThat(kbRecipients(id)).containsExactly("namgung.min@kb.example.com");
    assertThat(
            notifications(
                id, "namgung.min@kb.example.com", NotificationType.OPINION_REQUEST_OPENED))
        .isEqualTo(openedBefore + 1);
    assertThat(
            notifications(
                id, TestUsers.KFTC_ACCOUNT_INFO.email(), NotificationType.OPINION_REQUEST_CHANGED))
        .isEqualTo(ownerBefore + 1);
  }

  @Test
  void 다른_회신_담당자가_남아_있으면_후임자를_넣지_않는다() throws Exception {
    Long id =
        opinionRequest(
            "남은 담당자", List.of("namgung.min@kb.example.com", "bae.sua@kb.example.com"), false);

    withdraw(-2L, "bae.sua@kb.example.com", "004");

    assertThat(kbRecipients(id)).containsExactly("namgung.min@kb.example.com");
  }

  private Long testPlan(String title) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("subBusiness", "계좌정보 조회·해지");
    body.put("title", title);
    body.put("startDate", LocalDate.now().toString());
    body.put("endDate", LocalDate.now().plusDays(7).toString());
    body.put("items", List.of(Map.of("title", "조회")));
    body.put("organizationIds", List.of(orgId("004")));
    return json(send(post("/api/v1/test-plans"), body, hong()).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private JsonNode planRecipients(Long planId) throws Exception {
    return json(
        send(get("/api/v1/test-plans/" + planId + "/recipients"), null, hong())
            .andExpect(status().isOk()));
  }

  private Long accountIdOf(JsonNode rows, String email) {
    for (JsonNode row : rows) {
      if (row.get("email").asText().equals(email)) {
        return row.get("accountId").asLong();
      }
    }
    throw new IllegalArgumentException(email);
  }

  private List<String> selected(JsonNode rows) {
    List<String> emails = new ArrayList<>();
    rows.forEach(
        r -> {
          if (r.get("selected").asBoolean()) {
            emails.add(r.get("email").asText());
          }
        });
    return emails;
  }

  @Test
  void 알림_대상이_모두_탈퇴하면_테스트를_만든_사람을_다시_넣는다() throws Exception {
    Long planId = testPlan("알림 대상 인계");
    JsonNode rows = planRecipients(planId);
    Long opsId = accountIdOf(rows, "kim.ops@kftc.example.com");
    send(
            put("/api/v1/test-plans/" + planId + "/recipients"),
            Map.of("accountIds", List.of(opsId)),
            hong())
        .andExpect(status().isOk());

    withdraw(opsId, "kim.ops@kftc.example.com", "KFTC");

    assertThat(selected(planRecipients(planId)))
        .containsExactly(TestUsers.KFTC_ACCOUNT_INFO.email());
  }

  @Test
  void 만든_사람까지_탈퇴하면_그_업무_결제원_담당자_전부를_넣는다() throws Exception {
    Long planId = testPlan("만든 사람 탈퇴");
    JsonNode rows = planRecipients(planId);
    Long hongId = accountIdOf(rows, TestUsers.KFTC_ACCOUNT_INFO.email());

    withdraw(hongId, TestUsers.KFTC_ACCOUNT_INFO.email(), "KFTC");

    List<String> after = selected(planRecipients(planId));
    assertThat(after)
        .doesNotContain(TestUsers.KFTC_ACCOUNT_INFO.email())
        .contains("kim.ops@kftc.example.com", "lee.control@kftc.example.com");
  }
}
