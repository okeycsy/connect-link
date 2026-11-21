package com.connectlink.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.notice.NoticeRepository;
import com.connectlink.notification.NotificationRepository;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
import org.springframework.transaction.annotation.Transactional;

/** 파일 반출 결재: 결제원 담당자가 등록하며 붙인 파일은 같은 팀 팀장이 한 번에 승인해야 참가기관에 보인다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttachmentApprovalControllerTest {

  private static final String APPROVAL = "/api/v1/attachment-approvals/NOTICE/{ownerId}";
  private static final byte[] CONTENT = "가상 자료".getBytes(StandardCharsets.UTF_8);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NoticeRepository noticeRepository;

  /** 계좌통합관리 공지(시드) 하나. 홍성민이 올리고 KB국민은행 김민준이 본다. */
  private long noticeId() {
    return noticeRepository.findAll().stream()
        .filter(n -> n.getTitle().equals("본인계좌 일괄지급정지 전문 개선 일정 안내"))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private ResultActions upload(CurrentUser user, long noticeId, String name) throws Exception {
    return mockMvc.perform(
        multipart("/api/v1/attachments")
            .file(new MockMultipartFile("file", name, "application/pdf", CONTENT))
            .param("ownerType", "NOTICE")
            .param("ownerId", String.valueOf(noticeId))
            .with(TestUsers.as(user))
            .with(csrf()));
  }

  private long id(ResultActions result) throws Exception {
    return objectMapper
        .readTree(result.andReturn().getResponse().getContentAsString())
        .get("id")
        .asLong();
  }

  private ResultActions listAsGana(long noticeId) throws Exception {
    return mockMvc.perform(
        get("/api/v1/attachments")
            .param("ownerType", "NOTICE")
            .param("ownerId", String.valueOf(noticeId))
            .with(TestUsers.ganaMember()));
  }

  private long requestNotifications() {
    return notificationRepository.findAll().stream()
        .filter(
            n ->
                n.getRecipientEmail().equals("kang.jiwon@kftc.example.com")
                    && n.getType() == NotificationType.ATTACHMENT_APPROVAL_REQUESTED)
        .count();
  }

  @Test
  void 등록하며_붙인_파일은_팀장이_알림에서_한_번에_승인해야_참가기관에_보인다() throws Exception {
    long noticeId = noticeId();
    int before =
        objectMapper
            .readTree(listAsGana(noticeId).andReturn().getResponse().getContentAsString())
            .size();
    long notificationsBefore = requestNotifications();
    long first =
        id(
            upload(TestUsers.KFTC_ACCOUNT_INFO, noticeId, "안내문.pdf")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.approverName").value("강지원"))
                .andExpect(jsonPath("$.uploaderName").value("홍성민")));
    long second = id(upload(TestUsers.KFTC_ACCOUNT_INFO, noticeId, "참석명단.pdf"));

    // 알림은 등록 한 건에 한 번, 누르면 알림 센터에서 결재 창이 열린다.
    assertThat(requestNotifications()).isEqualTo(notificationsBefore + 1);
    assertThat(
            notificationRepository.findAll().stream()
                .anyMatch(
                    n ->
                        n.getRecipientEmail().equals("kang.jiwon@kftc.example.com")
                            && n.getTitle().contains("홍성민")
                            && ("/account-info/notifications?approvalOwnerType=NOTICE&approvalOwnerId="
                                    + noticeId)
                                .equals(n.getLink())))
        .isTrue();

    // 결재 전: 참가기관 목록에 없고 내려받지 못한다. 결제원 계정에는 보인다.
    listAsGana(noticeId).andExpect(jsonPath("$", hasSize(before)));
    mockMvc
        .perform(get("/api/v1/attachments/{id}/download", first).with(TestUsers.ganaMember()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/attachments")
                .param("ownerType", "NOTICE")
                .param("ownerId", String.valueOf(noticeId))
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO)))
        .andExpect(jsonPath("$[?(@.id == " + first + ")].approvalStatus").value("PENDING"));

    // 결재 창: 그 공지에 붙은 대기 파일 둘.
    mockMvc
        .perform(get(APPROVAL, noticeId).with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO_LEADER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerPath").value("notices?id=" + noticeId))
        .andExpect(jsonPath("$.ownerBusiness").value("account-info"))
        .andExpect(jsonPath("$.files[?(@.status == 'PENDING')]", hasSize(2)))
        .andExpect(jsonPath("$.files[0].uploaderTeam").value("계좌서비스부 계좌통합팀"));

    // 팀장은 결재하려고 파일을 열어 본다.
    mockMvc
        .perform(
            get("/api/v1/attachments/{id}/download", second)
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO_LEADER)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post(APPROVAL + "/approval", noticeId)
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO_LEADER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.files[?(@.status == 'PENDING')]").isEmpty())
        .andExpect(jsonPath("$.files[0].decidedAt").isNotEmpty());

    listAsGana(noticeId).andExpect(jsonPath("$", hasSize(before + 2)));
    mockMvc
        .perform(get("/api/v1/attachments/{id}/download", first).with(TestUsers.ganaMember()))
        .andExpect(status().isOk());
    assertThat(
            notificationRepository.findAll().stream()
                .filter(
                    n ->
                        n.getRecipientEmail().equals("hong.sungmin@kftc.example.com")
                            && n.getType() == NotificationType.ATTACHMENT_APPROVAL_DECIDED)
                .count())
        .isEqualTo(1);

    // 이미 결재한 건은 다시 결재하지 못한다.
    mockMvc
        .perform(
            post(APPROVAL + "/approval", noticeId)
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO_LEADER))
                .with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ATTACHMENT_ALREADY_DECIDED"));
  }

  @Test
  void 반려하면_사유가_남고_참가기관에는_계속_안_보인다() throws Exception {
    long noticeId = noticeId();
    long id = id(upload(TestUsers.KFTC_ACCOUNT_INFO, noticeId, "초안.pdf"));
    // 사유 없이는 반려하지 못한다.
    mockMvc
        .perform(
            post(APPROVAL + "/rejection", noticeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", " ")))
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO_LEADER))
                .with(csrf()))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post(APPROVAL + "/rejection", noticeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "최종본으로 올려 주세요")))
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO_LEADER))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.files[?(@.id == " + id + ")].status").value("REJECTED"))
        .andExpect(
            jsonPath("$.files[?(@.id == " + id + ")].rejectionReason").value("최종본으로 올려 주세요"));
    mockMvc
        .perform(get("/api/v1/attachments/{id}/download", id).with(TestUsers.ganaMember()))
        .andExpect(status().isNotFound());
    assertThat(
            notificationRepository.findAll().stream()
                .anyMatch(
                    n ->
                        n.getRecipientEmail().equals("hong.sungmin@kftc.example.com")
                            && n.getBody().contains("최종본으로 올려 주세요")))
        .isTrue();
  }

  @Test
  void 결재자가_아니면_결재하지_못하고_인터넷망에서도_결재하지_못한다() throws Exception {
    long noticeId = noticeId();
    upload(TestUsers.KFTC_ACCOUNT_INFO, noticeId, "안내문.pdf");
    // 거부: 결재자가 아닌 결제원 계정(올린 사람 본인 포함)은 결재할 파일이 없다.
    mockMvc
        .perform(
            post(APPROVAL + "/approval", noticeId)
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO))
                .with(csrf()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(post(APPROVAL + "/approval", noticeId).with(TestUsers.admin()).with(csrf()))
        .andExpect(status().isNotFound());
    // 거부: 참가기관 계정은 결재하지 않는다.
    mockMvc
        .perform(get(APPROVAL, noticeId).with(TestUsers.ganaMember()))
        .andExpect(status().isForbidden());
    // 거부: 결재자라도 인터넷망
    mockMvc
        .perform(
            post(APPROVAL + "/approval", noticeId)
                .header("X-Network-Zone", "EXTERNAL")
                .with(TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO_LEADER))
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INTERNAL_ONLY"));
  }

  @Test
  void 팀장이_올린_파일은_바로_승인되고_참가기관이_올린_파일은_결재가_없다() throws Exception {
    long noticeId = noticeId();
    long notificationsBefore = requestNotifications();
    upload(TestUsers.KFTC_ACCOUNT_INFO_LEADER, noticeId, "팀장자료.pdf")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
        .andExpect(jsonPath("$.approverName").value("강지원"));
    // 팀장 본인 파일은 결재 요청이 가지 않는다.
    assertThat(requestNotifications()).isEqualTo(notificationsBefore);

    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(new MockMultipartFile("file", "공문.pdf", "application/pdf", CONTENT))
                .param("ownerType", "DEMO")
                .param("ownerId", "31")
                .with(TestUsers.ganaMember())
                .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.approvalStatus").value("NOT_REQUIRED"));
  }

  @Test
  void 같은_팀에_팀장이_없으면_올리지_못한다() throws Exception {
    // 거부: 상황실에는 팀장 계정이 없다(시드).
    CurrentUser control =
        new CurrentUser(
            "lee.control@kftc.example.com",
            "이수진",
            "KFTC",
            "금융결제원",
            OrganizationType.KFTC,
            false,
            Set.of(Business.values()),
            JobRole.IT,
            Set.of(com.connectlink.organization.ItRole.CONTROL_ROOM));
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(new MockMultipartFile("file", "공문.pdf", "application/pdf", CONTENT))
                .param("ownerType", "DEMO")
                .param("ownerId", "32")
                .with(TestUsers.as(control))
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ATTACHMENT_APPROVER_NOT_FOUND"));
  }
}
