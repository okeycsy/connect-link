package com.connectlink.contact;

import com.connectlink.contact.dto.UpdateCampaignRunResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기 변경 예약 작업. 주기(기본 6개월)가 돌아온 업무에 주간을 등록해 변경 요청 메일을 보내고, 미회신 기관에 리마인드 메일을 다시 보낸다. 때는 {@code
 * app.contact.campaign.cron}(기본 매일 9시 30분, 서버 시간대)이다.
 */
@Component
class UpdateCampaignJob {

  private static final Logger log = LoggerFactory.getLogger(UpdateCampaignJob.class);

  private final UpdateCampaignService campaignService;

  UpdateCampaignJob(UpdateCampaignService campaignService) {
    this.campaignService = campaignService;
  }

  @Scheduled(cron = "${app.contact.campaign.cron:0 30 9 * * *}", zone = "Asia/Seoul")
  void run() {
    UpdateCampaignRunResponse result = campaignService.runSchedule();
    if (result.createdCampaignCount() > 0 || result.remindedOrganizationCount() > 0) {
      log.info(
          "정기 변경: 주간 {}개를 등록하고 미회신 기관 {}곳에 리마인드를 보냈습니다.",
          result.createdCampaignCount(),
          result.remindedOrganizationCount());
    }
  }
}
