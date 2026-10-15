package com.connectlink.notification;

/** 알림 발송 채널. 앱 안 알림 센터는 채널이 아니라 기본이다. */
public enum NotificationChannel {
  EMAIL("메일"),
  MATTERMOST("매터모스트");

  private final String label;

  NotificationChannel(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
