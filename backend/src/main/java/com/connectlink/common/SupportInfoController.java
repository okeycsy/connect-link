package com.connectlink.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 전 화면이 쓰는 문의 안내와 개인정보 처리방침의 관리부서 연락처. 로그인 없이 본다. */
@RestController
@RequestMapping("/api/v1/support-info")
@Tag(name = "공통", description = "서버 상태")
public class SupportInfoController {

  private final SupportProperties properties;

  public SupportInfoController(SupportProperties properties) {
    this.properties = properties;
  }

  @GetMapping
  @Operation(
      summary = "문의 안내 조회",
      description = "문의 메일 주소와 개인정보 관리부서 이름·전화. 설정하지 않은 값은 null 이다.")
  public SupportInfoResponse supportInfo() {
    return new SupportInfoResponse(
        blankToNull(properties.getInquiryEmail()),
        blankToNull(properties.getPrivacyDepartment()),
        blankToNull(properties.getPrivacyPhone()));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * 문의 안내.
   *
   * @param inquiryEmail 문의 메일 주소. 개인정보 관리부서 메일로도 쓴다. 없으면 null
   * @param privacyDepartment 개인정보 관리부서 이름. 없으면 null
   * @param privacyPhone 개인정보 관리부서 전화번호. 없으면 null
   */
  public record SupportInfoResponse(
      @Schema(nullable = true) String inquiryEmail,
      @Schema(nullable = true) String privacyDepartment,
      @Schema(nullable = true) String privacyPhone) {}
}
