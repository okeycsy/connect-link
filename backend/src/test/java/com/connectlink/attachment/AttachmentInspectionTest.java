package com.connectlink.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.auth.TestUsers;
import com.connectlink.common.ClockConfig;
import com.connectlink.common.MutableClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** 반입 보안 검사: 결제원 밖 계정이 올린 파일은 검사를 통과해야 내려받는다. 프로토타입 검사는 3초 뒤 통과다. */
@SpringBootTest(properties = "app.attachment.inspection.simulated-delay=3s")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttachmentInspectionTest {

  @TestConfiguration
  static class ClockOverride {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(ClockConfig.SERVER_ZONE);
    }
  }

  private static final byte[] CONTENT = "가상 공문 내용".getBytes(StandardCharsets.UTF_8);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private MutableClock clock;
  @Autowired private AttachmentInspectionService inspectionService;

  private MvcResult upload(RequestPostProcessor user, String ownerId) throws Exception {
    return mockMvc
        .perform(
            multipart("/api/v1/attachments")
                .file(new MockMultipartFile("file", "공문.pdf", "application/pdf", CONTENT))
                .param("ownerType", "DEMO")
                .param("ownerId", ownerId)
                .with(user)
                .with(csrf()))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private long idOf(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  @Test
  void 참가기관이_올린_파일은_검사_중에는_못_받고_3초_뒤_통과하면_받는다() throws Exception {
    MvcResult uploaded = upload(TestUsers.ganaMember(), "71");
    long id = idOf(uploaded);
    assertThat(
            objectMapper
                .readTree(uploaded.getResponse().getContentAsString())
                .get("inspectionStatus")
                .asText())
        .isEqualTo("PENDING");

    // 검사 중: 목록에는 상태와 함께 보이지만 결제원도 올린 사람도 내려받지 못한다.
    mockMvc
        .perform(
            get("/api/v1/attachments")
                .param("ownerType", "DEMO")
                .param("ownerId", "71")
                .with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].inspectionStatus").value("PENDING"));
    mockMvc
        .perform(get("/api/v1/attachments/{id}/download", id).with(TestUsers.admin()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ATTACHMENT_INSPECTION_PENDING"));
    mockMvc
        .perform(get("/api/v1/attachments/{id}/download", id).with(TestUsers.ganaMember()))
        .andExpect(status().isConflict());

    // 3초가 되기 전에는 그대로다.
    clock.advance(Duration.ofSeconds(2));
    assertThat(inspectionService.completePending()).isZero();

    clock.advance(Duration.ofSeconds(1));
    assertThat(inspectionService.completePending()).isEqualTo(1);
    mockMvc
        .perform(
            get("/api/v1/attachments")
                .param("ownerType", "DEMO")
                .param("ownerId", "71")
                .with(TestUsers.admin()))
        .andExpect(jsonPath("$[0].inspectionStatus").value("PASSED"));
    mockMvc
        .perform(get("/api/v1/attachments/{id}/download", id).with(TestUsers.admin()))
        .andExpect(status().isOk())
        .andExpect(content().bytes(CONTENT));
  }

  @Test
  void 결제원_계정이_올린_파일은_반입_검사가_없다() throws Exception {
    MvcResult uploaded = upload(TestUsers.admin(), "72");
    assertThat(
            objectMapper
                .readTree(uploaded.getResponse().getContentAsString())
                .get("inspectionStatus")
                .asText())
        .isEqualTo("NOT_REQUIRED");
  }
}
