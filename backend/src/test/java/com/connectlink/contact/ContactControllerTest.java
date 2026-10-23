package com.connectlink.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.account.UserAccountRepository;
import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가. 담당자 연락처: 업무·기관별 조회 범위, 내부망 전용(검색·필터, 결제원 쪽 수정·삭제), 기관 담당자의 수정·후임자 등록, 변경 이력(한 건, 가림), 확인과 6개월
 * 경과 알림, 기관 현행화 완료, 가입 승인·탈퇴 연결, 전역 검색. 테스트마다 되돌리므로(@Transactional) 시드 건을 그대로 쓴다.
 */
@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:contact-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContactControllerTest {

  private static final String BASE = "/api/v1/contacts";

  /** 결제원 상황실(CMS 만 맡지만 전 업무 조회). */
  private static final CurrentUser CONTROL_ROOM =
      new CurrentUser(
          "lee.control@kftc.example.com",
          "이관제",
          "KFTC",
          "금융결제원",
          OrganizationType.KFTC,
          false,
          Set.of(Business.CMS),
          JobRole.IT,
          Set.of(ItRole.CONTROL_ROOM));

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ContactRepository contactRepository;
  @Autowired private UserAccountRepository accountRepository;
  @Autowired private NotificationRepository notificationRepository;

  /** 결제원 계좌통합관리·자동이체통합관리 현업. */
  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  /** 결제원 CMS 현업(계좌통합관리 담당 아님). */
  private static RequestPostProcessor park() {
    return TestUsers.as(TestUsers.KFTC_CMS);
  }

  /** KB국민은행 계좌통합관리 담당자. */
  private static RequestPostProcessor gana() {
    return TestUsers.ganaMember();
  }

  /** 삼성증권 계좌통합관리 담당자. */
  private static RequestPostProcessor saa() {
    return TestUsers.as(TestUsers.SAA_MEMBER);
  }

  private Long contactId(String email, Business business) {
    return contactRepository.findByEmailOrderByBusinessAscIdAsc(email).stream()
        .filter(c -> c.getBusiness() == business)
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private static MockHttpServletRequestBuilder external(MockHttpServletRequestBuilder request) {
    return request.header("X-Network-Zone", "EXTERNAL");
  }

  private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body)
      throws Exception {
    return request
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body));
  }

  /** 남궁민(KB국민은행 계좌통합관리 전산) 양식. 바꿀 칸만 덮어쓴다. */
  private static Map<String, Object> namgungForm(Map<String, Object> overrides) {
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("subBusiness", "계좌정보 통합조회");
    body.put("name", "남궁민");
    body.put("department", "전산부");
    body.put("team", "채널개발팀");
    body.put("jobTitle", "과장");
    body.put("jobRole", "IT");
    body.put("itRoles", List.of("DEV"));
    body.put("rank", "PRIMARY");
    body.put("extensionPhone", "2201");
    body.put("mobilePhone", "010-0000-2201");
    body.put("email", "namgung.min@kb.example.com");
    body.putAll(overrides);
    return body;
  }

  private static Map<String, Object> successorForm(String email) {
    Map<String, Object> body = new HashMap<>();
    body.put("business", "ACCOUNT_INFO");
    body.put("name", "후임자");
    body.put("department", "디지털금융부");
    body.put("jobRole", "BUSINESS");
    body.put("rank", "SECONDARY");
    body.put("extensionPhone", "2109");
    body.put("mobilePhone", "010-0000-2109");
    body.put("email", email);
    return body;
  }

  // 조회 범위

  @Test
  void 결제원_담당자는_그_업무의_모든_기관_담당자를_본다() throws Exception {
    mockMvc
        .perform(get(BASE).param("business", "account-info").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(11))
        .andExpect(jsonPath("$.content[0].organizationType").value("KFTC"))
        .andExpect(jsonPath("$.content[*].organizationCode", hasItem("240")));
  }

  @Test
  void 상황실은_맡지_않은_업무도_전_기관을_본다() throws Exception {
    mockMvc
        .perform(get(BASE).param("business", "account-info").with(TestUsers.as(CONTROL_ROOM)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(11))
        .andExpect(jsonPath("$.content[0].permissions.edit").value(false));
  }

  @Test
  void 맡지_않은_업무는_결제원_현업도_보지_못한다() throws Exception {
    mockMvc
        .perform(get(BASE).param("business", "account-info").with(park()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void 기관_담당자는_결제원과_자기_기관_담당자만_본다() throws Exception {
    mockMvc
        .perform(get(BASE).param("business", "account-info").with(gana()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(
            jsonPath("$.content[*].organizationCode", everyItem(in(List.of("KFTC", "004")))));
    mockMvc
        .perform(
            get(BASE + "/" + contactId("choi.yujin@samsung.example.com", Business.ACCOUNT_INFO))
                .with(gana()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get(BASE + "/organizations").param("business", "account-info").with(gana()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].code", everyItem(in(List.of("KFTC", "004")))));
  }

  @Test
  void 검색과_필터는_내부망에서만_한다() throws Exception {
    mockMvc
        .perform(external(get(BASE).param("business", "account-info").param("q", "김").with(hong())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
    mockMvc
        .perform(external(get(BASE).param("business", "account-info").with(gana())))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get(BASE).param("business", "account-info").param("needsConfirm", "true").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(
            jsonPath("$.content[*].needsConfirm", everyItem(org.hamcrest.Matchers.is(true))));
    mockMvc
        .perform(get(BASE + "/counts").param("business", "account-info").with(hong()))
        .andExpect(jsonPath("$.total").value(11))
        .andExpect(jsonPath("$.needsConfirm").value(3))
        .andExpect(jsonPath("$.unreachable").value(0));
  }

  @Test
  void 엑셀은_내부망에서만_내려받는다() throws Exception {
    mockMvc
        .perform(get(BASE + "/excel").param("business", "account-info").with(hong()))
        .andExpect(status().isOk());
    mockMvc
        .perform(external(get(BASE + "/excel").param("business", "account-info").with(hong())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
  }

  // 수정·삭제

  @Test
  void 기관_담당자는_인터넷망에서도_같은_기관_담당자를_고치고_최근_확인일이_바뀐다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(
            external(
                json(put(BASE + "/" + id), namgungForm(Map.of("extensionPhone", "2202")))
                    .with(gana())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.extensionPhone").value("2202"))
        .andExpect(jsonPath("$.lastConfirmedByName").value("김민준"))
        .andExpect(jsonPath("$.needsConfirm").value(false));
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "CONTACT")
                .param("targetId", String.valueOf(id))
                .with(gana()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].changes[0].field").value("extensionPhone"))
        .andExpect(jsonPath("$.content[0].changes[0].before").value("2201"));
  }

  @Test
  void 다른_기관_담당자는_고치지_못한다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(json(put(BASE + "/" + id), namgungForm(Map.of())).with(saa()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void 결제원_담당자의_수정은_내부망에서만_한다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(
            external(json(put(BASE + "/" + id), namgungForm(Map.of("team", "인프라팀"))).with(hong())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
    mockMvc
        .perform(json(put(BASE + "/" + id), namgungForm(Map.of("team", "인프라팀"))).with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.team").value("인프라팀"))
        .andExpect(jsonPath("$.lastConfirmedByName").value("홍성민"));
  }

  @Test
  void 지우기는_결제원_담당자가_내부망에서만_한다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(delete(BASE + "/" + id).with(csrf()).with(gana()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    mockMvc
        .perform(external(delete(BASE + "/" + id).with(csrf()).with(hong())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
    mockMvc
        .perform(delete(BASE + "/" + id).with(csrf()).with(hong()))
        .andExpect(status().isNoContent());
    assertThat(contactRepository.findById(id)).isEmpty();
  }

  @Test
  void 목록의_권한_표시는_망과_역할을_따른다() throws Exception {
    mockMvc
        .perform(external(get(BASE).param("business", "account-info").with(gana())))
        .andExpect(
            jsonPath(
                "$.content[?(@.organizationCode == '004')].permissions.edit",
                everyItem(org.hamcrest.Matchers.is(true))))
        .andExpect(
            jsonPath(
                "$.content[?(@.organizationCode == 'KFTC')].permissions.edit",
                everyItem(org.hamcrest.Matchers.is(false))))
        .andExpect(
            jsonPath(
                "$.content[*].permissions.delete", everyItem(org.hamcrest.Matchers.is(false))));
    mockMvc
        .perform(get(BASE).param("business", "account-info").with(hong()))
        .andExpect(
            jsonPath("$.content[*].permissions.delete", everyItem(org.hamcrest.Matchers.is(true))));
  }

  // 변경 이력

  @Test
  void 이력은_이전_담당자_한_명까지만_보이고_휴대폰은_가린다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "CONTACT")
                .param("targetId", String.valueOf(id))
                .with(gana()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].action").value("UPDATED"))
        .andExpect(jsonPath("$.content[?(@.action == 'CREATED')]").isEmpty())
        .andExpect(
            jsonPath(
                "$.content[0].changes[?(@.field == 'extensionPhone')].before", hasItem("2299")))
        .andExpect(
            jsonPath(
                "$.content[0].changes[?(@.field == 'mobilePhone')].before",
                hasItem("010-****-2299")))
        .andExpect(
            jsonPath(
                "$.content[0].changes[?(@.field == 'mobilePhone')].after",
                hasItem("010-0000-2201")));
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "CONTACT")
                .param("targetId", String.valueOf(id))
                .with(saa()))
        .andExpect(status().isForbidden());
  }

  // 확인과 6개월 경과

  @Test
  void 변동_없음_확인은_최근_확인일만_바꾸고_이력은_남기지_않는다() throws Exception {
    Long id = contactId("kim.minjun@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(get(BASE + "/reconfirm-due").with(gana()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].name", hasItem("김민준")));
    mockMvc
        .perform(external(post(BASE + "/" + id + "/confirmation").with(csrf()).with(gana())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.needsConfirm").value(false))
        .andExpect(jsonPath("$.self").value(true));
    mockMvc
        .perform(get(BASE + "/reconfirm-due").with(gana()))
        .andExpect(jsonPath("$[*].name", not(hasItem("김민준"))));
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "CONTACT")
                .param("targetId", String.valueOf(id))
                .with(gana()))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void 다른_기관_담당자는_확인하지_못한다() throws Exception {
    Long id = contactId("kim.minjun@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(post(BASE + "/" + id + "/confirmation").with(csrf()).with(saa()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 내_담당자_정보는_업무별로_본다() throws Exception {
    mockMvc
        .perform(get(BASE + "/me").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].self", everyItem(org.hamcrest.Matchers.is(true))));
  }

  @Test
  void 경과_알림은_계정이_있는_담당자에게_한_번만_간다() throws Exception {
    mockMvc
        .perform(post("/api/v1/demo/contact-reconfirm-runs").with(csrf()).with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notified").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    assertThat(
            notificationRepository.findByRecipientEmailAndReadAtIsNull("kim.minjun@kb.example.com"))
        .anyMatch(n -> n.getType() == NotificationType.CONTACT_RECONFIRM_DUE);
    mockMvc
        .perform(post("/api/v1/demo/contact-reconfirm-runs").with(csrf()).with(TestUsers.admin()))
        .andExpect(jsonPath("$.notified").value(0));
  }

  // 연락 불가

  @Test
  void 상황실이_연락_불가로_표시하면_결제원_담당자에게_알리고_목록에_빨간_비고가_보인다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(
            json(
                    put(BASE + "/" + id + "/unreachable"),
                    Map.of("note", "02:10 내선·휴대폰 모두 받지 않음", "notifyOrganization", false))
                .with(TestUsers.as(CONTROL_ROOM)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreachable.note").value("02:10 내선·휴대폰 모두 받지 않음"))
        .andExpect(jsonPath("$.unreachable.markedByName").value("이관제"))
        .andExpect(jsonPath("$.permissions.clearUnreachable").value(true));
    assertThat(
            notificationRepository.findByRecipientEmailAndReadAtIsNull(
                "hong.sungmin@kftc.example.com"))
        .anyMatch(n -> n.getType() == NotificationType.CONTACT_UNREACHABLE);
    assertThat(
            notificationRepository.findByRecipientEmailAndReadAtIsNull("kim.minjun@kb.example.com"))
        .noneMatch(n -> n.getType() == NotificationType.CONTACT_UNREACHABLE);
    mockMvc
        .perform(
            get(BASE).param("business", "account-info").param("unreachable", "true").with(hong()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].name").value("남궁민"));
    mockMvc
        .perform(get(BASE + "/counts").param("business", "account-info").with(hong()))
        .andExpect(jsonPath("$.unreachable").value(1));
    mockMvc
        .perform(
            get("/api/v1/change-histories")
                .param("targetType", "CONTACT")
                .param("targetId", String.valueOf(id))
                .with(hong()))
        .andExpect(jsonPath("$.content[0].changes[0].field").value("unreachable"));
  }

  @Test
  void 기관에도_알리기를_고르면_그_기관_같은_업무_담당자에게_메일이_간다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(
            json(put(BASE + "/" + id + "/unreachable"), Map.of("notifyOrganization", true))
                .with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreachable.note").doesNotExist());
    assertThat(
            notificationRepository.findByRecipientEmailAndReadAtIsNull("kim.minjun@kb.example.com"))
        .anyMatch(n -> n.getType() == NotificationType.CONTACT_UNREACHABLE);
  }

  @Test
  void 기관_담당자는_연락_불가로_표시하지_못하고_결제원도_인터넷망에서는_못한다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(json(put(BASE + "/" + id + "/unreachable"), Map.of()).with(gana()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    mockMvc
        .perform(external(json(put(BASE + "/" + id + "/unreachable"), Map.of()).with(hong())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
    mockMvc
        .perform(external(get(BASE + "/" + id).with(hong())))
        .andExpect(jsonPath("$.permissions.markUnreachable").value(false));
  }

  @Test
  void 기관_담당자가_정보를_고치면_연락_불가_표시가_지워진다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(json(put(BASE + "/" + id + "/unreachable"), Map.of()).with(hong()))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            external(
                json(put(BASE + "/" + id), namgungForm(Map.of("mobilePhone", "010-0000-2299")))
                    .with(gana())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreachable").doesNotExist());
    assertThat(contactRepository.findById(id).orElseThrow().isUnreachable()).isFalse();
  }

  @Test
  void 연락_불가_해제는_고칠_수_있는_기관_담당자가_하고_다른_기관은_못한다() throws Exception {
    Long id = contactId("namgung.min@kb.example.com", Business.ACCOUNT_INFO);
    mockMvc
        .perform(json(put(BASE + "/" + id + "/unreachable"), Map.of()).with(hong()))
        .andExpect(status().isOk());
    mockMvc
        .perform(delete(BASE + "/" + id + "/unreachable").with(csrf()).with(saa()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(external(delete(BASE + "/" + id + "/unreachable").with(csrf()).with(gana())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreachable").doesNotExist())
        .andExpect(jsonPath("$.permissions.clearUnreachable").value(false));
  }

  // 등록

  @Test
  void 기관_담당자는_자기_기관에만_후임자를_등록하고_이메일이_겹치면_409다() throws Exception {
    Map<String, Object> body = successorForm("successor@kb.example.com");
    body.put("organizationId", 999999L);
    mockMvc
        .perform(external(json(post(BASE), body).with(gana())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.organizationCode").value("004"))
        .andExpect(jsonPath("$.lastConfirmedByName").value("김민준"));
    mockMvc
        .perform(json(post(BASE), body).with(gana()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONTACT_DUPLICATE"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
  }

  @Test
  void 맡지_않은_업무에는_등록하지_못한다() throws Exception {
    mockMvc
        .perform(json(post(BASE), successorForm("someone@kb.example.com")).with(park()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 결제원_담당자의_등록은_내부망에서만_한다() throws Exception {
    Map<String, Object> body = successorForm("new@kb.example.com");
    body.put(
        "organizationId",
        contactRepository
            .findById(contactId("kim.minjun@kb.example.com", Business.ACCOUNT_INFO))
            .orElseThrow()
            .getOrganization()
            .getId());
    mockMvc
        .perform(external(json(post(BASE), body).with(hong())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
    mockMvc
        .perform(json(post(BASE), body).with(hong()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.organizationCode").value("004"));
  }

  @Test
  void 검증에_걸리면_필드별_메시지를_준다() throws Exception {
    Map<String, Object> body = successorForm("not-an-email");
    body.put("name", "");
    mockMvc
        .perform(json(post(BASE), body).with(gana()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
  }

  // 정기 변경 주간의 기관 현행화

  @Test
  void 기관_담당자가_현행화_완료를_누르면_진행_중인_주간에_완료로_남는다() throws Exception {
    mockMvc
        .perform(
            external(
                json(post(BASE + "/organization-confirmations"), Map.of("business", "ACCOUNT_INFO"))
                    .with(gana())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.confirmedCount").value(2))
        .andExpect(jsonPath("$.campaignTitle").value("하반기 담당자 정기 변경"));
    mockMvc
        .perform(
            get("/api/v1/update-campaigns/current").param("business", "account-info").with(gana()))
        .andExpect(jsonPath("$.ownOrganization.completed").value(true))
        .andExpect(jsonPath("$.ownOrganization.completedByName").value("김민준"));
  }

  @Test
  void 다른_기관의_현행화_완료는_누르지_못한다() throws Exception {
    Long otherOrg =
        contactRepository
            .findById(contactId("choi.yujin@samsung.example.com", Business.ACCOUNT_INFO))
            .orElseThrow()
            .getOrganization()
            .getId();
    mockMvc
        .perform(
            json(
                    post(BASE + "/organization-confirmations"),
                    Map.of("business", "ACCOUNT_INFO", "organizationId", otherOrg))
                .with(gana()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.confirmedCount").value(2));
    assertThat(
            contactRepository
                .findById(contactId("choi.yujin@samsung.example.com", Business.ACCOUNT_INFO))
                .orElseThrow()
                .getLastConfirmedByName())
        .isEqualTo("최유진");
  }

  // 계정 연결

  @Test
  void 가입을_승인하면_그_업무의_담당자로_등록된다() throws Exception {
    Long accountId =
        accountRepository.findByEmail("han.yerin@kb.example.com").orElseThrow().getId();
    mockMvc
        .perform(json(post("/api/v1/sign-ups/" + accountId + "/approval"), Map.of()).with(hong()))
        .andExpect(status().isOk());
    assertThat(contactRepository.findByEmailOrderByBusinessAscIdAsc("han.yerin@kb.example.com"))
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.getBusiness()).isEqualTo(Business.ACCOUNT_INFO);
              assertThat(c.getOrganization().getCode()).isEqualTo("004");
              assertThat(c.getRank()).isEqualTo(ContactRank.SECONDARY);
            });
  }

  @Test
  void 기관의_유일한_담당자는_후임자를_등록하기_전에_탈퇴하지_못한다() throws Exception {
    mockMvc
        .perform(get("/api/v1/me/withdrawal").with(saa()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(false))
        .andExpect(jsonPath("$.blockers[*].code", hasItem("CONTACT_SUCCESSOR_REQUIRED")));
    mockMvc
        .perform(get("/api/v1/me/withdrawal").with(gana()))
        .andExpect(jsonPath("$.blockers[*].code", not(hasItem("CONTACT_SUCCESSOR_REQUIRED"))));
  }

  // 전역 검색

  @Test
  void 전역_검색의_담당자_묶음은_내부망에서만_채운다() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/search").param("business", "account-info").param("q", "남궁").with(hong()))
        .andExpect(
            jsonPath(
                "$.groups[?(@.category == 'CONTACT')].hits[*].title", hasItem("남궁민 · KB국민은행")));
    mockMvc
        .perform(
            external(
                get("/api/v1/search")
                    .param("business", "account-info")
                    .param("q", "남궁")
                    .with(hong())))
        .andExpect(jsonPath("$.groups[?(@.category == 'CONTACT')].hits[*]").isEmpty());
  }
}
