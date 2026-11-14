package com.connectlink.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectlink.common.ClockConfig;
import com.connectlink.common.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 실패 잠금(개인정보의 안전성 확보조치 기준 제5조): 5번 연달아 틀리면 30분 동안 맞는 비밀번호로도 로그인하지 못한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginLockTest {

  @TestConfiguration
  static class ClockOverride {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(ClockConfig.SERVER_ZONE);
    }
  }

  private static final String EMAIL = "park.jihoon@shinhan.example.com";
  private static final String PASSWORD = "demo1234!";

  @Autowired private MockMvc mockMvc;
  @Autowired private MutableClock clock;

  private ResultActions login(String password) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, password))
            .with(csrf()));
  }

  @Test
  void 다섯_번_틀리면_잠기고_맞는_비밀번호도_거부하다가_30분이_지나면_풀린다() throws Exception {
    for (int i = 0; i < 4; i++) {
      login("wrong-password")
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
    login("wrong-password")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    // 거부: 잠긴 동안은 맞는 비밀번호도 안 된다.
    login(PASSWORD)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

    // 허용: 잠금 시간이 지나면 다시 로그인한다.
    clock.advance(Duration.ofMinutes(31));
    login(PASSWORD).andExpect(status().isOk());
  }

  @Test
  void 맞게_로그인하면_틀린_횟수를_처음부터_센다() throws Exception {
    for (int i = 0; i < 4; i++) {
      login("wrong-password").andExpect(status().isUnauthorized());
    }
    login(PASSWORD).andExpect(status().isOk());
    for (int i = 0; i < 4; i++) {
      login("wrong-password")
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
  }
}
