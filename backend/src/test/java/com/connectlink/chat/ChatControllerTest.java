package com.connectlink.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Period;
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
 * 마. 소통 채널: 업무 단체방 구성원, 초대방 만들기·초대·나가기, 메시지·멘션·읽음, 확인 공감, 첨부와 보관 기간. 시드 방은 읽기만 하고, 쓰는 테스트는 새 초대방을
 * 만들어 쓴다. 따로 쓰는 DB 다.
 */
@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:chat-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

  private static final String ROOMS = "/api/v1/chat-rooms";

  /** 신한은행 계좌통합관리 담당자. 시드 초대방 구성원이 아니다. */
  private static final CurrentUser SHINHAN_ACCOUNT_INFO =
      new CurrentUser(
          "park.jihoon@shinhan.example.com",
          "박지훈",
          "088",
          "신한은행",
          OrganizationType.FINANCE_ORGANIZATION,
          false,
          Set.of(Business.ACCOUNT_INFO),
          JobRole.BUSINESS,
          Set.of());

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ChatService chatService;
  @Autowired private ChatProperties properties;
  @Autowired private ChatRoomRepository roomRepository;

  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  private static RequestPostProcessor kim() {
    return TestUsers.ganaMember();
  }

  private static RequestPostProcessor choi() {
    return TestUsers.as(TestUsers.SAA_MEMBER);
  }

  private static RequestPostProcessor parkCms() {
    return TestUsers.as(TestUsers.KFTC_CMS);
  }

  private static RequestPostProcessor shinhan() {
    return TestUsers.as(SHINHAN_ACCOUNT_INFO);
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

  private Long roomId(String name) {
    return roomRepository.findAll().stream()
        .filter(r -> r.getName().equals(name))
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private Long businessRoom() {
    return roomId("계좌통합관리 단체방");
  }

  /** 홍성민이 최유진과 함께 새 초대방을 만든다. */
  private Long newRoom(String name) throws Exception {
    return json(send(
                post(ROOMS),
                Map.of(
                    "business",
                    "ACCOUNT_INFO",
                    "name",
                    name,
                    "memberEmails",
                    List.of("choi.yujin@samsung.example.com")),
                hong())
            .andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  private Long sendMessage(Long roomId, String content, RequestPostProcessor user)
      throws Exception {
    return json(send(
                post(ROOMS + "/" + roomId + "/messages"),
                Map.of("content", content, "mentionOrganizationCodes", List.of()),
                user)
            .andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  // ---- 방 목록·구성원 ----

  @Test
  void 업무_단체방은_그_업무_담당자가_저절로_구성원이다() throws Exception {
    send(get(ROOMS).param("business", "account-info"), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("계좌통합관리 단체방"))
        .andExpect(jsonPath("$[0].type").value("BUSINESS"))
        .andExpect(jsonPath("$[*].name").value(not(hasItem("오픈뱅킹 안심차단 전문 테스트"))));
    send(get(ROOMS).param("business", "account-info"), null, choi())
        .andExpect(jsonPath("$[*].name").value(hasItem("오픈뱅킹 안심차단 전문 테스트")));
    send(get(ROOMS + "/" + businessRoom()), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members[*].name").value(hasItem("김민준")))
        .andExpect(jsonPath("$.mentionables[0].label").value("결제원"))
        .andExpect(jsonPath("$.permissions.invite").value(false))
        .andExpect(jsonPath("$.permissions.leave").value(false));
  }

  @Test
  void 다른_업무_사람도_초대받으면_그_방을_본다() throws Exception {
    // CMS 박하늘은 계좌통합관리 담당이 아니지만 시드에서 계좌통합관리 단체방에 초대받았다.
    send(get(ROOMS).param("business", "cms"), null, parkCms())
        .andExpect(jsonPath("$[0].name").value("CMS 단체방"))
        .andExpect(jsonPath("$[*].name").value(hasItem("계좌통합관리 단체방")));
    send(get(ROOMS + "/" + businessRoom()), null, parkCms())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.permissions.leave").value(true));
  }

  @Test
  void 구성원이_아니면_방과_메시지를_보지_못한다() throws Exception {
    Long custom = roomId("오픈뱅킹 안심차단 전문 테스트");
    send(get(ROOMS + "/" + custom), null, kim()).andExpect(status().isForbidden());
    send(get(ROOMS + "/" + custom + "/messages"), null, kim()).andExpect(status().isForbidden());
    send(post(ROOMS + "/" + custom + "/messages"), Map.of("content", "들어가도 되나요?"), kim())
        .andExpect(status().isForbidden());
    send(get(ROOMS + "/" + custom + "/messages"), null, choi())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].senderName").value("윤서연"));
  }

  // ---- 방 만들기·초대·나가기 ----

  @Test
  void 결제원_담당자만_방을_만든다() throws Exception {
    Long id = newRoom("응답코드 변경 확인");
    send(get(ROOMS + "/" + id), null, choi())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("CUSTOM"))
        .andExpect(jsonPath("$.members.length()").value(2))
        .andExpect(jsonPath("$.permissions.invite").value(false));
    Map<String, Object> body = Map.of("business", "ACCOUNT_INFO", "name", "기관이 만든 방");
    send(post(ROOMS), body, kim()).andExpect(status().isForbidden());
    // 다른 업무의 결제원 담당자도 만들지 못한다.
    send(post(ROOMS), body, parkCms()).andExpect(status().isForbidden());
  }

  @Test
  void 그_업무_담당자가_아닌_기관_사람은_초대하지_못한다() throws Exception {
    // 신한은행 이준호는 계좌통합관리 담당이 아니다.
    send(
            post(ROOMS),
            Map.of(
                "business",
                "ACCOUNT_INFO",
                "name",
                "잘못 고른 방",
                "memberEmails",
                List.of("lee.junho@shinhan.example.com")),
            hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CHAT_INVITEE_INVALID"));
  }

  @Test
  void 결제원_담당자가_초대하고_기관_담당자는_초대하지_못한다() throws Exception {
    Long id = newRoom("초대 시험");
    send(get(ROOMS + "/" + id + "/invite-candidates"), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].email").value(hasItem("park.jihoon@shinhan.example.com")))
        // 업무 밖 결제원 사람(CMS)도 부를 수 있다.
        .andExpect(jsonPath("$[*].email").value(hasItem("park.cms@kftc.example.com")))
        .andExpect(jsonPath("$[*].email").value(not(hasItem("choi.yujin@samsung.example.com"))))
        // 초대 화면이 업무·직무로 고르도록 담당 업무와 직무를 함께 준다.
        .andExpect(
            jsonPath("$[?(@.email == 'park.cms@kftc.example.com')].businesses[*]")
                .value(hasItem("CMS")))
        .andExpect(
            jsonPath("$[?(@.email == 'park.cms@kftc.example.com')].jobRole")
                .value(hasItem("BUSINESS")));
    send(
            post(ROOMS + "/" + id + "/members"),
            Map.of("emails", List.of("park.jihoon@shinhan.example.com")),
            hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.members[*].name").value(hasItem("박지훈")));
    send(get(ROOMS + "/" + id + "/invite-candidates"), null, choi())
        .andExpect(status().isForbidden());
    send(
            post(ROOMS + "/" + id + "/members"),
            Map.of("emails", List.of("park.cms@kftc.example.com")),
            choi())
        .andExpect(status().isForbidden());
  }

  @Test
  void 초대받은_사람은_나가고_업무_단체방_담당자는_나가지_못한다() throws Exception {
    Long id = newRoom("나가기 시험");
    send(delete(ROOMS + "/" + id + "/members/me"), null, choi()).andExpect(status().isNoContent());
    send(get(ROOMS + "/" + id), null, choi()).andExpect(status().isForbidden());
    send(delete(ROOMS + "/" + businessRoom() + "/members/me"), null, kim())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CHAT_CANNOT_LEAVE"));
  }

  // ---- 메시지·멘션·읽음 ----

  @Test
  void 메시지를_보내면_읽지_않은_사람_수가_보이고_읽으면_줄어든다() throws Exception {
    Long id = newRoom("읽음 시험");
    Long messageId = sendMessage(id, "내일 반영 일정 공유드립니다.", hong());
    send(get(ROOMS + "/" + id + "/messages"), null, hong())
        .andExpect(jsonPath("$.content[0].id").value(messageId))
        .andExpect(jsonPath("$.content[0].mine").value(true))
        .andExpect(jsonPath("$.content[0].unreadCount").value(1));
    send(get(ROOMS).param("business", "account-info"), null, choi())
        .andExpect(jsonPath("$[?(@.id == " + id + ")].unreadCount").value(1));
    send(post(ROOMS + "/" + id + "/read"), Map.of("lastMessageId", messageId), choi())
        .andExpect(status().isNoContent());
    send(get(ROOMS + "/" + id + "/messages"), null, hong())
        .andExpect(jsonPath("$.content[0].unreadCount").value(0));
    send(get(ROOMS).param("business", "account-info"), null, choi())
        .andExpect(jsonPath("$[?(@.id == " + id + ")].unreadCount").value(0));
  }

  @Test
  void 구성원의_기관만_멘션하고_멘션받은_기관은_표시를_본다() throws Exception {
    Long id = newRoom("멘션 시험");
    send(
            post(ROOMS + "/" + id + "/messages"),
            Map.of("content", "@삼성증권 확인 부탁드립니다.", "mentionOrganizationCodes", List.of("240")),
            hong())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mentions[0].label").value("삼성증권"));
    send(get(ROOMS + "/" + id + "/messages"), null, choi())
        .andExpect(jsonPath("$.content[0].mentionsMe").value(true));
    send(get(ROOMS).param("business", "account-info"), null, choi())
        .andExpect(jsonPath("$[?(@.id == " + id + ")].unreadMention").value(true));
    // KB국민은행은 이 방 구성원이 아니다.
    send(
            post(ROOMS + "/" + id + "/messages"),
            Map.of("content", "@KB국민은행 확인", "mentionOrganizationCodes", List.of("004")),
            hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CHAT_MENTION_INVALID"));
    send(post(ROOMS + "/" + id + "/messages"), Map.of("content", "  "), hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CHAT_MESSAGE_EMPTY"));
  }

  // ---- 확인 공감 ----

  @Test
  void 다른_사람_메시지에만_확인을_남기고_거둔다() throws Exception {
    Long id = newRoom("확인 시험");
    Long messageId = sendMessage(id, "전문 응답코드 확인해 주세요.", hong());
    send(put("/api/v1/chat-messages/" + messageId + "/ack"), null, choi())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acks[0].organizationName").value("삼성증권"))
        .andExpect(jsonPath("$.acks[0].mine").value(true));
    send(put("/api/v1/chat-messages/" + messageId + "/ack"), null, hong())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CHAT_ACK_OWN_MESSAGE"));
    send(put("/api/v1/chat-messages/" + messageId + "/ack"), null, kim())
        .andExpect(status().isForbidden());
    send(delete("/api/v1/chat-messages/" + messageId + "/ack"), null, choi())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acks.length()").value(0));
  }

  @Test
  void 시드의_멘션과_확인이_보인다() throws Exception {
    JsonNode page = json(send(get(ROOMS + "/" + businessRoom() + "/messages"), null, kim()));
    JsonNode notice = null;
    for (JsonNode m : page.get("content")) {
      if (m.get("content").asText().startsWith("@KB국민은행 @삼성증권")) {
        notice = m;
      }
    }
    assertThat(notice).isNotNull();
    assertThat(notice.get("mentionsMe").asBoolean()).isTrue();
    assertThat(notice.get("acks").get(0).get("organizationName").asText()).isEqualTo("KB국민은행");
  }

  // ---- 첨부 ----

  @Test
  void 보낸_사람만_첨부를_올리고_구성원은_내려받으며_보관_기간이_지나면_지운다() throws Exception {
    Long id = newRoom("첨부 시험");
    Long messageId =
        json(send(
                    post(ROOMS + "/" + id + "/messages"),
                    Map.of("content", "", "withAttachments", true),
                    hong())
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "테스트케이스.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(file)
                .param("ownerType", "CHAT_MESSAGE")
                .param("ownerId", String.valueOf(messageId))
                .with(csrf())
                .with(choi()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(file)
                .param("ownerType", "CHAT_MESSAGE")
                .param("ownerId", String.valueOf(messageId))
                .with(csrf())
                .with(hong()))
        .andExpect(status().isCreated());
    send(get(ROOMS + "/" + id + "/messages"), null, choi())
        .andExpect(jsonPath("$.content[0].attachments[0].name").value("테스트케이스.pdf"));
    send(
            get("/api/v1/attachments")
                .param("ownerType", "CHAT_MESSAGE")
                .param("ownerId", String.valueOf(messageId)),
            null,
            kim())
        .andExpect(status().isForbidden());

    Period retention = properties.getAttachmentRetention();
    try {
      properties.setAttachmentRetention(Period.ZERO);
      assertThat(chatService.expireAttachments()).isGreaterThanOrEqualTo(1);
    } finally {
      properties.setAttachmentRetention(retention);
    }
    send(get(ROOMS + "/" + id + "/messages"), null, choi())
        .andExpect(jsonPath("$.content[0].attachmentsExpired").value(true))
        .andExpect(jsonPath("$.content[0].attachments.length()").value(0));
  }

  // ---- 계정 탈퇴·단체방 ----

  @Test
  void 탈퇴하면_초대받은_방에서_빠진다() throws Exception {
    Long id = newRoom("탈퇴 시험");
    send(
            post(ROOMS + "/" + id + "/members"),
            Map.of("emails", List.of("park.jihoon@shinhan.example.com")),
            hong())
        .andExpect(status().isOk());
    send(get(ROOMS + "/" + id), null, shinhan()).andExpect(status().isOk());
    chatService.removeEverywhere("park.jihoon@shinhan.example.com");
    send(get(ROOMS + "/" + id), null, shinhan()).andExpect(status().isForbidden());
  }

  @Test
  void 업무마다_단체방이_하나씩_있다() {
    assertThat(chatService.ensureBusinessRooms()).isZero();
    for (Business business : Business.values()) {
      assertThat(roomRepository.findFirstByBusinessAndType(business, ChatRoomType.BUSINESS))
          .isPresent();
    }
  }
}
