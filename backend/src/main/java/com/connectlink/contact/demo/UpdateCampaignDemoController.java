package com.connectlink.contact.demo;

import com.connectlink.contact.UpdateCampaignService;
import com.connectlink.contact.dto.UpdateCampaignRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시연용: 정기 변경 예약 작업(6개월 주기 자동 등록, 미회신 리마인드)을 지금 한 번 돌린다(CLAUDE.md 6절 "예약 작업"). local·demo·test 프로필에만
 * 있다.
 */
@RestController
@RequestMapping("/api/v1/demo/update-campaign-runs")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class UpdateCampaignDemoController {

  private final UpdateCampaignService campaignService;

  public UpdateCampaignDemoController(UpdateCampaignService campaignService) {
    this.campaignService = campaignService;
  }

  @PostMapping
  @Operation(
      summary = "정기 변경 예약 작업 지금 돌리기(데모)",
      description = "매일 9시 30분 예약 작업과 같다. 주기가 돌아온 업무에 주간을 등록하고, 간격이 지난 미회신 기관에 리마인드 메일을 보낸다.")
  public UpdateCampaignRunResponse run() {
    return campaignService.runSchedule();
  }
}
