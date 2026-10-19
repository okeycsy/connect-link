package com.connectlink.testplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.account.UserAccountRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * 다. 기관 테스트: 만들기·고치기·지우기와 권한, 목록·상세의 기관별 범위, 알림 대상 설정, 열림 알림, 가이드 첨부·이력·홈·검색 연결, 샘플 수행률. 시드 테스트는
 * 읽기만 하고, 쓰는 테스트는 새 테스트를 만들어 쓴다. 알림·발송함을 쌓으므로 따로 쓰는 DB 다.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:testplan-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestPlanControllerTest {

  private static final String BASE = "/api/v1/test-plans";

  /** 시드: 계좌통합관리 진행 중(KB국민은행 56%, 신한은행 40%, 삼성증권 미등록, 저축은행중앙회 100%, 전체 49%). */
  private static final String SEED_IN_PROGRESS = "계좌정보 통합조회 전문 개선 2차";

  /** 시드: 계좌통합관리 수행 완료(KB국민은행·신한은행·신협). 삼성증권은 대상이 아니다. */
  private static final String SEED_COMPLETED = "내 계좌 한눈에 연계 개선";

  /** 시드: 계좌통합관리 기간 지남(삼성증권·저축은행중앙회). KB국민은행은 대상이 아니다. */
  private static final String SEED_OVERDUE = "오픈뱅킹 안심차단 등록 전문 신설";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OrganizationService organizationService;
  @Autowired private UserAccountRepository accountRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NotificationOutboxRepository outboxRepository;
  @Autowired private TestPlanRepository testPlanRepository;

  /** 결제원 계좌통합관리 현업. */
  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  /** 결제원 CMS 현업(계좌통합관리 담당 아님). */
  private static RequestPostProcessor park() {
    return TestUsers.as(TestUsers.KFTC_CMS);
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

  private Long accountId(String email) {
    return accountRepository.findByEmail(email).orElseThrow().getId();
  }

  private Long seedPlanId(String title) {
    return testPlanRepository.findAll().stream()
        .filter(p -> p.getTitle().equals(title))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  /** 계좌통합관리 기관 테스트 양식. 개발 건 이름은 알림을 찾는 표지로 쓴다. */
  private Map<String, Object> form(String title, List<String> orgCodes, String... items) {
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("subBusiness", "계좌정보 조회·해지");
    body.put("title", title);
    body.put("description", "테스트 서버에서 확인합니다.");
    body.put("startDate", LocalDate.now().toString());
    body.put("endDate", LocalDate.now().plusDays(7).toString());
    body.put("items", Arrays.stream(items).map(t -> Map.of("title", t)).toList());
    body.put("organizationIds", orgCodes.stream().map(this::orgId).toList());
    return body;
  }

  private JsonNode create(String title, List<String> orgCodes, String... items) throws Exception {
    return json(
        send(post(BASE), form(title, orgCodes, items), hong()).andExpect(status().isCreated()));
  }

  /** 상세 응답의 요구사항을 고치기 양식의 items 로 바꾼다(id 포함). */
  private List<Map<String, Object>> itemsOf(JsonNode detail) {
    List<Map<String, Object>> items = new ArrayList<>();
    for (JsonNode item : detail.get("items")) {
      Map<String, Object> m = new HashMap<>();
      m.put("id", item.get("id").asLong());
      m.put("title", item.get("title").asText());
      items.add(m);
    }
    return items;
  }

  private void report(JsonNode plan, String orgCode, int itemIndex, String result)
      throws Exception {
    Long itemId = plan.get("items").get(itemIndex).get("id").asLong();
    send(
            put(BASE + "/" + plan.get("id").asLong() + "/targets/" + orgId(orgCode) + "/results"),
            Map.of("results", List.of(Map.of("itemId", itemId, "result", result))),
            "004".equals(orgCode) ? kim() : choi())
        .andExpect(status().isOk());
  }

  private List<String> notified(NotificationType type, String mark) {
    return notificationRepository.findAll().stream()
        .filter(n -> n.getType() == type)
        .filter(n -> n.getTitle().contains(mark) || n.getBody().contains(mark))
        .map(n -> n.getRecipientEmail())
        .toList();
  }

  private static JsonNode byTitle(JsonNode page, String title) {
    return StreamSupport.stream(page.get("content").spliterator(), false)
        .filter(p -> p.get("title").asText().equals(title))
        .findFirst()
        .orElseThrow(() -> new AssertionError("목록에 없음: " + title));
  }

  // ---- 만들기 ----

  @Test
  void 결제원_담당자가_만들면_바로_열리고_대상_기관_담당자에게_메일이_간다() throws Exception {
    send(post(BASE), form("만들기 알림 확인", List.of("004", "240"), "잔액 조회 확인", "해지 요청 확인"), hong())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.progressRate").value(0))
        .andExpect(jsonPath("$.items[0].number").value("001"))
        .andExpect(jsonPath("$.items[1].number").value("002"))
        .andExpect(jsonPath("$.targets.length()").value(2))
        .andExpect(jsonPath("$.targets[*].unreported").value(everyItem(is(2))))
        .andExpect(jsonPath("$.recipients[0]").value("홍길동"))
        .andExpect(jsonPath("$.ownerName").value("홍길동"))
        .andExpect(jsonPath("$.permissions.manage").value(true))
        .andExpect(jsonPath("$.permissions.report").value(false))
        .andExpect(jsonPath("$.permissions.feedback").value(true));

    assertThat(notified(NotificationType.TEST_PLAN_OPENED, "만들기 알림 확인"))
        .contains("kim.gana@kb.example.com", "choi.saa@samsung.example.com")
        .doesNotContain("yoon.chaka@fsb.example.com", "hong.gildong@kftc.example.com");
    assertThat(outboxRepository.findAll())
        .filteredOn(o -> o.getSubject() != null && o.getSubject().contains("만들기 알림 확인"))
        .filteredOn(o -> o.getRecipient().equals("kim.gana@kb.example.com"))
        .extracting(NotificationOutbox::getChannel)
        .containsExactly(NotificationChannel.EMAIL);
  }

  @Test
  void 기관_담당자와_다른_업무_결제원은_만들지_못한다() throws Exception {
    send(post(BASE), form("기관이 만들기", List.of("004"), "확인"), kim())
        .andExpect(status().isForbidden());
    send(post(BASE), form("다른 업무 결제원", List.of("004"), "확인"), park())
        .andExpect(status().isForbidden());
    send(get(BASE + "/organizations").param("business", "account-info"), null, kim())
        .andExpect(status().isForbidden());
  }

  @Test
  void 대상_기관_후보는_그_업무에_참여하는_참가기관이다() throws Exception {
    send(get(BASE + "/organizations").param("business", "account-info"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].code").value(hasItem("004")))
        .andExpect(jsonPath("$[*].code").value(not(hasItem("JACHA"))))
        .andExpect(jsonPath("$[*].code").value(not(hasItem("KFTC"))))
        .andExpect(jsonPath("$[?(@.code == '004')].sector").value(hasItem("BANK")));
  }

  @Test
  void 양식이_틀리면_필드별로_알려_준다() throws Exception {
    Map<String, Object> period = form("거꾸로 된 기간", List.of("004"), "확인");
    period.put("endDate", LocalDate.now().minusDays(1).toString());
    send(post(BASE), period, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TEST_PLAN_PERIOD_INVALID"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("endDate"));
    send(post(BASE), form("요구사항 없음", List.of("004")), hong())
        .andExpect(jsonPath("$.code").value("TEST_PLAN_ITEMS_REQUIRED"));
    send(post(BASE), form("대상 없음", List.of(), "확인"), hong())
        .andExpect(jsonPath("$.code").value("TEST_PLAN_TARGETS_REQUIRED"));
    // 자차카드는 계좌통합관리에 참여하지 않는다.
    send(post(BASE), form("참여 안 하는 기관", List.of("JACHA"), "확인"), hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TEST_TARGET_INVALID"));
    send(post(BASE), form(" ", List.of("004"), " "), hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("title")))
        .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("items[0].title")));
  }

  // ---- 조회 ----

  @Test
  void 결제원_담당자는_그_업무의_모든_테스트와_전체_수행률을_본다() throws Exception {
    JsonNode page = json(send(get(BASE).param("business", "account-info"), null, hong()));
    JsonNode inProgress = byTitle(page, SEED_IN_PROGRESS);
    assertThat(inProgress.get("progressRate").asInt()).isEqualTo(49);
    assertThat(inProgress.get("targetCount").asInt()).isEqualTo(4);
    assertThat(inProgress.get("completedTargetCount").asInt()).isZero();
    assertThat(byTitle(page, SEED_OVERDUE).get("overdue").asBoolean()).isTrue();

    send(get(BASE).param("business", "account-info").param("state", "COMPLETED"), null, hong())
        .andExpect(jsonPath("$.content[*].title").value(hasItem(SEED_COMPLETED)))
        .andExpect(jsonPath("$.content[*].state").value(everyItem(is("COMPLETED"))))
        .andExpect(jsonPath("$.content[*].progressRate").value(everyItem(is(100))));
    send(get(BASE + "/counts").param("business", "account-info"), null, hong())
        .andExpect(jsonPath("$.inProgress").isNumber())
        .andExpect(jsonPath("$.completed").isNumber());
  }

  @Test
  void 기관_담당자는_자기_기관이_대상인_테스트와_자기_기관_수행률만_본다() throws Exception {
    JsonNode page = json(send(get(BASE).param("business", "account-info"), null, kim()));
    assertThat(page.get("content").findValuesAsText("title"))
        .contains(SEED_IN_PROGRESS, SEED_COMPLETED)
        .doesNotContain(SEED_OVERDUE);
    JsonNode own = byTitle(page, SEED_IN_PROGRESS);
    assertThat(own.get("progressRate").asInt()).isEqualTo(56);
    assertThat(own.get("ownCompletedCount").asInt()).isEqualTo(2);
    assertThat(own.get("targetCount").isNull()).isTrue();

    send(get(BASE + "/" + seedPlanId(SEED_IN_PROGRESS)), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.progressRate").value(56))
        .andExpect(jsonPath("$.targets.length()").value(1))
        .andExpect(jsonPath("$.targets[0].organizationCode").value("004"))
        .andExpect(jsonPath("$.ownOrganizationId").value(orgId("004")))
        .andExpect(jsonPath("$.recipients.length()").value(0))
        .andExpect(jsonPath("$.permissions.manage").value(false))
        .andExpect(jsonPath("$.permissions.report").value(true))
        .andExpect(jsonPath("$.permissions.feedback").value(false));
  }

  @Test
  void 대상이_아닌_기관과_다른_업무_결제원은_보지_못한다() throws Exception {
    send(get(BASE + "/" + seedPlanId(SEED_COMPLETED)), null, choi())
        .andExpect(status().isForbidden());
    send(get(BASE + "/" + seedPlanId(SEED_IN_PROGRESS)), null, park())
        .andExpect(status().isForbidden());
    send(get(BASE).param("business", "cms"), null, kim()).andExpect(status().isForbidden());
    send(get(BASE + "/999999"), null, hong())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TEST_PLAN_NOT_FOUND"));
  }

  // ---- 고치기·지우기 ----

  @Test
  void 고치면_새_대상_기관에_알리고_결과가_있는_요구사항과_기관은_빼지_못한다() throws Exception {
    JsonNode plan = create("고치기 대상", List.of("004"), "잔액 조회 확인", "해지 요청 확인");
    Long id = plan.get("id").asLong();
    report(plan, "004", 0, "PASSED");
    List<Map<String, Object>> items = itemsOf(plan);

    Map<String, Object> dropItem = form("고치기 대상", List.of("004"));
    dropItem.put("items", List.of(items.get(1)));
    send(put(BASE + "/" + id), dropItem, hong())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TEST_ITEM_HAS_RESULTS"));

    Map<String, Object> dropTarget = form("고치기 대상", List.of("240"));
    dropTarget.put("items", items);
    send(put(BASE + "/" + id), dropTarget, hong())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TEST_TARGET_HAS_RESULTS"));

    Map<String, Object> business = form("고치기 대상", List.of("004"));
    business.put("items", items);
    business.put("business", "AUTO_TRANSFER");
    send(put(BASE + "/" + id), business, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TEST_PLAN_BUSINESS_FIXED"));

    Map<String, Object> changed = form("고치기 대상 개정", List.of("004", "240"));
    List<Map<String, Object>> newItems = new ArrayList<>(items);
    newItems.get(1).put("title", "해지 사유코드 확인");
    newItems.add(Map.of("title", "오류 응답코드 확인"));
    changed.put("items", newItems);
    send(put(BASE + "/" + id), changed, kim()).andExpect(status().isForbidden());
    send(put(BASE + "/" + id), changed, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("고치기 대상 개정"))
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[1].title").value("해지 사유코드 확인"))
        .andExpect(jsonPath("$.items[2].number").value("003"))
        .andExpect(jsonPath("$.targets.length()").value(2));
    // 새로 더한 삼성증권에만 열림 알림이 간다(KB국민은행은 만들 때 한 번).
    assertThat(notified(NotificationType.TEST_PLAN_OPENED, "고치기 대상 개정"))
        .containsExactly("choi.saa@samsung.example.com");

    send(
            get("/api/v1/change-histories")
                .param("targetType", "TEST_PLAN")
                .param("targetId", String.valueOf(id)),
            null,
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].action").value("UPDATED"))
        .andExpect(jsonPath("$.content[0].changes[*].field").value(hasItem("items")))
        .andExpect(jsonPath("$.content[0].changes[*].field").value(hasItem("targets")));
  }

  @Test
  void 요구사항이_늘면_수행_완료가_풀린다() throws Exception {
    JsonNode plan = create("완료 풀림 확인", List.of("004"), "잔액 조회 확인");
    Long id = plan.get("id").asLong();
    Long itemId = plan.get("items").get(0).get("id").asLong();
    report(plan, "004", 0, "PASSED");
    send(
            put(BASE + "/" + id + "/targets/" + orgId("004") + "/items/" + itemId + "/feedback"),
            Map.of("rate", 100),
            hong())
        .andExpect(status().isOk());
    send(get(BASE + "/" + id), null, hong())
        .andExpect(jsonPath("$.state").value("COMPLETED"))
        .andExpect(jsonPath("$.progressRate").value(100))
        .andExpect(jsonPath("$.completedAt").isNotEmpty());

    Map<String, Object> more = form("완료 풀림 확인", List.of("004"));
    List<Map<String, Object>> items = itemsOf(plan);
    items.add(Map.of("title", "해지 요청 확인"));
    more.put("items", items);
    send(put(BASE + "/" + id), more, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.progressRate").value(50))
        .andExpect(jsonPath("$.completedAt").isEmpty())
        .andExpect(jsonPath("$.targets[0].completedCount").value(1))
        .andExpect(jsonPath("$.targets[0].unreported").value(1));
  }

  @Test
  void 지우기는_결제원_담당자가_하고_기관_결과도_함께_지운다() throws Exception {
    JsonNode plan = create("지우기 대상", List.of("004"), "잔액 조회 확인");
    Long id = plan.get("id").asLong();
    report(plan, "004", 0, "PASSED");
    send(delete(BASE + "/" + id), null, kim()).andExpect(status().isForbidden());
    send(delete(BASE + "/" + id), null, park()).andExpect(status().isForbidden());
    send(delete(BASE + "/" + id), null, hong()).andExpect(status().isNoContent());
    send(get(BASE + "/" + id), null, hong()).andExpect(status().isNotFound());
  }

  // ---- 알림 대상 ----

  @Test
  void 알림_대상은_그_업무_결제원_담당자_중에서_고른다() throws Exception {
    Long id = create("알림 대상 설정", List.of("004"), "확인").get("id").asLong();
    send(get(BASE + "/" + id + "/recipients"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == '홍길동')].selected").value(hasItem(true)))
        .andExpect(jsonPath("$[?(@.name == '김운영')].selected").value(hasItem(false)))
        .andExpect(jsonPath("$[*].email").value(not(hasItem("kim.gana@kb.example.com"))))
        .andExpect(jsonPath("$[*].email").value(not(hasItem("park.cms@kftc.example.com"))));

    send(
            put(BASE + "/" + id + "/recipients"),
            Map.of(
                "accountIds",
                List.of(
                    accountId("kim.ops@kftc.example.com"),
                    accountId("lee.control@kftc.example.com"))),
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.selected == true)].name").value(hasItem("김운영")))
        .andExpect(jsonPath("$[?(@.selected == true)].name").value(hasItem("이상황")))
        .andExpect(jsonPath("$[?(@.selected == true)].name").value(not(hasItem("홍길동"))));
    send(get(BASE + "/" + id), null, hong()).andExpect(jsonPath("$.recipients.length()").value(2));

    send(
            put(BASE + "/" + id + "/recipients"),
            Map.of("accountIds", List.of(accountId("kim.gana@kb.example.com"))),
            hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TEST_RECIPIENT_INVALID"));
  }

  @Test
  void 기관_담당자와_다른_업무_결제원은_알림_대상을_보거나_바꾸지_못한다() throws Exception {
    Long id = create("알림 대상 거부", List.of("004"), "확인").get("id").asLong();
    send(get(BASE + "/" + id + "/recipients"), null, kim()).andExpect(status().isForbidden());
    send(get(BASE + "/" + id + "/recipients"), null, park()).andExpect(status().isForbidden());
    send(put(BASE + "/" + id + "/recipients"), Map.of("accountIds", List.of()), kim())
        .andExpect(status().isForbidden());
  }

  // ---- 첨부·이력·홈·검색 ----

  @Test
  void 가이드는_결제원_담당자가_올리고_대상_기관_담당자가_내려받는다() throws Exception {
    Long id = create("가이드 첨부", List.of("004"), "확인").get("id").asLong();
    MockMultipartFile file =
        new MockMultipartFile("file", "가이드.pdf", "application/pdf", "pdf".getBytes());
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(file)
                .param("ownerType", "TEST_PLAN")
                .param("ownerId", String.valueOf(id))
                .with(csrf())
                .with(kim()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(file)
                .param("ownerType", "TEST_PLAN")
                .param("ownerId", String.valueOf(id))
                .with(csrf())
                .with(hong()))
        .andExpect(status().isCreated());
    send(
            get("/api/v1/attachments")
                .param("ownerType", "TEST_PLAN")
                .param("ownerId", String.valueOf(id)),
            null,
            kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("가이드.pdf"));
    send(
            get("/api/v1/attachments")
                .param("ownerType", "TEST_PLAN")
                .param("ownerId", String.valueOf(id)),
            null,
            choi())
        .andExpect(status().isForbidden());
  }

  @Test
  void 테스트_이력은_결제원_담당자만_본다() throws Exception {
    Long id = create("이력 확인", List.of("004"), "확인").get("id").asLong();
    send(
            get("/api/v1/change-histories")
                .param("targetType", "TEST_PLAN")
                .param("targetId", String.valueOf(id)),
            null,
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].action").value("CREATED"));
    // 대상 기관 목록이 들어 있어 기관 담당자에게는 보이지 않는다.
    send(
            get("/api/v1/change-histories")
                .param("targetType", "TEST_PLAN")
                .param("targetId", String.valueOf(id)),
            null,
            kim())
        .andExpect(status().isForbidden());
  }

  @Test
  void 홈_기관_테스트_카드는_실제_데이터다() throws Exception {
    send(get("/api/v1/home").param("business", "account-info"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[?(@.key == 'TEST_PROGRESS')].source").value(hasItem("REAL")));
    send(get("/api/v1/home").param("business", "account-info"), null, kim())
        .andExpect(jsonPath("$.cards[?(@.key == 'TEST_PROGRESS')].source").value(hasItem("REAL")));
  }

  @Test
  void 검색은_개발_건으로_찾고_기관_담당자에게는_대상이_아닌_테스트가_나오지_않는다() throws Exception {
    send(get("/api/v1/search").param("business", "account-info").param("q", "안심차단"), null, hong())
        .andExpect(
            jsonPath("$.groups[?(@.category == 'TEST_PLAN')].hits[*].title")
                .value(hasItem(SEED_OVERDUE)));
    send(get("/api/v1/search").param("business", "account-info").param("q", "안심차단"), null, kim())
        .andExpect(
            jsonPath("$.groups[?(@.category == 'TEST_PLAN')].hits[*].title")
                .value(not(hasItem(containsString("안심차단")))));
  }

  // ---- 샘플 ----

  @Test
  @Transactional
  void 샘플_수행률은_서버_계산과_같다() {
    for (TestPlan plan : testPlanRepository.findAll()) {
      int rate = plan.getProgressRate();
      boolean completed = plan.getCompletedAt() != null;
      Map<Long, int[]> targets = new HashMap<>();
      plan.getTargets()
          .forEach(
              t -> targets.put(t.getId(), new int[] {t.getProgressRate(), t.getCompletedCount()}));

      TestProgress.recalculate(plan, LocalDateTime.now());

      assertThat(plan.getProgressRate()).as(plan.getTitle() + " 수행률").isEqualTo(rate);
      assertThat(plan.getCompletedAt() != null).as(plan.getTitle() + " 수행 완료").isEqualTo(completed);
      for (TestPlanTarget t : plan.getTargets()) {
        String label = plan.getTitle() + " " + t.getOrganization().getName();
        assertThat(t.getProgressRate()).as(label + " 수행률").isEqualTo(targets.get(t.getId())[0]);
        assertThat(t.getCompletedCount())
            .as(label + " 완료 요구사항")
            .isEqualTo(targets.get(t.getId())[1]);
      }
    }
  }
}
