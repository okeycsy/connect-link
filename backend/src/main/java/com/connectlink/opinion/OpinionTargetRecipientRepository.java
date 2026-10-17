package com.connectlink.opinion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpinionTargetRecipientRepository
    extends JpaRepository<OpinionTargetRecipient, Long> {

  List<OpinionTargetRecipient> findByAccountId(Long accountId);
}
