package com.connectlink.chat.dto;

import com.connectlink.chat.ChatRoomType;
import com.connectlink.organization.Business;
import java.util.List;

/** 방 상세: 구성원, 멘션할 수 있는 기관(구성원의 기관), 권한. */
public record ChatRoomDetailResponse(
    Long id,
    Business business,
    ChatRoomType type,
    String typeLabel,
    String name,
    String createdByName,
    List<ChatMemberResponse> members,
    List<ChatMentionResponse> mentionables,
    ChatRoomPermissions permissions) {}
