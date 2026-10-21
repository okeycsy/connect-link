package com.connectlink.chat;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReadMarkRepository extends JpaRepository<ChatReadMark, Long> {

  List<ChatReadMark> findByRoomId(Long roomId);

  Optional<ChatReadMark> findByRoomIdAndEmail(Long roomId, String email);

  void deleteByEmail(String email);
}
