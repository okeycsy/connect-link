package com.connectlink.notification;

/** 발송함 상태. 프로토타입은 실제로 보내지 않으므로 {@code PENDING} 에 머문다. */
public enum OutboxStatus {
  PENDING("발송 대기"),
  SENT("발송됨"),
  FAILED("실패");

  private final String label;

  OutboxStatus(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
