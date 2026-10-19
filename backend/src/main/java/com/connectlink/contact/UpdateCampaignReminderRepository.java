package com.connectlink.contact;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateCampaignReminderRepository
    extends JpaRepository<UpdateCampaignReminder, Long> {

  List<UpdateCampaignReminder> findByCampaignIdOrderBySentAtAscIdAsc(Long campaignId);

  void deleteByCampaignId(Long campaignId);
}
