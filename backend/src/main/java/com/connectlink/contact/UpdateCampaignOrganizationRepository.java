package com.connectlink.contact;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateCampaignOrganizationRepository
    extends JpaRepository<UpdateCampaignOrganization, Long> {

  @EntityGraph(attributePaths = "organization")
  List<UpdateCampaignOrganization> findByCampaignId(Long campaignId);

  Optional<UpdateCampaignOrganization> findByCampaignIdAndOrganizationId(
      Long campaignId, Long organizationId);

  void deleteByCampaignId(Long campaignId);
}
