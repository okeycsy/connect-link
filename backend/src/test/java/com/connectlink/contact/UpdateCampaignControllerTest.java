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

/** 정기 변경(현행화) 주간: 등록·수정·삭제와 권한, 기간 겹침, 진행률과 완료·미확인·정보 변경 기관 거르기, 기관 담당자의 진행 중 주간 안내. 테스트마다 되돌린다. */
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

  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
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
        .andExpect(jsonPath("$.campaign.targetCount").value(6))
        .andExpect(jsonPath("$.campaign.progressRate").value(33))
        .andExpect(jsonPath("$.organizations.length()").value(6))
        .andExpect(jsonPath("$.organizations[0].completed").value(false));
    mockMvc
        .perform(get(BASE + "/" + id + "/progress").param("filter", "COMPLETED").with(hong()))
        .andExpect(
            jsonPath("$.organizations[*].organizationCode", containsInAnyOrder("088", "240")));
    mockMvc
        .perform(get(BASE + "/" + id + "/progress").param("filter", "INCOMPLETE").with(hong()))
        .andExpect(jsonPath("$.organizations.length()").value(4))
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
                .with(hong()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("UPCOMING"))
        .andExpect(jsonPath("$.registrantName").value("홍길동"));
    assertThat(
            notificationRepository.findByRecipientEmailAndReadAtIsNull(
                "lee.dara@shinhan.example.com"))
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
                .with(hong()))
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
}
