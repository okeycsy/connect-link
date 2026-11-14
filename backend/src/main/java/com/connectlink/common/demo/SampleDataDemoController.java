package com.connectlink.common.demo;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시연용: 샘플 데이터를 지금 오늘 기준으로 다시 넣는다(CLAUDE.md 6절 "예약 작업", 매일 4시 작업과 같다). 모든 데이터를 지우므로 관리자만 부른다.
 * local·demo·test 프로필에만 있다.
 */
@RestController
@RequestMapping("/api/v1/demo/sample-data-refreshes")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class SampleDataDemoController {

  private final SampleDataRefresher refresher;

  public SampleDataDemoController(SampleDataRefresher refresher) {
    this.refresher = refresher;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "샘플 데이터 오늘 기준으로 다시 넣기(데모)",
      description = "매일 4시 예약 작업과 같다. 시연 중 만든 데이터와 모든 로그인이 사라진다. 관리자만 부른다.")
  public void refresh(@AuthenticationPrincipal CurrentUser user) {
    if (!user.admin()) {
      throw new BusinessException(
          HttpStatus.FORBIDDEN, "SAMPLE_DATA_REFRESH_ADMIN_ONLY", "샘플 데이터는 관리자만 다시 넣을 수 있습니다.");
    }
    refresher.refresh();
  }
}
