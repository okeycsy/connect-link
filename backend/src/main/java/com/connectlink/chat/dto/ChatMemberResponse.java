package com.connectlink.chat.dto;

/**
 * 방 구성원 한 명.
 *
 * @param kftc 결제원 사람인지(아바타 색, 초대 권한 표시)
 * @param roleLabel 역할 표기(예: "참가기관 · 현업")
 * @param invited 따로 초대한 사람인지. 업무 단체방의 업무 담당자는 false 이고 나가지 못한다
 */
public record ChatMemberResponse(
    String email,
    String name,
    String organizationCode,
    String organizationName,
    boolean kftc,
    String roleLabel,
    boolean invited) {}
