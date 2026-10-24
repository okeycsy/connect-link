package com.connectlink.suspension;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationOutbox;
import com.connectlink.notification.NotificationOutboxRepository;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * 라. 일시중단: 등록·수정·삭제와 권한, 확인 시작·확정과 상태, 알림 채널, 목록·기관별 이력, 완료 처리, 전 영업일 취합, 공문·이력·홈·검색 연결. 시드 건은 읽기만
 * 하고, 쓰는 테스트는 새 건을 만들어 쓴다. 알림·발송함을 쌓으므로 따로 쓰는 DB 다.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:suspension-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SuspensionControllerTest {

  private static final String BASE = "/api/v1/suspensions";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OrganizationService organizationService;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NotificationOutboxRepository outboxRepository;
  @Autowired private SuspensionService suspensionService;
  @Autowired private TargetWorkRepository targetWorkRepository;

  /** 결제원 운영(계좌통합관리). 레일과 무관한 일시중단을 맡는다. */
  private static RequestPostProcessor ops() {
    return TestUsers.as(
        new CurrentUser(
            "kim.ops@kftc.example.com",
            "김태호",
            "KFTC",
            "금융결제원",
            OrganizationType.KFTC,
            false,
            Set.of(Business.ACCOUNT_INFO),
            JobRole.IT,
            Set.of(ItRole.OPS),
            "차장"));
  }

  /** 시드 대상 업무의 ID. */
  private Long work(String name) {
    return targetWorkRepository.findAll().stream()
        .filter(w -> w.getName().equals(name))
        .findFirst()
        .orElseThrow()
        .getId();
  }

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

  /** 삼성증권 계좌통합관리 담당자(다른 기관). */
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

  /** 모레 02:00~04:00 계좌통합관리 일시중단 양식. 중단 이유는 알림을 찾는 표지로 쓴다. */
  private Map<String, Object> form(String reason) {
    LocalDateTime start = LocalDate.now().plusDays(2).atTime(2, 0);
    Map<String, Object> body = new HashMap<>();
    body.put("targetWorkIds", List.of(work("계좌통합관리")));
    body.put("startAt", start.toString());
    body.put("endAt", start.plusHours(2).toString());
    body.put("reason", reason);
    body.put("kftcAction", "조회 차단");
    body.put("contacts", List.of(contact("김민준", "010-0000-2101", "kim.minjun@kb.example.com")));
    return body;
  }

  private static Map<String, Object> contact(String name, String phone, String email) {
    Map<String, Object> contact = new HashMap<>();
    contact.put("name", name);
    contact.put("phone", phone);
    contact.put("email", email);
    return contact;
  }

  private Long createAsKim(String reason) throws Exception {
    return json(send(post(BASE), form(reason), kim()).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private List<String> notified(String bodyMark) {
    return notificationRepository.findAll().stream()
        .filter(n -> n.getType() == NotificationType.SUSPENSION_CHANGED)
        .filter(n -> n.getBody().contains(bodyMark) || n.getTitle().contains(bodyMark))
        .map(n -> n.getRecipientEmail())
        .toList();
  }

  private List<NotificationOutbox> outbox(String bodyMark) {
    return outboxRepository.findAll().stream()
        .filter(o -> o.getBody() != null && o.getBody().contains(bodyMark))
        .toList();
  }

  // ---- 등록 ----

  @Test
  void 기관_담당자가_등록하면_자기_기관으로_검토_대기가_되고_결제원에_메일과_매터모스트가_간다() throws Exception {
    Map<String, Object> body = form("등록 알림 확인 작업");
    body.put("organizationId", orgId("240")); // 기관 담당자가 보낸 기관은 무시한다.
    send(post(BASE), body, kim())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
        .andExpect(jsonPath("$.organizationName").value("KB국민은행"))
        .andExpect(jsonPath("$.registrantKftc").value(false))
        .andExpect(jsonPath("$.registrantJobTitle").value("대리"))
        .andExpect(jsonPath("$.permissions.edit").value(true))
        .andExpect(jsonPath("$.permissions.delete").value(true))
        .andExpect(jsonPath("$.permissions.confirm").value(false));

    List<String> recipients = notified("등록 알림 확인 작업");
    assertThat(recipients)
        .contains(
            "hong.sungmin@kftc.example.com",
            "kim.ops@kftc.example.com",
            "lee.control@kftc.example.com",
            "admin@kftc.example.com")
        .doesNotContain("kim.minjun@kb.example.com", "park.cms@kftc.example.com");
    List<NotificationOutbox> sent = outbox("등록 알림 확인 작업");
    assertThat(sent)
        .filteredOn(o -> o.getRecipient().equals("hong.sungmin@kftc.example.com"))
        .extracting(NotificationOutbox::getChannel)
        .containsExactly(NotificationChannel.EMAIL);
    assertThat(sent)
        .filteredOn(o -> o.getRecipient().equals("@hong.sungmin"))
        .extracting(NotificationOutbox::getChannel)
        .containsExactly(NotificationChannel.MATTERMOST);
  }

  @Test
  void 결제원_담당자는_기관을_골라_등록한다() throws Exception {
    send(post(BASE), form("기관 미선택"), hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SUSPENSION_ORGANIZATION_REQUIRED"));
    Map<String, Object> body = form("결제원 대리 등록");
    body.put("organizationId", orgId("088"));
    send(post(BASE), body, hong())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.organizationName").value("신한은행"))
        .andExpect(jsonPath("$.registrantKftc").value(true));
  }

  @Test
  void 참가기관이_아닌_기관은_고를_수_없다() throws Exception {
    Map<String, Object> body = form("참가기관 아님");
    body.put("organizationId", orgId("KFTC"));
    send(post(BASE), body, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SUSPENSION_ORGANIZATION_INVALID"));
  }

  @Test
  void 기관_담당자는_자기_기관이_참여하는_업무면_맡지_않아도_등록하고_본인_건을_본다() throws Exception {
    Map<String, Object> body = form("기관 기준 CMS 등록");
    body.put("targetWorkIds", List.of(work("CMS"))); // 김민준은 계좌통합관리 담당이지만 KB국민은행은 CMS 에도 참여한다.
    Long id =
        json(send(post(BASE), body, kim())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businesses[0].code").value("CMS"))
                .andExpect(jsonPath("$.organizationName").value("KB국민은행")))
            .get("id")
            .asLong();
    send(get(BASE + "/" + id), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissions.edit").value(true));
    send(get(BASE + "/" + id), null, park()).andExpect(status().isOk());
    send(get(BASE + "/" + id), null, hong()).andExpect(status().isForbidden());
    assertThat(outbox("기관 기준 CMS 등록")).isNotEmpty();
  }

  @Test
  void 대상_업무는_기관_참여나_맡은_업무와_상관없이_고른다() throws Exception {
    Map<String, Object> body = form("참여 안 하는 업무");
    // 삼성증권은 대출이동에 참여하지 않고 최유진도 맡지 않지만 고를 수 있다.
    body.put("targetWorkIds", List.of(work("계좌통합관리"), work("대출이동")));
    send(post(BASE), body, choi())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.businesses.length()").value(2));
  }

  @Test
  void 레일과_무관한_대상_업무만_고르면_운영_상황실_대외계가_알림을_받고_확정한다() throws Exception {
    Map<String, Object> body = form("레일 무관 회선 작업");
    body.put("targetWorkIds", List.of(work("타행환공동망"), work("대외계 회선")));
    Long id =
        json(send(post(BASE), body, kim())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businesses.length()").value(0))
                .andExpect(jsonPath("$.targetWorks[0].name").value("타행환공동망")))
            .get("id")
            .asLong();
    assertThat(notified("레일 무관 회선 작업"))
        .contains("kim.ops@kftc.example.com", "lee.control@kftc.example.com")
        .doesNotContain("hong.sungmin@kftc.example.com", "park.cms@kftc.example.com");
    send(get(BASE + "/" + id), null, hong()).andExpect(status().isForbidden());
    send(get(BASE + "/" + id), null, kim()).andExpect(status().isOk());
    // 운영 담당자의 계좌통합관리 목록에 나온다.
    JsonNode list = json(send(get(BASE).param("business", "account-info"), null, ops()));
    assertThat(list.findValuesAsText("reason")).contains("레일 무관 회선 작업");
    JsonNode hongList = json(send(get(BASE).param("business", "account-info"), null, hong()));
    assertThat(hongList.findValuesAsText("reason")).doesNotContain("레일 무관 회선 작업");
    Map<String, Object> handover = new HashMap<>();
    handover.put("handover", "운영팀이 우회 경로를 열었습니다.");
    send(post(BASE + "/" + id + "/confirmation"), handover, ops())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void 사용_안_함_대상_업무는_새로_고르지_못한다() throws Exception {
    Map<String, Object> create = new HashMap<>();
    create.put("name", "테스트용 사용 안 함 업무");
    create.put("category", "기타");
    create.put("sortOrder", 999);
    create.put("active", false);
    create.put("businesses", List.of());
    Long inactive =
        json(send(post("/api/v1/target-works"), create, TestUsers.admin())
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    Map<String, Object> body = form("사용 안 함 업무 고르기");
    body.put("targetWorkIds", List.of(inactive));
    send(post(BASE), body, kim())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SUSPENSION_TARGET_WORK_INVALID"));
    JsonNode active = json(send(get("/api/v1/target-works"), null, kim()));
    assertThat(active.findValuesAsText("name")).doesNotContain("테스트용 사용 안 함 업무");
  }

  // ---- 대상 업무 기준정보 ----

  @Test
  void 대상_업무_목록은_로그인한_누구나_사용_중인_것을_순서대로_본다() throws Exception {
    JsonNode works =
        json(send(get("/api/v1/target-works"), null, kim()).andExpect(status().isOk()));
    assertThat(works.size()).isGreaterThanOrEqualTo(40);
    assertThat(works.get(0).get("name").asText()).isEqualTo("계좌통합관리");
    send(get("/api/v1/target-works").param("includeInactive", "true"), null, kim())
        .andExpect(status().isForbidden());
  }

  @Test
  void 관리자는_대상_업무를_더하고_고친다() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "테스트용 새 업무");
    body.put("category", "기타");
    body.put("sortOrder", 900);
    body.put("active", true);
    body.put("businesses", List.of("CMS"));
    Long id =
        json(send(post("/api/v1/target-works"), body, TestUsers.admin())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businesses[0].code").value("CMS")))
            .get("id")
            .asLong();
    body.put("name", "테스트용 고친 업무");
    body.put("businesses", List.of());
    send(put("/api/v1/target-works/" + id), body, TestUsers.admin())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("테스트용 고친 업무"))
        .andExpect(jsonPath("$.businesses.length()").value(0));
    body.put("name", "타행환공동망");
    send(put("/api/v1/target-works/" + id), body, TestUsers.admin())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TARGET_WORK_NAME_DUPLICATE"));
  }

  @Test
  void 관리자가_아니면_대상_업무를_더하지_못한다() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("name", "권한 없는 업무");
    body.put("sortOrder", 1);
    body.put("active", true);
    body.put("businesses", List.of());
    send(post("/api/v1/target-works"), body, hong()).andExpect(status().isForbidden());
  }

  @Test
  void 여러_업무를_멈추는_건은_한_건으로_등록되고_그_업무들의_결제원_담당자가_모두_다룬다() throws Exception {
    Map<String, Object> body = form("여러 업무 한 건 등록");
    body.put("targetWorkIds", List.of(work("CMS"), work("계좌통합관리"), work("CMS")));
    body.put(
        "contacts",
        List.of(
            contact("김민준", "010-0000-2101", "kim.minjun@kb.example.com"),
            contact("한예린", "010-0000-2102", null)));
    Long id =
        json(send(post(BASE), body, kim())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetWorks.length()").value(2))
                .andExpect(jsonPath("$.targetWorks[0].name").value("계좌통합관리"))
                .andExpect(jsonPath("$.businesses.length()").value(2))
                .andExpect(jsonPath("$.businesses[0].code").value("ACCOUNT_INFO"))
                .andExpect(jsonPath("$.businesses[1].code").value("CMS"))
                .andExpect(jsonPath("$.contacts.length()").value(2))
                .andExpect(jsonPath("$.contacts[1].name").value("한예린")))
            .get("id")
            .asLong();
    assertThat(notified("여러 업무 한 건 등록"))
        .contains("hong.sungmin@kftc.example.com", "park.cms@kftc.example.com");
    send(get(BASE + "/" + id), null, park())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissions.confirm").value(true));
    send(get(BASE + "/" + id), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissions.confirm").value(true));
    JsonNode cms = json(send(get(BASE).param("business", "cms"), null, park()));
    assertThat(cms.findValuesAsText("reason")).contains("여러 업무 한 건 등록");
    JsonNode accountInfo = json(send(get(BASE).param("business", "account-info"), null, hong()));
    assertThat(accountInfo.findValuesAsText("reason")).contains("여러 업무 한 건 등록");
  }

  @Test
  void 고치면서_업무를_빼고_담당자를_줄이면_그대로_남는다() throws Exception {
    Map<String, Object> body = form("업무 빼기");
    body.put("targetWorkIds", List.of(work("계좌통합관리"), work("CMS")));
    body.put(
        "contacts",
        List.of(contact("김민준", "010-0000-2101", null), contact("한예린", "010-0000-2102", null)));
    Long id = json(send(post(BASE), body, kim())).get("id").asLong();
    body.put("targetWorkIds", List.of(work("계좌통합관리")));
    body.put("contacts", List.of(contact("한예린", "010-0000-2102", null)));
    send(put(BASE + "/" + id), body, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.businesses.length()").value(1))
        .andExpect(jsonPath("$.contacts.length()").value(1))
        .andExpect(jsonPath("$.contacts[0].name").value("한예린"));
    // 빠진 업무(CMS)의 결제원 담당자는 그 뒤로 그 건을 보지 못한다.
    send(get(BASE + "/" + id), null, park()).andExpect(status().isForbidden());
  }

  @Test
  void 대상_업무와_담당자가_비면_필드_메시지가_온다() throws Exception {
    Map<String, Object> body = form("빈 업무");
    body.put("targetWorkIds", List.of());
    body.put("contacts", List.of());
    send(post(BASE), body, kim())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("targetWorkIds")))
        .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("contacts")));
  }

  @Test
  void 결제원은_맡지_않은_업무라도_기관이_참여하면_대리_등록하고_본인_건을_고친다() throws Exception {
    Map<String, Object> body = form("다른 업무 결제원 대리 등록");
    body.put("organizationId", orgId("004"));
    Long id =
        json(send(post(BASE), body, park())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrantKftc").value(true)))
            .get("id")
            .asLong();
    send(get(BASE + "/" + id), null, park())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissions.edit").value(true))
        .andExpect(jsonPath("$.permissions.confirm").value(false));
    send(get(BASE + "/" + id), null, hong()).andExpect(status().isOk());
  }

  @Test
  void 등록_기관_목록은_기관이_참여하는_업무를_함께_준다() throws Exception {
    send(get(BASE + "/registrable-organizations"), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].code").value("004"))
        .andExpect(jsonPath("$[0].businesses.length()").value(4));
    JsonNode all = json(send(get(BASE + "/registrable-organizations"), null, park()));
    assertThat(all.findValuesAsText("code")).contains("004", "088", "071");
  }

  @Test
  void 종료가_시작보다_앞서면_등록하지_못한다() throws Exception {
    Map<String, Object> body = form("거꾸로 된 일시");
    body.put("endAt", LocalDate.now().plusDays(2).atTime(1, 0).toString());
    send(post(BASE), body, kim())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SUSPENSION_PERIOD_INVALID"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("endAt"));
  }

  @Test
  void 양식_검증_메시지는_필드별로_온다() throws Exception {
    Map<String, Object> body = form("");
    body.put(
        "contacts", List.of(contact("김민준", "010-0000-2101", null), contact("한예린", "전화", null)));
    send(post(BASE), body, kim())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("reason")))
        .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("contacts[1].phone")));
  }

  // ---- 조회 ----

  @Test
  void 결제원_담당자는_그_업무의_진행_중과_예정을_본다() throws Exception {
    send(get(BASE).param("business", "account-info").param("period", "CURRENT"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].organizationName").value(hasItem("KB국민은행")))
        .andExpect(jsonPath("$.content[*].period").value(everyItem(is("CURRENT"))));
    send(get(BASE).param("business", "account-info").param("period", "UPCOMING"), null, hong())
        .andExpect(jsonPath("$.content[*].organizationName").value(hasItem("신한은행")))
        .andExpect(jsonPath("$.content[*].organizationName").value(hasItem("삼성증권")));
    send(get(BASE + "/counts").param("business", "account-info"), null, hong())
        .andExpect(jsonPath("$.current").isNumber())
        .andExpect(jsonPath("$.pendingReview").isNumber());
  }

  @Test
  void 결제원_담당자는_기관별_과거_이력을_본다() throws Exception {
    send(
            get(BASE)
                .param("business", "account-info")
                .param("period", "PAST")
                .param("organizationId", String.valueOf(orgId("004"))),
            null,
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].status").value(hasItem("COMPLETED")))
        .andExpect(jsonPath("$.content[*].period").value(everyItem(is("PAST"))))
        .andExpect(jsonPath("$.content[*].organizationName").value(everyItem(is("KB국민은행"))));
  }

  @Test
  void 기관_담당자는_자기_기관_것만_본다() throws Exception {
    send(get(BASE).param("business", "account-info"), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].organizationName").value(everyItem(is("KB국민은행"))));
    // 기관을 골라도 자기 기관 밖으로 넓어지지 않는다.
    send(
            get(BASE)
                .param("business", "account-info")
                .param("organizationId", String.valueOf(orgId("240"))),
            null,
            kim())
        .andExpect(jsonPath("$.content[*].organizationName").value(everyItem(is("KB국민은행"))));
    send(get(BASE + "/organizations").param("business", "account-info"), null, kim())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].code").value("004"));
  }

  @Test
  void 다른_기관이나_다른_업무의_건은_보지_못한다() throws Exception {
    Long id = createAsKim("다른 기관 조회 거부");
    send(get(BASE + "/" + id), null, choi()).andExpect(status().isForbidden());
    send(get(BASE + "/" + id), null, park()).andExpect(status().isForbidden());
    send(get(BASE).param("business", "cms"), null, kim()).andExpect(status().isForbidden());
  }

  // ---- 검토·확정 ----

  @Test
  void 결제원_담당자가_확인을_시작하면_검토_중_조치를_쓰면_확정이다() throws Exception {
    Long id = createAsKim("확정 흐름 작업");
    send(post(BASE + "/" + id + "/review"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_REVIEW"))
        .andExpect(jsonPath("$.reviewers[0].name").value("홍성민"))
        .andExpect(jsonPath("$.permissions.startReview").value(false));
    send(
            post(BASE + "/" + id + "/confirmation"),
            Map.of("handover", "운영팀이 조회를 차단합니다."),
            TestUsers.admin())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.handover").value("운영팀이 조회를 차단합니다."))
        .andExpect(jsonPath("$.confirmedByName").value("데모 관리자"))
        .andExpect(jsonPath("$.reviewers.length()").value(2));
    // 등록한 기관 담당자에게 확정 알림이 간다.
    assertThat(
            notificationRepository.findAll().stream()
                .filter(n -> n.getRecipientEmail().equals("kim.minjun@kb.example.com"))
                .filter(n -> n.getTitle().contains("확정")))
        .isNotEmpty();
  }

  @Test
  void 기관_담당자는_확인하거나_확정하지_못한다() throws Exception {
    Long id = createAsKim("기관 확정 거부");
    send(post(BASE + "/" + id + "/review"), null, kim()).andExpect(status().isForbidden());
    send(post(BASE + "/" + id + "/confirmation"), Map.of("handover", "확정"), kim())
        .andExpect(status().isForbidden());
    send(post(BASE + "/" + id + "/confirmation"), Map.of("handover", " "), hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("handover"));
  }

  // ---- 수정·삭제 ----

  @Test
  void 기관이_확정된_건을_고치면_검토_대기로_돌아가고_결제원에_매터모스트가_간다() throws Exception {
    Long id = createAsKim("기관 수정 전 작업");
    send(post(BASE + "/" + id + "/confirmation"), Map.of("handover", "차단"), hong())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    Map<String, Object> changed = form("기관 수정 뒤 작업");
    send(put(BASE + "/" + id), changed, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
        .andExpect(jsonPath("$.reviewers.length()").value(0))
        .andExpect(jsonPath("$.handover").value("차단"));
    List<NotificationOutbox> sent =
        outboxRepository.findAll().stream()
            .filter(o -> o.getSubject().contains("수정되었습니다"))
            .filter(o -> o.getBody().contains("중단 이유"))
            .toList();
    assertThat(sent).isNotEmpty();
    assertThat(sent)
        .extracting(NotificationOutbox::getChannel)
        .containsOnly(NotificationChannel.MATTERMOST);
    assertThat(sent).extracting(NotificationOutbox::getRecipient).contains("@hong.sungmin");
  }

  @Test
  void 결제원_담당자는_기관이_올린_건을_고치고_상태는_그대로다() throws Exception {
    Long id = createAsKim("결제원 수정 대상");
    send(post(BASE + "/" + id + "/review"), null, hong());
    Map<String, Object> changed = form("결제원 수정 대상");
    changed.put("kftcAction", "결제원이 고친 조치사항");
    send(put(BASE + "/" + id), changed, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_REVIEW"))
        .andExpect(jsonPath("$.kftcAction").value("결제원이 고친 조치사항"));
    assertThat(
            notificationRepository.findAll().stream()
                .filter(n -> n.getRecipientEmail().equals("kim.minjun@kb.example.com"))
                .filter(n -> n.getTitle().contains("결제원이 고쳤습니다")))
        .isNotEmpty();
  }

  @Test
  void 다른_기관_담당자와_다른_업무_결제원은_고치거나_지우지_못한다() throws Exception {
    Long id = createAsKim("수정 거부 대상");
    send(put(BASE + "/" + id), form("수정 거부 대상"), choi()).andExpect(status().isForbidden());
    send(put(BASE + "/" + id), form("수정 거부 대상"), park()).andExpect(status().isForbidden());
    send(delete(BASE + "/" + id), null, choi()).andExpect(status().isForbidden());
  }

  @Test
  void 삭제는_등록한_본인만_하고_결제원에_매터모스트가_간다() throws Exception {
    Long id = createAsKim("삭제 대상 작업");
    send(delete(BASE + "/" + id), null, hong()).andExpect(status().isForbidden());
    send(delete(BASE + "/" + id), null, kim()).andExpect(status().isNoContent());
    send(get(BASE + "/" + id), null, hong()).andExpect(status().isNotFound());
    assertThat(outbox("삭제 대상 작업"))
        .filteredOn(o -> o.getSubject().contains("삭제되었습니다"))
        .isNotEmpty()
        .extracting(NotificationOutbox::getChannel)
        .containsOnly(NotificationChannel.MATTERMOST);
  }

  @Test
  void 끝난_확정_건은_완료가_되고_더_고치지_못한다() throws Exception {
    Map<String, Object> body = form("이미 끝난 작업");
    body.put("startAt", LocalDate.now().minusDays(3).atTime(1, 0).toString());
    body.put("endAt", LocalDate.now().minusDays(3).atTime(3, 0).toString());
    Long id = json(send(post(BASE), body, kim())).get("id").asLong();
    send(post(BASE + "/" + id + "/confirmation"), Map.of("handover", "끝"), hong());

    assertThat(suspensionService.completeEnded()).isGreaterThanOrEqualTo(1);
    send(get(BASE + "/" + id), null, kim())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.period").value("PAST"))
        .andExpect(jsonPath("$.permissions.edit").value(false))
        .andExpect(jsonPath("$.permissions.delete").value(false));
    send(put(BASE + "/" + id), body, hong())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SUSPENSION_COMPLETED"));
  }

  // ---- 공문·이력 ----

  @Test
  void 공문은_고칠_수_있는_사람이_올리고_볼_수_있는_사람이_내려받는다() throws Exception {
    Long id = createAsKim("공문 첨부 작업");
    MockMultipartFile file =
        new MockMultipartFile("file", "공문.pdf", "application/pdf", "pdf".getBytes());
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(file)
                .param("ownerType", "SUSPENSION")
                .param("ownerId", String.valueOf(id))
                .with(csrf())
                .with(kim()))
        .andExpect(status().isCreated());
    send(
            get("/api/v1/attachments")
                .param("ownerType", "SUSPENSION")
                .param("ownerId", String.valueOf(id)),
            null,
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("공문.pdf"));
    send(
            get("/api/v1/attachments")
                .param("ownerType", "SUSPENSION")
                .param("ownerId", String.valueOf(id)),
            null,
            choi())
        .andExpect(status().isForbidden());
  }

  @Test
  void 변경_이력은_볼_수_있는_사람만_본다() throws Exception {
    Long id = createAsKim("이력 확인 작업");
    send(
            get("/api/v1/change-histories")
                .param("targetType", "SUSPENSION")
                .param("targetId", String.valueOf(id)),
            null,
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].action").value("CREATED"));
    send(
            get("/api/v1/change-histories")
                .param("targetType", "SUSPENSION")
                .param("targetId", String.valueOf(id)),
            null,
            choi())
        .andExpect(status().isForbidden());
  }

  // ---- 홈·검색·취합 ----

  @Test
  void 홈_일시중단_카드는_실제_데이터다() throws Exception {
    send(get("/api/v1/home").param("business", "account-info"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[?(@.key == 'SUSPENSIONS')].source").value(hasItem("REAL")))
        .andExpect(
            jsonPath("$.cards[?(@.key == 'SUSPENSIONS')].title").value(hasItem("다가오는 일시중단")));
  }

  @Test
  void 검색은_기관_이름으로_찾고_기관_담당자에게는_다른_기관이_나오지_않는다() throws Exception {
    send(get("/api/v1/search").param("business", "account-info").param("q", "삼성"), null, hong())
        .andExpect(
            jsonPath("$.groups[?(@.category == 'SUSPENSION')].hits[*].title")
                .value(hasItem(org.hamcrest.Matchers.containsString("삼성증권"))));
    send(get("/api/v1/search").param("business", "account-info").param("q", "삼성"), null, kim())
        .andExpect(
            jsonPath("$.groups[?(@.category == 'SUSPENSION')].hits[*].title")
                .value(not(hasItem(org.hamcrest.Matchers.containsString("삼성증권")))));
  }

  @Test
  void 전_영업일_취합은_확정_건을_운영_관제_채널로_보낸다() throws Exception {
    JsonNode digest =
        json(send(post("/api/v1/suspension-digests"), null, hong()).andExpect(status().isOk()));
    assertThat(digest.get("recipients").get(0).asText()).isEqualTo("~kftc-ops");
    assertThat(digest.get("body").asText()).contains("확정 일시중단");
    assertThat(
            outboxRepository.findAll().stream()
                .filter(o -> o.getRecipient().equals("~kftc-control-room"))
                .map(NotificationOutbox::getChannel))
        .contains(NotificationChannel.MATTERMOST);
    send(post("/api/v1/suspension-digests"), null, kim()).andExpect(status().isForbidden());
  }
}
