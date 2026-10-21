package com.connectlink.chat.dto;

/**
 * 이 방에서 지금 할 수 있는 일.
 *
 * @param invite 사람을 초대할 수 있는지(그 업무 결제원 담당자)
 * @param leave 방에서 나갈 수 있는지(따로 초대받은 사람)
 */
public record ChatRoomPermissions(boolean invite, boolean leave) {}
