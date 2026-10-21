package com.connectlink.testplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationOutbox;
import com.connectlink.notification.NotificationOutboxRepository;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.OrganizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 다. 기관 테스트 전 테스트데이터 준비: 기관 담당자의 생성 여부·메모 저장과 매터모스트 알림, 결제원 담당자의 전 기관 현황, 결제원이 지정하는 회신 양식과 기관
 * 테스트데이터 파일의 권한, 테스트데이터를 등록한 기관을 대상에서 빼지 못하는 규칙.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:test-data-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestDataControllerTest {

  private static final String BASE = "/api/v1/test-plans";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OrganizationService organizationService;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NotificationOutboxRepository outboxRepository;

  /** 결제원 계좌통합관리 현업. 새 테스트를 만들면 알림 대상이 된다. */
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

  /** 결제원 CMS 담당자(계좌통합관리를 맡지 않았다). */
  private static RequestPostProcessor cms() {
    return TestUsers.as(TestUsers.KFTC_CMS);
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

  private Map<String, Object> planBody(String title, List<Long> organizationIds) {
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("subBusiness", "계좌정보 조회·해지");
    body.put("title", title);
    body.put("startDate", LocalDate.now().toString());
    body.put("endDate", LocalDate.now().plusDays(7).toString());
    body.put("items", List.of(Map.of("title", "잔액 조회 확인")));
    body.put("organizationIds", organizationIds);
    return body;
  }

  /** KB국민은행·삼성증권 대상 계좌통합관리 기관 테스트를 홍성민이 만든다. */
  private Long create(String title) throws Exception {
    return json(send(post(BASE), planBody(title, List.of(orgId("004"), orgId("240"))), hong())
            .andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private String testData(Long planId, String orgCode) {
    return BASE + "/" + planId + "/targets/" + orgId(orgCode) + "/test-data";
  }

  private static Map<String, Object> data(String status, String note) {
    Map<String, Object> m = new HashMap<>();
    m.put("status", status);
    m.put("note", note);
    return m;
  }

  private ResultActions upload(String ownerType, Long ownerId, RequestPostProcessor user)
      throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "data.xlsx", "application/octet-stream", "샘플".getBytes(StandardCharsets.UTF_8));
    return mockMvc.perform(
        multipart("/api/v1/attachments")
            .file(file)
            .param("ownerType", ownerType)
            .param("ownerId", String.valueOf(ownerId))
            .with(csrf())
            .with(user));
  }

  @Test
  void 기관_담당자가_생성_완료로_저장하면_알림_대상에게_매터모스트가_가고_결제원은_현황을_본다() throws Exception {
    Long planId = create("테스트데이터 알림");
    send(get(testData(planId, "004")), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NOT_CREATED"))
        .andExpect(jsonPath("$.statusLabel").value("미생성"))
        .andExpect(jsonPath("$.canEdit").value(true));

    send(put(testData(planId, "004")), data("CREATED", "  테스트 계좌 10개  "), kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.note").value("테스트 계좌 10개"))
        .andExpect(jsonPath("$.reportedByName").value("김민준"))
        .andExpect(jsonPath("$.reportedAt").isNotEmpty());

    assertThat(outboxRepository.findAll())
        .filteredOn(o -> o.getSubject().contains("테스트데이터 알림"))
        .filteredOn(o -> o.getSubject().contains("생성 완료"))
        .extracting(NotificationOutbox::getRecipient, NotificationOutbox::getChannel)
        .containsExactly(tuple("@hong.sungmin", NotificationChannel.MATTERMOST));

    // 같은 내용을 다시 저장하거나 메모만 바꾸면 다시 알리지 않는다.
    send(put(testData(planId, "004")), data("CREATED", "테스트 계좌 10개"), kim())
        .andExpect(status().isOk());
    send(put(testData(planId, "004")), data("CREATED", "테스트 계좌 12개"), kim())
        .andExpect(status().isOk());
    assertThat(
            notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.TEST_DATA_REPORTED)
                .filter(n -> n.getTitle().contains("테스트데이터 알림")))
        .hasSize(1);

    send(get(BASE + "/" + planId), null, hong())
        .andExpect(
            jsonPath("$.targets[?(@.organizationCode == '004')].testDataStatus").value("CREATED"))
        .andExpect(
            jsonPath("$.targets[?(@.organizationCode == '240')].testDataStatusLabel").value("미생성"));
    send(get(testData(planId, "004")), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.note").value("테스트 계좌 12개"))
        .andExpect(jsonPath("$.canEdit").value(false));

    // 생성 여부와 메모 변경은 기관별 결과 이력에 남는다.
    Long targetId = json(send(get(testData(planId, "004")), null, kim())).get("targetId").asLong();
    send(
            get("/api/v1/change-histories")
                .param("targetType", "TEST_PLAN_TARGET")
                .param("targetId", String.valueOf(targetId)),
            null,
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].changes[0].label").value("테스트데이터 메모"));
  }

  @Test
  void 다른_기관과_결제원은_기관_테스트데이터를_저장하지_못한다() throws Exception {
    Long planId = create("테스트데이터 저장 거부");
    send(put(testData(planId, "004")), data("CREATED", null), choi())
        .andExpect(status().isForbidden());
    send(put(testData(planId, "004")), data("CREATED", null), hong())
        .andExpect(status().isForbidden());
    send(put(testData(planId, "004")), data(null, null), kim())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
  }

  @Test
  void 기관_담당자는_자기_기관_테스트데이터만_보고_다른_업무_결제원은_보지_못한다() throws Exception {
    Long planId = create("테스트데이터 조회 범위");
    send(get(testData(planId, "004")), null, choi()).andExpect(status().isForbidden());
    send(get(testData(planId, "240")), null, choi()).andExpect(status().isOk());
    send(get(testData(planId, "004")), null, cms()).andExpect(status().isForbidden());
  }

  @Test
  void 회신_양식은_결제원이_올리고_대상_기관이_내려받으며_테스트데이터_파일은_그_기관만_올린다() throws Exception {
    Long planId = create("테스트데이터 파일 권한");
    upload("TEST_DATA_FORM", planId, kim()).andExpect(status().isForbidden());
    upload("TEST_DATA_FORM", planId, hong()).andExpect(status().isCreated());
    send(
            get("/api/v1/attachments")
                .param("ownerType", "TEST_DATA_FORM")
                .param("ownerId", String.valueOf(planId)),
            null,
            kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    Long kbTarget = json(send(get(testData(planId, "004")), null, kim())).get("targetId").asLong();
    upload("TEST_DATA", kbTarget, choi()).andExpect(status().isForbidden());
    upload("TEST_DATA", kbTarget, hong()).andExpect(status().isForbidden());
    upload("TEST_DATA", kbTarget, kim()).andExpect(status().isCreated());
    send(get(testData(planId, "004")), null, hong()).andExpect(jsonPath("$.fileCount").value(1));
    send(
            get("/api/v1/attachments")
                .param("ownerType", "TEST_DATA")
                .param("ownerId", String.valueOf(kbTarget)),
            null,
            choi())
        .andExpect(status().isForbidden());
  }

  @Test
  void 테스트데이터를_등록한_기관은_대상에서_뺄_수_없다() throws Exception {
    Long planId = create("테스트데이터 대상 빼기");
    Long kbTarget = json(send(get(testData(planId, "004")), null, kim())).get("targetId").asLong();
    upload("TEST_DATA", kbTarget, kim()).andExpect(status().isCreated());
    send(put(BASE + "/" + planId), planBody("테스트데이터 대상 빼기", List.of(orgId("240"))), hong())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TEST_TARGET_HAS_TEST_DATA"));
    // 테스트데이터가 없는 기관은 뺄 수 있다.
    send(put(BASE + "/" + planId), planBody("테스트데이터 대상 빼기", List.of(orgId("004"))), hong())
        .andExpect(status().isOk());
    // 테스트를 지우면 테스트데이터 파일도 함께 지운다.
    send(delete(BASE + "/" + planId), null, hong()).andExpect(status().is2xxSuccessful());
  }
}
