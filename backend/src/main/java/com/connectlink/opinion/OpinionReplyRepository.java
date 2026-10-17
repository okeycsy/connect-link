package com.connectlink.opinion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpinionReplyRepository extends JpaRepository<OpinionReply, Long> {

  List<OpinionReply> findByTarget_Request_Id(Long requestId);

  Optional<OpinionReply> findByTarget_Id(Long targetId);

  boolean existsByTarget_Id(Long targetId);
}
