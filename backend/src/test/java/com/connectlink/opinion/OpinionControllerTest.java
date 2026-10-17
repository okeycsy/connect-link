package com.connectlink.opinion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.account.AccountService;
import com.connectlink.account.BusinessMember;
import com.connectlink.auth.TestUsers;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 * 나. 의견 회신 취합: 요청 작성·회신 요청 보내기·수정·마감·취소·삭제, 기관 회신(임시저장·제출·수정 요청), 회신 현황·리마인드·엑셀, 첨부·이력 권한. 시드 요청은
 * 읽기만 하고, 쓰는 테스트는 새 요청을 만들어 쓴다. 알림·발송함을 쌓으므로 따로 쓰는 DB 다.
 */
@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:opinion-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpinionControllerTest {

  private static final String BASE = "/api/v1/opinion-requests";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OrganizationService organizationService;
  @Autowired private AccountService accountService;
  @Autowired private NotificationRepository notificationRepository;

  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  private static RequestPostProcessor park() {
    return TestUsers.as(TestUsers.KFTC_CMS);
  }

  private static RequestPostProcessor kim() {
    return TestUsers.ganaMember();
  }

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
    return accountService.listActiveMembers(Business.ACCOUNT_INFO).stream()
        .filter(m -> m.email().equals(email))
        .map(BusinessMember::accountId)
        .findFirst()
        .orElseThrow();
  }

  /** 가나은행(김가나, 은행권 양식)과 사아증권(최사아)이 대상인 새 요청. */
  private Map<String, Object> newRequest(String title) {
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("title", title);
    body.put("content", "의견을 회신해 주세요.");
    body.put("dueDate", LocalDate.now().plusDays(5).toString());
    body.put("forms", List.of(Map.of("name", "은행권", "description", "은행용 양식")));
    body.put(
        "targets",
        List.of(
            Map.of(
                "organizationId",
                orgId("GANA"),
                "formName",
                "은행권",
                "recipientAccountIds",
                List.of(accountId("kim.gana@gana.example.com"))),
            Map.of(
                "organizationId",
                orgId("SAA"),
                "recipientAccountIds",
                List.of(accountId("choi.saa@saa.example.com")))));
    return body;
  }

  private Long create(String title) throws Exception {
    return json(send(post(BASE), newRequest(title), hong()).andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private Long createOpen(String title) throws Exception {
    Long id = create(title);
    send(post(BASE + "/" + id + "/opening"), null, hong()).andExpect(status().isOk());
    return id;
  }

  // ---- 목록·상세 ----

  @Test
  void 결제원_담당자는_그_업무의_요청을_상태별로_본다() throws Exception {
    send(get(BASE).param("business", "account-info").param("status", "OPEN"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].title").value(hasItem("계좌통합관리 수수료 체계 개편 의견 요청")))
        .andExpect(jsonPath("$.content[0].targetCount").isNumber());
    send(get(BASE).param("business", "account-info").param("status", "DRAFT"), null, hong())
        .andExpect(jsonPath("$.content[*].title").value(hasItem("야간 배치 시간 조정 의견 요청")));
  }

  @Test
  void 다른_업무의_결제원_담당자는_목록을_보지_못한다() throws Exception {
    send(get(BASE).param("business", "account-info"), null, park())
        .andExpect(status().isForbidden());
  }

  @Test
  void 기관_담당자는_자기_기관이_대상인_요청만_보고_작성중은_보지_못한다() throws Exception {
    send(get(BASE).param("business", "account-info").param("status", "OPEN"), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].title").value(hasItem("오픈뱅킹 연계 전문 항목 추가 검토")))
        .andExpect(jsonPath("$.content[0].targetCount").doesNotExist());
    send(get(BASE).param("business", "account-info").param("status", "DRAFT"), null, kim())
        .andExpect(jsonPath("$.totalElements").value(0));
    send(get(BASE + "/counts").param("business", "account-info"), null, kim())
        .andExpect(jsonPath("$.draft").value(0));
  }

  @Test
  void 기관_담당자의_상세에는_자기_기관_행만_있다() throws Exception {
    Long id = createOpen("상세 범위 확인");
    send(get(BASE + "/" + id), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targets.length()").value(1))
        .andExpect(jsonPath("$.targets[0].organizationCode").value("GANA"))
        .andExpect(jsonPath("$.forms[0].name").value("은행권"))
        .andExpect(jsonPath("$.permissions.reply").value(true))
        .andExpect(jsonPath("$.permissions.manage").value(false));
    send(get(BASE + "/" + id), null, choi()).andExpect(jsonPath("$.forms.length()").value(0));
  }

  @Test
  void 작성중_요청은_대상_기관도_열지_못한다() throws Exception {
    Long id = create("작성중 숨김");
    send(get(BASE + "/" + id), null, kim()).andExpect(status().isForbidden());
  }

  @Test
  void 회신_담당자_후보는_그_업무의_결제원_담당자만_본다() throws Exception {
    send(get(BASE + "/recipient-candidates").param("business", "account-info"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.organizationCode == 'GANA')].members[0].name").value("김가나"))
        .andExpect(jsonPath("$[?(@.organizationCode == 'GANA')].sectorLabel").value("은행"))
        .andExpect(jsonPath("$[?(@.organizationCode == 'SAA')].sector").value("SECURITIES"))
        .andExpect(jsonPath("$[*].organizationCode").value(not(hasItem("KFTC"))))
        .andExpect(jsonPath("$[*].organizationCode").value(not(hasItem("HAHA"))));
    send(get(BASE + "/recipient-candidates").param("business", "account-info"), null, kim())
        .andExpect(status().isForbidden());
  }

  // ---- 작성·상태 관리 ----

  @Test
  void 회신_요청을_보내면_회신_담당자에게_알림과_메일이_간다() throws Exception {
    Long id = create("알림 확인 요청");
    send(post(BASE + "/" + id + "/opening"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.permissions.close").value(true));
    assertThat(
            notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.OPINION_REQUEST_OPENED)
                .filter(n -> n.getTitle().contains("알림 확인 요청"))
                .map(n -> n.getRecipientEmail()))
        .containsExactlyInAnyOrder("kim.gana@gana.example.com", "choi.saa@saa.example.com");
    JsonNode mails =
        json(
            mockMvc.perform(
                get("/api/v1/demo/mailbox").param("email", "kim.gana@gana.example.com")));
    assertThat(mails.get(0).get("subject").asText()).contains("알림 확인 요청");
  }

  @Test
  void 대상이_없으면_회신_요청을_보낼_수_없다() throws Exception {
    Map<String, Object> body = newRequest("대상 없음");
    body.put("targets", List.of());
    Long id =
        json(send(post(BASE), body, hong()).andExpect(status().isCreated())).get("id").asLong();
    send(post(BASE + "/" + id + "/opening"), null, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("OPINION_TARGETS_REQUIRED"));
  }

  @Test
  void 지난_기한과_다른_기관_담당자는_받지_않는다() throws Exception {
    Map<String, Object> past = newRequest("지난 기한");
    past.put("dueDate", LocalDate.now().minusDays(1).toString());
    send(post(BASE), past, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"));

    Map<String, Object> wrong = newRequest("다른 기관 담당자");
    wrong.put(
        "targets",
        List.of(
            Map.of(
                "organizationId",
                orgId("SAA"),
                "recipientAccountIds",
                List.of(accountId("kim.gana@gana.example.com")))));
    send(post(BASE), wrong, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("OPINION_RECIPIENT_INVALID"));

    Map<String, Object> notParticipating = newRequest("참여하지 않는 기관");
    notParticipating.put(
        "targets",
        List.of(Map.of("organizationId", orgId("JACHA"), "recipientAccountIds", List.of())));
    send(post(BASE), notParticipating, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("OPINION_TARGET_INVALID"));
  }

  @Test
  void 기관_담당자와_다른_업무_담당자는_요청을_만들지_못한다() throws Exception {
    send(post(BASE), newRequest("기관이 만든 요청"), kim()).andExpect(status().isForbidden());
    send(post(BASE), newRequest("CMS 담당자가 만든 요청"), park()).andExpect(status().isForbidden());
  }

  @Test
  void 회신을_저장한_기관은_대상에서_뺄_수_없다() throws Exception {
    Long id = createOpen("대상 빼기");
    send(put(BASE + "/" + id + "/my-reply"), Map.of("type", "AGREE"), kim())
        .andExpect(status().isOk());
    Map<String, Object> body = newRequest("대상 빼기");
    body.remove("business");
    body.put(
        "targets",
        List.of(
            Map.of(
                "organizationId",
                orgId("SAA"),
                "recipientAccountIds",
                List.of(accountId("choi.saa@saa.example.com")))));
    send(put(BASE + "/" + id), body, hong())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OPINION_TARGET_HAS_REPLY"));
  }

  @Test
  void 작성중은_지우고_회신중은_취소한다() throws Exception {
    Long draft = create("지울 요청");
    send(delete(BASE + "/" + draft), null, hong()).andExpect(status().isNoContent());
    send(get(BASE + "/" + draft), null, hong()).andExpect(status().isNotFound());

    Long open = createOpen("취소할 요청");
    send(delete(BASE + "/" + open), null, hong()).andExpect(status().isConflict());
    send(post(BASE + "/" + open + "/cancellation"), Map.of("reason", "다른 요청과 합칩니다."), hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.cancelled").value(true))
        .andExpect(jsonPath("$.cancelReason").value("다른 요청과 합칩니다."));
    send(put(BASE + "/" + open + "/my-reply"), Map.of("type", "AGREE"), kim())
        .andExpect(status().isConflict());
  }

  // ---- 기관 회신 ----

  @Test
  void 기관_담당자는_임시저장하고_최종제출한다() throws Exception {
    Long id = createOpen("회신 흐름");
    send(get(BASE + "/" + id + "/my-reply"), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.replyId").isEmpty())
        .andExpect(jsonPath("$.formName").value("은행권"))
        .andExpect(jsonPath("$.editable").value(true));

    send(put(BASE + "/" + id + "/my-reply"), Map.of("content", "검토 중"), kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.draftContent").value("검토 중"))
        .andExpect(jsonPath("$.content").isEmpty());

    send(post(BASE + "/" + id + "/my-reply/submission"), Map.of("content", "검토 중"), kim())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("type"));
    send(post(BASE + "/" + id + "/my-reply/submission"), Map.of("type", "DISAGREE"), kim())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));

    send(post(BASE + "/" + id + "/my-reply/submission"), Map.of("type", "AGREE"), kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"))
        .andExpect(jsonPath("$.typeLabel").value("동의"))
        .andExpect(jsonPath("$.editable").value(false))
        .andExpect(jsonPath("$.revisionRequestable").value(true));

    send(put(BASE + "/" + id + "/my-reply"), Map.of("type", "DISAGREE"), kim())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OPINION_REPLY_NOT_EDITABLE"));
  }

  @Test
  void 대상이_아닌_기관과_결제원은_회신하지_못한다() throws Exception {
    Map<String, Object> body = newRequest("가나만 대상");
    body.put(
        "targets",
        List.of(Map.of("organizationId", orgId("GANA"), "recipientAccountIds", List.of())));
    body.put("forms", List.of());
    Long id =
        json(send(post(BASE), body, hong()).andExpect(status().isCreated())).get("id").asLong();
    send(post(BASE + "/" + id + "/opening"), null, hong()).andExpect(status().isOk());
    send(put(BASE + "/" + id + "/my-reply"), Map.of("type", "AGREE"), choi())
        .andExpect(status().isForbidden());
    send(put(BASE + "/" + id + "/my-reply"), Map.of("type", "AGREE"), hong())
        .andExpect(status().isForbidden());
  }

  @Test
  void 제출한_회신은_수정_요청을_승인받은_뒤_고쳐_다시_제출한다() throws Exception {
    Long id = createOpen("수정 요청 흐름");
    send(post(BASE + "/" + id + "/my-reply/submission"), Map.of("type", "AGREE"), kim())
        .andExpect(status().isOk());
    Long replyId =
        json(send(
                    post(BASE + "/" + id + "/my-reply/revision-request"),
                    Map.of("reason", "조건을 더하려고 합니다."),
                    kim())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVISION_REQUESTED")))
            .get("replyId")
            .asLong();
    assertThat(
            notificationRepository.findAll().stream()
                .anyMatch(
                    n ->
                        n.getType() == NotificationType.OPINION_REPLY_REVISION
                            && n.getRecipientEmail().equals("hong.gildong@kftc.example.com")
                            && n.getTitle().contains("수정 요청 흐름")))
        .isTrue();

    send(post(BASE + "/" + id + "/replies/" + replyId + "/revision-approval"), null, choi())
        .andExpect(status().isForbidden());
    send(post(BASE + "/" + id + "/replies/" + replyId + "/revision-approval"), null, hong())
        .andExpect(status().isNoContent());

    send(get(BASE + "/" + id + "/my-reply"), null, kim())
        .andExpect(jsonPath("$.status").value("REVISING"))
        .andExpect(jsonPath("$.draftType").value("AGREE"))
        .andExpect(jsonPath("$.type").value("AGREE"));
    send(
            post(BASE + "/" + id + "/my-reply/submission"),
            Map.of("type", "CONDITIONAL", "content", "7월 적용이면 동의합니다."),
            kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"))
        .andExpect(jsonPath("$.type").value("CONDITIONAL"));

    send(
            get("/api/v1/change-histories")
                .param("targetType", "OPINION_REPLY")
                .param("targetId", replyId.toString()),
            null,
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].changes[0].before").value("동의"))
        .andExpect(jsonPath("$.content[0].changes[0].after").value("조건부 동의"));
    send(
            get("/api/v1/change-histories")
                .param("targetType", "OPINION_REPLY")
                .param("targetId", replyId.toString()),
            null,
            choi())
        .andExpect(status().isForbidden());
  }

  @Test
  void 수정_요청을_반려하면_사유와_함께_제출_상태로_돌아간다() throws Exception {
    Long id = createOpen("수정 요청 반려");
    send(post(BASE + "/" + id + "/my-reply/submission"), Map.of("type", "AGREE"), kim());
    Long replyId =
        json(send(
                post(BASE + "/" + id + "/my-reply/revision-request"),
                Map.of("reason", "고치고 싶습니다."),
                kim()))
            .get("replyId")
            .asLong();
    send(
            post(BASE + "/" + id + "/replies/" + replyId + "/revision-rejection"),
            Map.of("reason", "마감이 가까워 그대로 둡니다."),
            hong())
        .andExpect(status().isNoContent());
    send(get(BASE + "/" + id + "/my-reply"), null, kim())
        .andExpect(jsonPath("$.status").value("SUBMITTED"))
        .andExpect(jsonPath("$.revisionRejectionReason").value("마감이 가까워 그대로 둡니다."));
  }

  // ---- 회신 현황·취합 ----

  @Test
  void 회신_현황은_회신_여부와_유형별_기관을_자동으로_센다() throws Exception {
    Long id = createOpen("현황 집계");
    send(
        post(BASE + "/" + id + "/my-reply/submission"),
        Map.of("type", "CONDITIONAL", "content", "조건이 있습니다."),
        kim());
    send(put(BASE + "/" + id + "/my-reply"), Map.of("type", "AGREE"), choi());
    send(get(BASE + "/" + id + "/collation"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary.targetCount").value(2))
        .andExpect(jsonPath("$.summary.repliedCount").value(1))
        .andExpect(jsonPath("$.summary.replyRate").value(50))
        .andExpect(jsonPath("$.summary.byType[1].type").value("CONDITIONAL"))
        .andExpect(jsonPath("$.summary.byType[1].organizations[0]").value("가나은행"))
        .andExpect(jsonPath("$.summary.notRepliedOrganizations[0]").value("사아증권"))
        .andExpect(jsonPath("$.rows[1].replied").value(false))
        .andExpect(jsonPath("$.rows[1].replyStatus").value("DRAFT"))
        .andExpect(jsonPath("$.rows[1].type").isEmpty());
    send(get(BASE + "/" + id + "/collation"), null, kim()).andExpect(status().isForbidden());
  }

  @Test
  void 리마인드는_미회신_기관에만_가고_이력이_남는다() throws Exception {
    Long id = createOpen("리마인드 확인");
    send(post(BASE + "/" + id + "/my-reply/submission"), Map.of("type", "AGREE"), kim());
    send(post(BASE + "/" + id + "/reminders"), Map.of(), hong())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.organizationCount").value(1))
        .andExpect(jsonPath("$.organizationNames[0]").value("사아증권"));
    assertThat(
            notificationRepository.findAll().stream()
                .filter(n -> n.getType() == NotificationType.OPINION_REPLY_DUE)
                .filter(n -> n.getTitle().contains("리마인드 확인"))
                .map(n -> n.getRecipientEmail()))
        .containsExactly("choi.saa@saa.example.com");
    send(get(BASE + "/" + id + "/reminders"), null, hong())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].senderName").value("홍길동"));
    send(
            post(BASE + "/" + id + "/reminders"),
            Map.of("organizationIds", List.of(orgId("GANA"))),
            hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("OPINION_REMINDER_NO_RECIPIENT"));
    send(post(BASE + "/" + id + "/reminders"), Map.of(), kim()).andExpect(status().isForbidden());
  }

  @Test
  void 엑셀은_내부망에서_결과_요약과_취합표_두_장으로_내려받는다() throws Exception {
    Long id = createOpen("엑셀 확인");
    send(post(BASE + "/" + id + "/my-reply/submission"), Map.of("type", "AGREE"), kim());
    byte[] content =
        send(get(BASE + "/" + id + "/collation/excel"), null, hong())
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
      assertThat(workbook.getSheetName(0)).isEqualTo("결과 요약");
      assertThat(workbook.getSheet("기관별 취합표").getRow(1).getCell(1).getStringCellValue())
          .isEqualTo("가나은행");
      assertThat(workbook.getSheet("기관별 취합표").getRow(1).getCell(5).getStringCellValue())
          .isEqualTo("동의");
    }
    send(
            get(BASE + "/" + id + "/collation/excel").header("X-Network-Zone", "EXTERNAL"),
            null,
            hong())
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
  }

  @Test
  void 마감하면_회신을_더_받지_않는다() throws Exception {
    Long id = createOpen("마감 확인");
    send(post(BASE + "/" + id + "/closing"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.permissions.edit").value(false));
    send(post(BASE + "/" + id + "/my-reply/submission"), Map.of("type", "AGREE"), kim())
        .andExpect(status().isConflict());
  }

  // ---- 첨부 권한 ----

  @Test
  void 회신_양식_파일은_그_양식을_고른_기관만_내려받는다() throws Exception {
    Long id = createOpen("양식 권한");
    Long formId =
        json(send(get(BASE + "/" + id), null, hong())).get("forms").get(0).get("id").asLong();
    MockMultipartFile file =
        new MockMultipartFile("file", "양식.xlsx", "application/octet-stream", new byte[] {1, 2, 3});
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(file)
                .param("ownerType", "OPINION_REPLY_FORM")
                .param("ownerId", formId.toString())
                .with(csrf())
                .with(hong()))
        .andExpect(status().isCreated());
    send(
            get("/api/v1/attachments")
                .param("ownerType", "OPINION_REPLY_FORM")
                .param("ownerId", formId.toString()),
            null,
            kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("양식.xlsx"));
    send(
            get("/api/v1/attachments")
                .param("ownerType", "OPINION_REPLY_FORM")
                .param("ownerId", formId.toString()),
            null,
            choi())
        .andExpect(status().isForbidden());
  }

  @Test
  void 요청_첨부자료는_결제원만_올리고_대상_기관은_내려받는다() throws Exception {
    Long id = createOpen("첨부자료 권한");
    MockMultipartFile file =
        new MockMultipartFile("file", "개편안.pdf", "application/pdf", new byte[] {1});
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(file)
                .param("ownerType", "OPINION_REQUEST")
                .param("ownerId", id.toString())
                .with(csrf())
                .with(kim()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(file)
                .param("ownerType", "OPINION_REQUEST")
                .param("ownerId", id.toString())
                .with(csrf())
                .with(hong()))
        .andExpect(status().isCreated());
    send(
            get("/api/v1/attachments")
                .param("ownerType", "OPINION_REQUEST")
                .param("ownerId", id.toString()),
            null,
            kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("개편안.pdf"));
  }

  // ---- 홈·검색 ----

  @Test
  void 홈_카드와_검색에_실제_요청이_나온다() throws Exception {
    send(get("/api/v1/home").param("business", "account-info"), null, hong())
        .andExpect(
            jsonPath("$.cards[?(@.key == 'OPINION_REPLIES')].source").value(hasItem("REAL")));
    send(get("/api/v1/home").param("business", "account-info"), null, kim())
        .andExpect(
            jsonPath("$.cards[?(@.key == 'OPINION_REPLIES')].title").value(hasItem("회신할 의견 요청")));
    send(get("/api/v1/search").param("business", "account-info").param("q", "야간 배치"), null, hong())
        .andExpect(
            jsonPath("$.groups[?(@.category == 'OPINION_REQUEST')].hits[0].title")
                .value(hasItem("야간 배치 시간 조정 의견 요청")));
    send(get("/api/v1/search").param("business", "account-info").param("q", "야간 배치"), null, kim())
        .andExpect(jsonPath("$.groups[*].category").value(not(hasItem("OPINION_REQUEST"))));
  }
}
