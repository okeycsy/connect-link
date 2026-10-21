package com.connectlink.chat.dto;

import com.connectlink.attachment.dto.AttachmentResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 메시지 한 건.
 *
 * @param senderKftc 결제원 사람이 보냈는지(아바타 색)
 * @param mine 내가 보냈는지
 * @param unreadCount 이 메시지를 아직 읽지 않은 구성원 수(보낸 사람 빼고)
 * @param mentionsMe 내 기관을 멘션했는지
 * @param attachments 첨부(CHAT_MESSAGE). 보관 기간이 지나 지웠으면 비고 attachmentsExpired 가 true 다
 * @param canAck 확인 공감을 남길 수 있는지(다른 사람이 보낸 메시지)
 */
public record ChatMessageResponse(
    Long id,
    String senderEmail,
    String senderName,
    String senderOrganizationCode,
    String senderOrganizationName,
    boolean senderKftc,
    boolean mine,
    String content,
    LocalDateTime sentAt,
    long unreadCount,
    List<ChatMentionResponse> mentions,
    boolean mentionsMe,
    List<ChatAckResponse> acks,
    boolean withAttachments,
    List<AttachmentResponse> attachments,
    boolean attachmentsExpired,
    boolean canAck) {}
