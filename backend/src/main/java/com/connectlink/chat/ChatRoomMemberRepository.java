package com.connectlink.chat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

  List<ChatRoomMember> findByEmail(String email);
}
