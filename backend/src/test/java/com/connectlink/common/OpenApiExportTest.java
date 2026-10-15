package com.connectlink.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OpenAPI 문서를 build/openapi.json 으로 내보낸다. 프론트의 {@code npm run gen:api}가 이 파일에서 schema.d.ts 를 만든다.
 * 서버를 띄우지 않고도 타입을 다시 만들 수 있게 테스트로 둔다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiExportTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void OpenAPI_문서를_build_디렉터리에_내보낸다() throws Exception {
    String json =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(json).contains("/api/v1/organizations").contains("/api/v1/health");

    String exportPath = System.getProperty("openapi.export.path");
    if (exportPath != null) {
      Path target = Path.of(exportPath);
      Files.createDirectories(target.getParent());
      Files.writeString(target, json, StandardCharsets.UTF_8);
    }
  }
}
