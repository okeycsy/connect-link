package com.connectlink.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 서버가 떠 있는지와 어떤 프로필로 떠 있는지 알려 준다. 화면의 데모 도구가 프로필을 읽는 데도 쓴다. */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "공통", description = "서버 상태")
public class HealthController {

  private final Environment environment;

  public HealthController(Environment environment) {
    this.environment = environment;
  }

  @GetMapping
  @Operation(summary = "서버 상태 조회")
  public HealthResponse health() {
    String[] profiles = environment.getActiveProfiles();
    if (profiles.length == 0) {
      profiles = environment.getDefaultProfiles();
    }
    return new HealthResponse("UP", String.join(",", profiles), OffsetDateTime.now());
  }

  /** 서버 상태. */
  public record HealthResponse(String status, String profile, OffsetDateTime serverTime) {}
}
