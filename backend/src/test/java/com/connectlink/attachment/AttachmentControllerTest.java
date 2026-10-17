package com.connectlink.attachment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttachmentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private static final byte[] CONTENT = "가상 공문 내용".getBytes(StandardCharsets.UTF_8);

  @Test
  void 올리고_목록에서_보고_원래_이름으로_내려받고_지운다() throws Exception {
    MvcResult uploaded =
        mockMvc
            .perform(
                multipart("/api/v1/attachments")
                    .file(new MockMultipartFile("file", "공문.pdf", "application/pdf", CONTENT))
                    .param("ownerType", "DEMO")
                    .param("ownerId", "7")
                    .with(TestUsers.admin())
                    .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("공문.pdf"))
            .andExpect(jsonPath("$.contentType").value("application/pdf"))
            .andExpect(jsonPath("$.size").value(CONTENT.length))
            .andExpect(jsonPath("$.uploadedBy").value("admin@kftc.example.com"))
            .andExpect(jsonPath("$.uploadedAt").isNotEmpty())
            .andReturn();
    long id = objectMapper.readTree(uploaded.getResponse().getContentAsString()).get("id").asLong();

    mockMvc
        .perform(
            get("/api/v1/attachments")
                .param("ownerType", "DEMO")
                .param("ownerId", "7")
                .with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(id));

    mockMvc
        .perform(get("/api/v1/attachments/{id}/download", id).with(TestUsers.ganaMember()))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    containsString("filename*=UTF-8''%EA%B3%B5%EB%AC%B8.pdf")))
        .andExpect(content().bytes(CONTENT));

    mockMvc
        .perform(delete("/api/v1/attachments/{id}", id).with(TestUsers.admin()).with(csrf()))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            get("/api/v1/attachments")
                .param("ownerType", "DEMO")
                .param("ownerId", "7")
                .with(TestUsers.admin()))
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(get("/api/v1/attachments/{id}/download", id).with(TestUsers.admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ATTACHMENT_NOT_FOUND"));
  }

  @Test
  void 빈_파일은_400이다() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]))
                .param("ownerType", "DEMO")
                .param("ownerId", "1")
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("FILE_EMPTY"));
  }

  /** 없는 일시중단에 붙이려 하면 일시중단 권한 빈이 거부한다. */
  @Test
  void 권한_빈이_허락하지_않는_대상에는_관리자도_올리거나_볼_수_없다() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(new MockMultipartFile("file", "a.txt", "text/plain", CONTENT))
                .param("ownerType", "SUSPENSION")
                .param("ownerId", "999999")
                .with(TestUsers.admin())
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    mockMvc
        .perform(
            get("/api/v1/attachments")
                .param("ownerType", "SUSPENSION")
                .param("ownerId", "999999")
                .with(TestUsers.admin()))
        .andExpect(status().isForbidden());
  }

  @Test
  void 로그인_없는_업로드는_401이다() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(new MockMultipartFile("file", "a.txt", "text/plain", CONTENT))
                .param("ownerType", "DEMO")
                .param("ownerId", "1")
                .with(csrf()))
        .andExpect(status().isUnauthorized());
  }
}
