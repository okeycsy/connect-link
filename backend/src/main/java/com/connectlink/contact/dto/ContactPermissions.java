package com.connectlink.contact.dto;

/**
 * 지금 사용자가 이 담당자에게 할 수 있는 일. 화면은 이 값으로 버튼을 보이고, 서버는 같은 규칙으로 다시 막는다. 결제원 담당자의 수정·삭제·연락 불가 표시는 내부망에서만
 * 되므로 인터넷망이면 false 다.
 *
 * @param edit 고치고 "변동 없음" 으로 확인할 수 있는지
 * @param delete 지울 수 있는지(그 업무 결제원 담당자, 내부망)
 * @param markUnreachable 연락 불가로 표시할 수 있는지(이 업무 담당자를 보는 결제원 쪽, 내부망)
 * @param clearUnreachable 연락 불가 표시를 해제할 수 있는지. 표시가 없으면 false
 */
public record ContactPermissions(
    boolean edit, boolean delete, boolean markUnreachable, boolean clearUnreachable) {}
