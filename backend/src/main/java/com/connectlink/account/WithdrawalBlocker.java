package com.connectlink.account;

/**
 * 탈퇴를 막는 까닭 하나. {@link WithdrawalCheck} 가 만든다.
 *
 * @param code 대문자 스네이크 코드(예: {@code LAST_ADMIN})
 * @param message 화면에 그대로 보이는 한국어 문장. 무엇을 하면 탈퇴할 수 있는지까지 쓴다
 * @param link 그 일을 하는 화면 주소. 없으면 null
 */
public record WithdrawalBlocker(String code, String message, String link) {}
