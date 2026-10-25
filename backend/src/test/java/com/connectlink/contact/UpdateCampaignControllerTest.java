package com.connectlink.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
 * 정기 변경(현행화) 주간: 등록·수정·삭제와 권한, 기간 겹침, 진행률과 완료·미확인·정보 변경 기관 거르기, 기관 담당자의 진행 중 주간 안내, 미회신 기관 리마인드와
 * 6개월 주기 예약 작업. 테스트마다 되돌린다.
 */
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:update-campaign-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UpdateCampaignControllerTest {

  private static final String BASE = "/api/v1/update-campaigns";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UpdateCampaignRepository campaignRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private UpdateCampaignReminderRepository reminderRepository;

  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  private static RequestPostProcessor moon() {
    return TestUsers.as(TestUsers.KFTC_AUTO_TRANSFER);
  }

  private static RequestPostProcessor park() {
    return TestUsers.as(TestUsers.KFTC_CMS);
  }

  private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body)
      throws Exception {
    return request
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body));
  }

  private Long campaignId(String title) {
    return campaignRepository.findAll().stream()
        .filter(c -> c.getTitle().equals(title))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private static Map<String, Object> form(String business, LocalDate start, LocalDate end) {
    return Map.of("business", business, "title", "임시 정기 변경", "startDate", start, "endDate", end);
  }

  @Test
  void 결제원_담당자는_그_업무의_주간을_최근순으로_본다() throws Exception {
    mockMvc
        .perform(get(BASE).param("business", "account-info").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].title").value("하반기 담당자 정기 변경"))
        .andExpect(jsonPath("$[0].status").value("OPEN"))
        .andExpect(jsonPath("$[0].completedCount").value(2))
        .andExpect(jsonPath("$[0].changedCount").value(2))
        .andExpect(jsonPath("$[1].status").value("CLOSED"));
  }

  @Test
  void 기관_담당자와_다른_업무_담당자는_주간_목록을_보지_못한다() throws Exception {
    mockMvc
        .perform(get(BASE).param("business", "account-info").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get(BASE).param("business", "account-info").with(park()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 진행률은_완료_기관_수를_대상_기관_수로_나눈다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    mockMvc
        .perform(get(BASE + "/" + id + "/progress").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.campaign.targetCount").value(5))
        .andExpect(jsonPath("$.campaign.progressRate").value(40))
        .andExpect(jsonPath("$.organizations.length()").value(5))
        .andExpect(jsonPath("$.organizations[0].completed").value(false));
    mockMvc
        .perform(get(BASE + "/" + id + "/progress").param("filter", "COMPLETED").with(hong()))
        .andExpect(
            jsonPath("$.organizations[*].organizationCode", containsInAnyOrder("088", "240")));
    mockMvc
        .perform(get(BASE + "/" + id + "/progress").param("filter", "INCOMPLETE").with(hong()))
        .andExpect(jsonPath("$.organizations.length()").value(3))
        .andExpect(jsonPath("$.organizations[*].organizationCode", not(hasItem("088"))));
    mockMvc
        .perform(get(BASE + "/" + id + "/progress").param("filter", "CHANGED").with(hong()))
        .andExpect(
            jsonPath("$.organizations[*].organizationCode", containsInAnyOrder("088", "048")));
    mockMvc
        .perform(get(BASE + "/" + id + "/progress").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 주간을_등록하면_그_업무_참가기관_담당자에게_알린다() throws Exception {
    LocalDate today = LocalDate.now();
    mockMvc
        .perform(
            json(post(BASE), form("AUTO_TRANSFER", today.plusDays(1), today.plusDays(7)))
                .with(moon()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("UPCOMING"))
        .andExpect(jsonPath("$.registrantName").value("문서준"));
    assertThat(
            notificationRepository.findByRecipientEmailAndReadAtIsNull(
                "lee.junho@shinhan.example.com"))
        .anyMatch(
            n ->
                n.getType() == NotificationType.CONTACT_CHANGE_REQUESTED
                    && n.getBusiness() == Business.AUTO_TRANSFER);
  }

  @Test
  void 기관_담당자는_주간을_등록하지_못한다() throws Exception {
    LocalDate today = LocalDate.now();
    mockMvc
        .perform(
            json(post(BASE), form("ACCOUNT_INFO", today.plusDays(30), today.plusDays(37)))
                .with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 같은_업무의_주간은_기간이_겹치지_않는다() throws Exception {
    LocalDate today = LocalDate.now();
    mockMvc
        .perform(json(post(BASE), form("ACCOUNT_INFO", today, today.plusDays(2))).with(hong()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("UPDATE_CAMPAIGN_OVERLAP"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"));
  }

  @Test
  void 종료일이_시작일보다_앞서면_400이다() throws Exception {
    LocalDate today = LocalDate.now();
    mockMvc
        .perform(
            json(post(BASE), form("AUTO_TRANSFER", today.plusDays(9), today.plusDays(3)))
                .with(moon()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 예정_주간만_지우고_시작한_주간은_지우지_못한다() throws Exception {
    mockMvc
        .perform(delete(BASE + "/" + campaignId("하반기 담당자 정기 변경")).with(csrf()).with(hong()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("UPDATE_CAMPAIGN_STARTED"));
    Long upcoming = campaignId("연말 담당자 정기 변경");
    mockMvc
        .perform(delete(BASE + "/" + upcoming).with(csrf()).with(hong()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(delete(BASE + "/" + upcoming).with(csrf()).with(park()))
        .andExpect(status().isNoContent());
    assertThat(campaignRepository.findById(upcoming)).isEmpty();
  }

  @Test
  void 끝난_주간은_고치지_못한다() throws Exception {
    LocalDate today = LocalDate.now();
    mockMvc
        .perform(
            json(
                    put(BASE + "/" + campaignId("상반기 담당자 정기 변경")),
                    form("ACCOUNT_INFO", today.minusDays(185), today.minusDays(170)))
                .with(hong()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("UPDATE_CAMPAIGN_CLOSED"));
  }

  @Test
  void 기관_담당자는_진행_중인_주간과_자기_기관_현행화를_본다() throws Exception {
    mockMvc
        .perform(
            get(BASE + "/current").param("business", "account-info").with(TestUsers.ganaMember()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.campaign.title").value("하반기 담당자 정기 변경"))
        .andExpect(jsonPath("$.ownOrganization.organizationCode").value("004"))
        .andExpect(jsonPath("$.ownOrganization.completed").value(false));
    mockMvc
        .perform(get(BASE + "/current").param("business", "cms").with(park()))
        .andExpect(jsonPath("$.campaign").doesNotExist());
  }

  private long remindersTo(String email) {
    return notificationRepository.findByRecipientEmailAndReadAtIsNull(email).stream()
        .filter(n -> n.getType() == NotificationType.CONTACT_CHANGE_REMINDER)
        .count();
  }

  private long remindersOf(Long campaignId, String organizationCode) {
    return reminderRepository.findByCampaignIdOrderBySentAtAscIdAsc(campaignId).stream()
        .filter(r -> r.getOrganization().getCode().equals(organizationCode))
        .count();
  }

  @Test
  void 결제원_담당자는_미회신_기관에만_리마인드를_보낸다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    mockMvc
        .perform(post(BASE + "/" + id + "/reminders").with(csrf()).with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationCount").value(3))
        .andExpect(jsonPath("$.skippedOrganizationCount").value(0));
    // 미완료 KB국민은행 담당자는 받고, 현행화 완료한 신한은행 담당자는 받지 않는다.
    assertThat(remindersTo("kim.minjun@kb.example.com")).isEqualTo(1);
    assertThat(remindersTo("lee.junho@shinhan.example.com")).isZero();
    mockMvc
        .perform(get(BASE + "/" + id + "/progress").param("filter", "INCOMPLETE").with(hong()))
        .andExpect(
            jsonPath("$.organizations[?(@.organizationCode == '004')].reminderCount").value(2));
  }

  @Test
  void 고른_기관에만_리마인드를_보낸다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    Long kb =
        reminderRepository
            .findByCampaignIdOrderBySentAtAscIdAsc(id)
            .get(0)
            .getOrganization()
            .getId();
    mockMvc
        .perform(
            json(post(BASE + "/" + id + "/reminders"), Map.of("organizationIds", List.of(kb)))
                .with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationCount").value(1));
    assertThat(remindersOf(id, "004")).isEqualTo(2);
    assertThat(remindersOf(id, "048")).isZero();
  }

  @Test
  void 기관_담당자와_다른_업무_담당자는_리마인드를_보내지_못한다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    mockMvc
        .perform(post(BASE + "/" + id + "/reminders").with(csrf()).with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(post(BASE + "/" + id + "/reminders").with(csrf()).with(park()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 미리보기는_기본_문구와_받을_기관을_보여_준다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    mockMvc
        .perform(get(BASE + "/" + id + "/reminder-preview").with(hong()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.subject")
                .value(org.hamcrest.Matchers.startsWith("[금융결제원] (계좌통합관리) 정기 변경 리마인드: ")))
        .andExpect(jsonPath("$.body").value(org.hamcrest.Matchers.containsString("현행화 완료")))
        .andExpect(
            jsonPath("$.body")
                .value(
                    org.hamcrest.Matchers.endsWith(
                        "가지 않습니다.\nhttp://localhost:8080/account-info/contacts")))
        .andExpect(jsonPath("$.bodyMaxLength").value(1000))
        .andExpect(jsonPath("$.organizations.length()").value(3))
        .andExpect(jsonPath("$.organizations[?(@.organizationCode == '088')]").isEmpty());
    // 미리보기는 보내지 않는다.
    assertThat(remindersTo("kim.minjun@kb.example.com")).isZero();
  }

  @Test
  void 기관_담당자와_다른_업무_담당자는_미리보기를_보지_못한다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    mockMvc
        .perform(get(BASE + "/" + id + "/reminder-preview").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get(BASE + "/" + id + "/reminder-preview").with(park()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 고친_제목과_본문으로_리마인드를_보낸다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    mockMvc
        .perform(
            json(
                    post(BASE + "/" + id + "/reminders"),
                    Map.of("subject", "  고친 제목  ", "body", "고친 본문입니다."))
                .with(hong()))
        .andExpect(status().isOk());
    assertThat(
            notificationRepository
                .findByRecipientEmailAndReadAtIsNull("kim.minjun@kb.example.com")
                .stream()
                .filter(n -> n.getType() == NotificationType.CONTACT_CHANGE_REMINDER)
                .map(n -> n.getTitle() + "|" + n.getBody()))
        .containsExactly("고친 제목|고친 본문입니다.");
  }

  @Test
  void 빈_제목이나_너무_긴_본문은_400이다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    mockMvc
        .perform(
            json(
                    post(BASE + "/" + id + "/reminders"),
                    Map.of("subject", "  ", "body", "x".repeat(1001)))
                .with(hong()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    assertThat(remindersTo("kim.minjun@kb.example.com")).isZero();
  }

  @Test
  void 진행_중이_아닌_주간에는_리마인드를_보내지_못한다() throws Exception {
    Long id = campaignId("상반기 담당자 정기 변경");
    mockMvc
        .perform(post(BASE + "/" + id + "/reminders").with(csrf()).with(hong()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("UPDATE_CAMPAIGN_NOT_OPEN"));
  }

  @Test
  void 예약_작업은_간격이_지난_미회신_기관에만_리마인드를_보낸다() throws Exception {
    Long id = campaignId("하반기 담당자 정기 변경");
    // 주간은 사흘 전 시작했다(간격 3일). KB국민은행은 어제 리마인드를 받아 이번에는 건너뛴다.
    mockMvc
        .perform(post("/api/v1/demo/update-campaign-runs").with(csrf()).with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remindedOrganizationCount").value(2));
    assertThat(remindersOf(id, "004")).isEqualTo(1);
    assertThat(remindersOf(id, "048")).isEqualTo(1);
    assertThat(remindersOf(id, "088")).isZero();
    // 같은 날 다시 돌려도 간격이 지나지 않아 보내지 않는다.
    mockMvc
        .perform(post("/api/v1/demo/update-campaign-runs").with(csrf()).with(hong()))
        .andExpect(jsonPath("$.remindedOrganizationCount").value(0));
  }

  @Test
  void 주기가_지난_업무는_예약_작업이_주간을_등록하고_변경_요청_메일을_보낸다() throws Exception {
    LocalDate today = LocalDate.now();
    campaignRepository.saveAndFlush(
        new UpdateCampaign(
            Business.LOAN_TRANSFER,
            "지난 정기 변경",
            today.minusMonths(6).minusDays(1),
            today.minusMonths(6).plusDays(6),
            "홍성민"));
    mockMvc
        .perform(post("/api/v1/demo/update-campaign-runs").with(csrf()).with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createdCampaignCount").value(1));
    UpdateCampaign created =
        campaignRepository.findByBusinessOrderByStartDateDescIdDesc(Business.LOAN_TRANSFER).get(0);
    assertThat(created.isAutoCreated()).isTrue();
    assertThat(created.getStartDate()).isEqualTo(today);
    assertThat(created.getEndDate()).isEqualTo(today.plusDays(13));
    assertThat(created.getRegistrantName()).isEqualTo(UpdateCampaignService.AUTO_REGISTRANT);
    assertThat(notificationRepository.findAll())
        .anyMatch(
            n ->
                n.getType() == NotificationType.CONTACT_CHANGE_REQUESTED
                    && n.getBusiness() == Business.LOAN_TRANSFER);
    // 진행 중 주간이 생겼으니 다시 돌려도 더 만들지 않는다. 주간이 하나도 없는 업무(자동이체)도 만들지 않는다.
    mockMvc
        .perform(post("/api/v1/demo/update-campaign-runs").with(csrf()).with(hong()))
        .andExpect(jsonPath("$.createdCampaignCount").value(0));
  }

  @Test
  void 결제원_담당자는_다음_정기_변경_요청일을_본다() throws Exception {
    LocalDate start = LocalDate.now().minusDays(3);
    mockMvc
        .perform(get(BASE + "/schedule").param("business", "account-info").with(hong()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycleMonths").value(6))
        .andExpect(jsonPath("$.reminderIntervalDays").value(3))
        .andExpect(jsonPath("$.nextCycleDate").value(start.plusMonths(6).toString()));
    mockMvc
        .perform(
            get(BASE + "/schedule").param("business", "account-info").with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
  }
}
