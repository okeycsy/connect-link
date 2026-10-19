package com.connectlink.account;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoAccountControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void 로그인_전에도_역할별_데모_계정을_관리자부터_보여_준다() throws Exception {
    mockMvc
        .perform(get("/api/v1/demo/accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.password").value("demo1234!"))
        .andExpect(jsonPath("$.accounts[0].email").value("admin@kftc.example.com"))
        .andExpect(jsonPath("$.accounts[0].admin").value(true))
        .andExpect(jsonPath("$.accounts[1].roleLabel").value(Matchers.startsWith("결제원 · ")))
        .andExpect(
            jsonPath("$.accounts[*].email")
                .value(Matchers.not(Matchers.hasItem("jung.daeun@sbi.example.com"))));
  }

  @Test
  void 목록의_계정은_모두_공통_비밀번호로_로그인된다() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/v1/demo/accounts"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode root = objectMapper.readTree(body);
    String password = root.get("password").asText();
    for (JsonNode account : root.get("accounts")) {
      String email = account.get("email").asText();
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          java.util.Map.of("email", email, "password", password)))
                  .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value(email));
    }
  }
}
