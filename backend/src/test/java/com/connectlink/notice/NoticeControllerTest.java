package com.connectlink.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.opinion.OpinionRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 공지·자료실: 업무 글은 그 업무 담당자만 보고 그 업무 결제원 담당자가 쓴다. 공통 공지는 모두 보고 관리자가 쓴다. 글을 만들므로 따로 쓰는 DB 다. */
@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:notice-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoticeControllerTest {

  private static final String BASE = "/api/v1/notices";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private static RequestPostProcessor hong() {
    return TestUsers.as(TestUsers.KFTC_ACCOUNT_INFO);
  }

  private static RequestPostProcessor kim() {
    return TestUsers.ganaMember();
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

  private static Map<String, Object> body(String business, String category, String title) {
    Map<String, Object> body = new HashMap<>();
    body.put("business", business);
    body.put("category", category);
    body.put("title", title);
    body.put("content", "내용입니다.");
    body.put("pinned", false);
    body.put("effectiveDate", "2026-11-01");
    return body;
  }

  private Long create(Map<String, Object> body, RequestPostProcessor user) throws Exception {
    String json =
        send(post(BASE), body, user)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode node = objectMapper.readTree(json);
    return node.get("id").asLong();
  }

  @Test
  void 업무_담당자는_그_업무_글과_공통_공지를_고정_글부터_본다() throws Exception {
    send(get(BASE).param("business", "account-info"), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].pinned").value(true))
        .andExpect(jsonPath("$.content[*].title", hasItem("연결이 이용 안내")))
        .andExpect(jsonPath("$.content[*].title", hasItem("계좌통합관리업무 규약(예시)")))
        .andExpect(jsonPath("$.content[*].title", not(hasItem("CMS 정산 마감 시각 안내"))))
        .andExpect(jsonPath("$.content[*].canEdit", everyItem(is(false))));
  }

  @Test
  void 분류로_거르면_그_분류만_보인다() throws Exception {
    send(get(BASE).param("business", "account-info").param("category", "RULE"), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].category", everyItem(is("RULE"))))
        .andExpect(jsonPath("$.content[0].effectiveDate").exists());
  }

  @Test
  void 담당_업무가_아니면_목록도_글도_보지_못한다() throws Exception {
    send(get(BASE).param("business", "cms"), null, kim()).andExpect(status().isForbidden());
    Long cmsNotice = create(body("CMS", "NOTICE", "CMS 전용 공지"), TestUsers.admin());
    send(get(BASE + "/" + cmsNotice), null, kim()).andExpect(status().isForbidden());
    send(get(BASE + "/" + cmsNotice), null, TestUsers.as(TestUsers.KFTC_CMS))
        .andExpect(status().isOk());
  }

  @Test
  void 그_업무_결제원_담당자는_글을_올리고_고치고_지운다() throws Exception {
    Long id = create(body("ACCOUNT_INFO", "ENFORCEMENT_RULE", "시행세칙 개정"), hong());
    send(get(BASE + "/" + id), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoryLabel").value("시행세칙"))
        .andExpect(jsonPath("$.authorName").value("홍성민"));
    Map<String, Object> changed = body("CMS", "ENFORCEMENT_RULE", "시행세칙 개정(수정)");
    send(put(BASE + "/" + id), changed, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("시행세칙 개정(수정)"))
        .andExpect(jsonPath("$.business").value("ACCOUNT_INFO"))
        .andExpect(jsonPath("$.canEdit").value(true));
    send(delete(BASE + "/" + id), null, hong()).andExpect(status().isNoContent());
    send(get(BASE + "/" + id), null, hong()).andExpect(status().isNotFound());
  }

  @Test
  void 참가기관_담당자와_다른_업무_결제원_담당자는_올리거나_고치지_못한다() throws Exception {
    send(post(BASE), body("ACCOUNT_INFO", "NOTICE", "기관이 쓴 공지"), kim())
        .andExpect(status().isForbidden());
    send(post(BASE), body("ACCOUNT_INFO", "NOTICE", "다른 업무 공지"), TestUsers.as(TestUsers.KFTC_CMS))
        .andExpect(status().isForbidden());
    Long id = create(body("ACCOUNT_INFO", "NOTICE", "홍성민 공지"), hong());
    send(put(BASE + "/" + id), body(null, "NOTICE", "바꿈"), TestUsers.as(TestUsers.KFTC_CMS))
        .andExpect(status().isForbidden());
    send(delete(BASE + "/" + id), null, kim()).andExpect(status().isForbidden());
  }

  @Test
  void 공통_공지는_관리자만_올리고_모두_본다() throws Exception {
    send(post(BASE), body(null, "NOTICE", "결제원 담당자의 공통 공지"), hong())
        .andExpect(status().isForbidden());
    Long id = create(body(null, "NOTICE", "관리자 공통 공지"), TestUsers.admin());
    send(get(BASE).param("business", "cms"), null, TestUsers.as(TestUsers.KFTC_CMS))
        .andExpect(jsonPath("$.content[*].title", hasItem("관리자 공통 공지")));
    send(get(BASE + "/" + id), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.business").doesNotExist())
        .andExpect(jsonPath("$.effectiveDate").doesNotExist());
  }

  @Test
  void 규약_시행세칙은_공통으로_올리지_못하고_빈_제목은_그_칸에_알린다() throws Exception {
    send(post(BASE), body(null, "RULE", "공통 규약"), TestUsers.admin())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("NOTICE_COMMON_REGULATION"));
    send(post(BASE), body("ACCOUNT_INFO", "NOTICE", " "), hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
  }

  @Test
  void 회의_안내에_이은_참석_회신_요청은_대상_기관_담당자에게_회신하기로_보인다() throws Exception {
    String json =
        send(get(BASE).param("business", "account-info"), null, kim())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode meeting = null;
    for (JsonNode n : objectMapper.readTree(json).get("content")) {
      if (n.get("title").asText().equals("계좌통합관리업무 전문 변경 회의 개최 안내")) {
        meeting = n;
      }
    }
    assertThat(meeting).isNotNull();
    assertThat(meeting.at("/opinionRequest/title").asText()).isEqualTo("계좌통합관리업무 전문 변경 회의 참석 회신");
    assertThat(meeting.at("/opinionRequest/replyable").asBoolean()).isTrue();
    long noticeId = meeting.get("id").asLong();

    // 결제원 담당자는 요청을 보지만 회신하지 않는다.
    send(get(BASE + "/" + noticeId), null, hong())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.opinionRequest.status").value("OPEN"))
        .andExpect(jsonPath("$.opinionRequest.replyable").value(false));
  }

  @Test
  void 볼_수_없는_의견_요청은_잇지_않은_것처럼_보이고_다른_업무_요청은_이을_수_없다() throws Exception {
    // 작성중 요청을 이으면 기관 담당자에게는 보이지 않는다.
    Long draftId = opinionId("야간 배치 시간 조정 의견 요청");
    Map<String, Object> linked = body("ACCOUNT_INFO", "NOTICE", "작성중 요청을 이은 공지");
    linked.put("opinionRequestId", draftId);
    Long id = create(linked, hong());
    send(get(BASE + "/" + id), null, kim())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.opinionRequest").doesNotExist());
    send(get(BASE + "/" + id), null, hong())
        .andExpect(jsonPath("$.opinionRequest.id").value(draftId));

    Map<String, Object> wrong = body("ACCOUNT_INFO", "NOTICE", "다른 업무 요청");
    wrong.put("opinionRequestId", opinionId("CMS 출금이체 전문 개정 의견 요청"));
    send(post(BASE), wrong, hong())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("NOTICE_OPINION_MISMATCH"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("opinionRequestId"));

    Map<String, Object> common = body(null, "NOTICE", "공통 공지에 잇기");
    common.put("opinionRequestId", draftId);
    send(post(BASE), common, TestUsers.admin())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("NOTICE_OPINION_MISMATCH"));
  }

  @Autowired private OpinionRequestRepository opinionRequestRepository;

  private Long opinionId(String title) {
    return opinionRequestRepository.findAll().stream()
        .filter(r -> r.getTitle().equals(title))
        .findFirst()
        .orElseThrow()
        .getId();
  }
}
