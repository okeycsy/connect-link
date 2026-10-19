package com.connectlink.contact;

import com.connectlink.organization.Business;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateCampaignRepository extends JpaRepository<UpdateCampaign, Long> {

  List<UpdateCampaign> findByBusinessOrderByStartDateDescIdDesc(Business business);

  /** 이 날짜를 기간에 품은 주간. 같은 업무의 주간은 겹치지 않으므로 많아야 하나다. */
  Optional<UpdateCampaign> findFirstByBusinessAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
      Business business, LocalDate onOrAfterStart, LocalDate onOrBeforeEnd);

  /** 기간 [start, end] 와 겹치는 같은 업무의 다른 주간. */
  List<UpdateCampaign> findByBusinessAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
      Business business, LocalDate end, LocalDate start);
}
