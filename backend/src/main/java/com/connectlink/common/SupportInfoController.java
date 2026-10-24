package com.connectlink.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 전 화면이 쓰는 문의 안내. 로그인 없이 본다. */
@RestController
@RequestMapping("/api/v1/support-info")
@Tag(name = "공통", description = "서버 상태")
public class SupportInfoController {

  private final SupportProperties properties;

  public SupportInfoController(SupportProperties properties) {
    this.properties = properties;
  }

  @GetMapping
  @Operation(summary = "문의 안내 조회", description = "문의 메일 주소. 설정하지 않았으면 inquiryEmail 이 null 이다.")
  public SupportInfoResponse supportInfo() {
    String email = properties.getInquiryEmail();
    return new SupportInfoResponse(email == null || email.isBlank() ? null : email.trim());
  }

  /**
   * 문의 안내.
   *
   * @param inquiryEmail 문의 메일 주소. 없으면 null
   */
  public record SupportInfoResponse(@Schema(nullable = true) String inquiryEmail) {}
}
