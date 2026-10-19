package com.connectlink.contact.dto;

/** 6개월 미확인 알림을 보낸 결과. {@code notified} 는 이번에 알린 담당자 수다. */
public record ContactReconfirmRunResponse(int notified) {}
