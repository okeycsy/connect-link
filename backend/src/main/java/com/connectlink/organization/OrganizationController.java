package com.connectlink.organization;

import com.connectlink.organization.dto.BusinessResponse;
import com.connectlink.organization.dto.OrganizationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기관·업무 기준정보. 가입 화면(로그인 전)도 쓰므로 공개 API다. 연락처 같은 민감 정보는 여기에 담지 않는다. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "기준정보", description = "기관과 업무 영역")
public class OrganizationController {

  private final OrganizationService organizationService;

  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @GetMapping("/organizations")
  @Operation(summary = "기관 목록 조회", description = "사용 중인 기관을 구분·이름순으로 돌려준다.")
  public List<OrganizationResponse> listOrganizations() {
    return organizationService.listActive();
  }

  @GetMapping("/businesses")
  @Operation(summary = "업무 영역 목록 조회", description = "왼쪽 레일과 가입 화면의 업무 선택지에 쓰는 고정 목록이다.")
  public List<BusinessResponse> listBusinesses() {
    return organizationService.listBusinesses();
  }
}
