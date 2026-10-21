package com.connectlink.chat;

import com.connectlink.organization.Business;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  List<ChatRoom> findByBusinessOrderByTypeAscIdAsc(Business business);

  Optional<ChatRoom> findFirstByBusinessAndType(Business business, ChatRoomType type);
}
