package com.connectlink.chat.dto;

/** 방에 초대할 수 있는 사람 한 명. 그 업무 참가기관 담당자와 결제원 사람(업무 밖 대외계 등 포함)이다. */
public record ChatInviteCandidateResponse(
    String email,
    String name,
    String organizationCode,
    String organizationName,
    boolean kftc,
    String roleLabel) {}
