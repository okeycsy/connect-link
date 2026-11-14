package com.connectlink.auth.audit;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.auth.audit.dto.AccessLogResponse;
import com.connectlink.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/access-logs")
@Tag(name = "접속기록", description = "개인정보 처리 접속기록(관리자 점검용)")
public class AccessLogController {

  private final AccessLogService service;
  private final CurrentUserProvider currentUserProvider;

  public AccessLogController(AccessLogService service, CurrentUserProvider currentUserProvider) {
    this.service = service;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "접속기록 목록",
      description = "개인정보를 다룬 요청의 기록(최근 것부터). 관리자만, 내부망에서만 본다(403 INTERNAL_ONLY). 날짜는 그날 하루 전체를 포함한다.")
  public PageResponse<AccessLogResponse> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String email,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(currentUserProvider.require(), from, to, email, page, size);
  }
}
