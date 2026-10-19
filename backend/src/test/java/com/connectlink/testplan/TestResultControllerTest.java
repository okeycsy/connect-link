package com.connectlink.testplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.common.ClockConfig;
import com.connectlink.common.MutableClock;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationOutbox;
import com.connectlink.notification.NotificationOutboxRepository;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.OrganizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 다. 기관 테스트 결과: 기관 담당자의 요구사항별 결과 등록과 매터모스트 알림, 결제원 의견(미니 수행률·특이사항)과 수행 완료, 의견 뒤 재등록의 "다시 확인", 기관별
 * 결과·이력의 범위. 테스트마다 새 기관 테스트를 만들어 쓴다. 등록과 의견의 앞뒤를 가리려고 시계를 돌린다.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:test-result-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestResultControllerTest.ClockOverride.class)
class TestResultControllerTest {

  @TestConfiguration
  static class ClockOverride {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(ClockConfig.SERVER_ZONE);
    }
  }

  private static final String BASE = "/api/v1/test-plans";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private MutableClock clock;
  @Autowired private OrganizationService organizationService;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NotificationOutboxRepository outboxRepository;

  /** 결제원 계좌통합관리 현업. 새 테스트를 만들면 결과 등록 알림 대상이 된다. */
  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  /** KB국민은행 계좌통합관리 담당자. */
  private static RequestPostProcessor kim() {
    return TestUsers.ganaMember();
  }

  /** 삼성증권 계좌통합관리 담당자. */
  private static RequestPostProcessor choi() {
    return TestUsers.as(TestUsers.SAA_MEMBER);
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

  /** KB국민은행·삼성증권 대상 계좌통합관리 기관 테스트를 홍길동이 만든다. */
  private Plan create(String title, String... items) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("subBusiness", "계좌정보 조회·해지");
    body.put("title", title);
    body.put("startDate", LocalDate.now().toString());
    body.put("endDate", LocalDate.now().plusDays(7).toString());
    body.put("items", Arrays.stream(items).map(t -> Map.of("title", t)).toList());
    body.put("organizationIds", List.of(orgId("004"), orgId("240")));
    JsonNode detail = json(send(post(BASE), body, hong()).andExpect(status().isCreated()));
    return new Plan(detail.get("id").asLong(), detail.get("items"));
  }

  private record Plan(Long id, JsonNode items) {
    Long item(int index) {
      return items.get(index).get("id").asLong();
    }
  }

  private String target(Plan plan, String orgCode) {
    return BASE + "/" + plan.id() + "/targets/" + orgId(orgCode);
  }

  private static Map<String, Object> entry(Long itemId, String result, String note) {
    Map<String, Object> m = new HashMap<>();
    m.put("itemId", itemId);
    m.put("result", result);
    m.put("note", note);
    return m;
  }

  @SafeVarargs
  private ResultActions report(
      Plan plan, String orgCode, RequestPostProcessor user, Map<String, Object>... entries)
      throws Exception {
    return send(put(target(plan, orgCode) + "/results"), Map.of("results", List.of(entries)), user);
  }

  private ResultActions feedback(Plan plan, String orgCode, int index, Integer rate, String note)
      throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("rate", rate);
    body.put("note", note);
    return send(
        put(target(plan, orgCode) + "/items/" + plan.item(index) + "/feedback"), body, hong());
  }

  private long count(NotificationType type, String mark) {
    return notificationRepository.findAll().stream()
        .filter(n -> n.getType() == type)
        .filter(n -> n.getTitle().contains(mark) || n.getBody().contains(mark))
        .count();
  }

  // ---- 결과 등록 ----

  @Test
  void 기관_담당자가_결과를_저장하면_수행률이_오르고_알림_대상에게_매터모스트가_간다() throws Exception {
    Plan plan = create("결과 등록 알림", "잔액 조회 확인", "해지 요청 확인");
    report(
            plan,
            "004",
            kim(),
            entry(plan.item(0), "PASSED", null),
            entry(plan.item(1), "FAILED", " "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TEST_RESULT_NOTE_REQUIRED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("results[1].note"));

    report(
            plan,
            "004",
            kim(),
            entry(plan.item(0), "PASSED", null),
            entry(plan.item(1), "FAILED", "해지 사유코드 09 를 처리하지 못합니다."))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.progressRate").value(50))
        .andExpect(jsonPath("$.summary.passed").value(1))
        .andExpect(jsonPath("$.summary.failed").value(1))
        .andExpect(jsonPath("$.summary.awaitingReview").value(2))
        .andExpect(jsonPath("$.rows[0].effectiveRate").value(100))
        .andExpect(jsonPath("$.rows[0].completed").value(false))
        .andExpect(jsonPath("$.rows[1].resultLabel").value("오류"))
        .andExpect(jsonPath("$.rows[1].reportedByName").value("김민준"))
        .andExpect(jsonPath("$.canReport").value(true))
        .andExpect(jsonPath("$.canFeedback").value(false));

    assertThat(
            notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.TEST_RESULT_REPORTED)
                .filter(n -> n.getTitle().contains("결과 등록 알림"))
                .map(n -> n.getRecipientEmail()))
        .containsExactly("hong.sungmin@kftc.example.com");
    assertThat(outboxRepository.findAll())
        .filteredOn(o -> o.getBody() != null && o.getBody().contains("002 오류"))
        .filteredOn(o -> o.getSubject().contains("결과 등록 알림"))
        .extracting(NotificationOutbox::getRecipient, NotificationOutbox::getChannel)
        .containsExactly(tuple("@hong.sungmin", NotificationChannel.MATTERMOST));

    // 바뀐 것이 없으면 다시 알리지 않는다.
    report(
            plan,
            "004",
            kim(),
            entry(plan.item(0), "PASSED", null),
            entry(plan.item(1), "FAILED", "해지 사유코드 09 를 처리하지 못합니다."))
        .andExpect(status().isOk());
    assertThat(count(NotificationType.TEST_RESULT_REPORTED, "결과 등록 알림")).isEqualTo(1);
    send(get(BASE + "/" + plan.id()), null, hong())
        .andExpect(jsonPath("$.progressRate").value(25))
        .andExpect(
            jsonPath("$.targets[?(@.organizationCode == '004')].lastReportedAt").isNotEmpty());
  }

  @Test
  void 다른_기관과_결제원은_기관_결과를_등록하지_못한다() throws Exception {
    Plan plan = create("등록 거부", "잔액 조회 확인");
    report(plan, "240", kim(), entry(plan.item(0), "PASSED", null))
        .andExpect(status().isForbidden());
    report(plan, "004", hong(), entry(plan.item(0), "PASSED", null))
        .andExpect(status().isForbidden());
    report(plan, "004", kim(), entry(999999L, "PASSED", null))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TEST_ITEM_INVALID"));
  }

  @Test
  void 기관_담당자는_자기_기관_결과만_본다() throws Exception {
    Plan plan = create("결과 조회 범위", "잔액 조회 확인");
    send(get(target(plan, "004")), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.organizationCode").value("004"))
        .andExpect(jsonPath("$.rows[0].result").isEmpty())
        .andExpect(jsonPath("$.summary.unreported").value(1));
    send(get(target(plan, "240")), null, hong()).andExpect(status().isOk());
    send(get(target(plan, "240")), null, kim()).andExpect(status().isForbidden());
    send(get(BASE + "/" + plan.id() + "/targets/" + orgId("050")), null, hong())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TEST_TARGET_NOT_FOUND"));
  }

  // ---- 결제원 의견 ----

  @Test
  void 결제원_의견_100과_기관_정상이면_요구사항이_수행_완료다() throws Exception {
    Plan plan = create("수행 완료 흐름", "잔액 조회 확인");
    feedback(plan, "004", 0, 100, null)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TEST_RESULT_NOT_REPORTED"));
    report(plan, "004", kim(), entry(plan.item(0), "PASSED", null)).andExpect(status().isOk());
    feedback(plan, "004", 0, 100, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].completed").value(true))
        .andExpect(jsonPath("$.rows[0].feedbackByName").value("홍성민"))
        .andExpect(jsonPath("$.summary.completed").value(true))
        .andExpect(jsonPath("$.summary.awaitingReview").value(0));
    // 삼성증권이 아직이라 테스트는 진행 중이다.
    send(get(BASE + "/" + plan.id()), null, hong())
        .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.progressRate").value(50));
    // 결과를 등록한 김가나에게 알림 센터로만 알린다.
    assertThat(count(NotificationType.TEST_RESULT_FEEDBACK, "수행 완료 흐름")).isEqualTo(1);
    assertThat(outboxRepository.findAll())
        .filteredOn(o -> o.getSubject() != null && o.getSubject().contains("결제원 의견"))
        .isEmpty();

    report(plan, "240", choi(), entry(plan.item(0), "PASSED", null)).andExpect(status().isOk());
    feedback(plan, "240", 0, 100, null).andExpect(status().isOk());
    send(get(BASE + "/" + plan.id()), null, hong())
        .andExpect(jsonPath("$.state").value("COMPLETED"))
        .andExpect(jsonPath("$.stateLabel").value("수행 완료"))
        .andExpect(jsonPath("$.progressRate").value(100));
    // 기관 담당자도 자기 기관 완료를 본다.
    send(get(BASE + "/" + plan.id()), null, kim())
        .andExpect(jsonPath("$.targets[0].completed").value(true));
  }

  @Test
  void 미니_수행률이_100이_아니면_그_값으로_집계하고_완료가_아니다() throws Exception {
    Plan plan = create("미니 수행률 집계", "해지 요청 확인");
    report(plan, "004", kim(), entry(plan.item(0), "PASSED", null));
    feedback(plan, "004", 0, 80, "003 전문에 대해 예외사항 발생")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].effectiveRate").value(80))
        .andExpect(jsonPath("$.rows[0].feedbackNote").value("003 전문에 대해 예외사항 발생"))
        .andExpect(jsonPath("$.rows[0].completed").value(false))
        .andExpect(jsonPath("$.summary.progressRate").value(80));
    // 둘 다 비우면 의견을 지우고 기관 결과(정상 100)로 돌아간다.
    feedback(plan, "004", 0, null, " ")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].feedbackRate").isEmpty())
        .andExpect(jsonPath("$.rows[0].effectiveRate").value(100));
  }

  @Test
  void 결제원_의견_뒤에_기관이_결과를_바꾸면_다시_확인이다() throws Exception {
    Plan plan = create("다시 확인 흐름", "한도 초과 응답 확인");
    report(plan, "004", kim(), entry(plan.item(0), "PASSED", null));
    feedback(plan, "004", 0, 70, "응답코드가 표준과 다릅니다.")
        .andExpect(jsonPath("$.rows[0].effectiveRate").value(70));

    clock.advance(Duration.ofMinutes(5));
    report(plan, "004", kim(), entry(plan.item(0), "PASSED", "응답코드를 고쳤습니다."))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].feedbackOutdated").value(true))
        .andExpect(jsonPath("$.rows[0].effectiveRate").value(100))
        .andExpect(jsonPath("$.rows[0].completed").value(false))
        .andExpect(jsonPath("$.summary.awaitingReview").value(1));

    clock.advance(Duration.ofMinutes(5));
    feedback(plan, "004", 0, 100, null)
        .andExpect(jsonPath("$.rows[0].feedbackOutdated").value(false))
        .andExpect(jsonPath("$.rows[0].completed").value(true));
  }

  @Test
  void 기관_담당자는_의견을_남기지_못하고_미니_수행률은_0에서_100이다() throws Exception {
    Plan plan = create("의견 거부", "잔액 조회 확인");
    report(plan, "004", kim(), entry(plan.item(0), "PASSED", null));
    send(
            put(target(plan, "004") + "/items/" + plan.item(0) + "/feedback"),
            Map.of("rate", 100),
            kim())
        .andExpect(status().isForbidden());
    feedback(plan, "004", 0, 101, null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("rate"));
  }

  // ---- 이력 ----

  @Test
  void 기관별_결과_이력은_결제원_담당자와_그_기관만_본다() throws Exception {
    Plan plan = create("결과 이력", "잔액 조회 확인");
    JsonNode detail = json(send(get(BASE + "/" + plan.id()), null, hong()));
    String gana = null;
    for (JsonNode t : detail.get("targets")) {
      if (t.get("organizationCode").asText().equals("004")) {
        gana = t.get("targetId").asText();
      }
    }
    report(plan, "004", kim(), entry(plan.item(0), "FAILED", "E99 응답"));
    for (RequestPostProcessor user : List.of(hong(), kim())) {
      send(
              get("/api/v1/change-histories")
                  .param("targetType", "TEST_PLAN_TARGET")
                  .param("targetId", gana),
              null,
              user)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].summary").value("KB국민은행 결과 등록"))
          .andExpect(jsonPath("$.content[0].changes[0].after").value("오류 · E99 응답"));
    }
    send(
            get("/api/v1/change-histories")
                .param("targetType", "TEST_PLAN_TARGET")
                .param("targetId", gana),
            null,
            choi())
        .andExpect(status().isForbidden());
  }
}
