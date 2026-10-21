package com.connectlink.chat.dto;

import com.connectlink.chat.ChatRoomType;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 방 목록의 한 줄.
 *
 * @param unreadCount 내가 읽지 않은, 다른 사람이 보낸 메시지 수
 * @param unreadMention 읽지 않은 메시지 중 내 기관을 멘션한 것이 있는지
 */
public record ChatRoomSummaryResponse(
    Long id,
    Business business,
    ChatRoomType type,
    String typeLabel,
    String name,
    int memberCount,
    @Schema(nullable = true) String lastMessagePreview,
    @Schema(nullable = true) String lastMessageSenderName,
    @Schema(nullable = true) LocalDateTime lastMessageAt,
    long unreadCount,
    boolean unreadMention) {}
