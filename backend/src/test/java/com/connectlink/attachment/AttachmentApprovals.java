package com.connectlink.attachment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 테스트 도우미: 결제원 담당자가 올린 파일을 같은 팀 팀장이 반출 승인한다(그래야 참가기관에 보인다). */
public final class AttachmentApprovals {

  private static final ObjectMapper JSON = new ObjectMapper();

  private AttachmentApprovals() {}

  /** 업로드 응답의 파일을 팀장이 승인한다. */
  public static void approve(MockMvc mockMvc, MvcResult uploaded, CurrentUser leader)
      throws Exception {
    long id = JSON.readTree(uploaded.getResponse().getContentAsString()).get("id").asLong();
    mockMvc
        .perform(
            post("/api/v1/attachment-approvals/{id}/approval", id)
                .with(TestUsers.as(leader))
                .with(csrf()))
        .andExpect(status().isOk());
  }
}
