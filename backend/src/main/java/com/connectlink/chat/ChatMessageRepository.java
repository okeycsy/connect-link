package com.connectlink.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  /** 최근 메시지부터. */
  Page<ChatMessage> findByRoom_IdOrderByIdDesc(Long roomId, Pageable pageable);

  Optional<ChatMessage> findFirstByRoom_IdOrderByIdDesc(Long roomId);

  /** 이 메시지 뒤에 다른 사람이 보낸 메시지 수(읽지 않은 메시지). */
  long countByRoom_IdAndIdGreaterThanAndSenderEmailNot(
      Long roomId, Long afterMessageId, String email);

  /** 이 메시지 뒤에 다른 사람이 보낸, 이 기관을 멘션한 메시지가 있는지. */
  @Query(
      "select count(m) > 0 from ChatMessage m join m.mentions n where m.room.id = :roomId"
          + " and m.id > :after and m.senderEmail <> :email and n.organizationCode = :code")
  boolean existsUnreadMention(
      @Param("roomId") Long roomId,
      @Param("after") Long afterMessageId,
      @Param("email") String email,
      @Param("code") String organizationCode);

  /** 이 메시지 뒤에 다른 사람이 보낸, 이 기관을 멘션한 메시지. 최근 것부터. */
  @Query(
      "select m from ChatMessage m join m.mentions n where m.room.id = :roomId"
          + " and m.id > :after and m.senderEmail <> :email and n.organizationCode = :code"
          + " order by m.id desc")
  List<ChatMessage> findUnreadMentions(
      @Param("roomId") Long roomId,
      @Param("after") Long afterMessageId,
      @Param("email") String email,
      @Param("code") String organizationCode,
      Pageable pageable);

  /** 보관 기간이 지났고 아직 첨부를 지우지 않은 첨부 메시지. */
  List<ChatMessage> findByWithAttachmentsTrueAndAttachmentsExpiredFalseAndCreatedAtBefore(
      LocalDateTime cutoff);
}
