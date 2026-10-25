package com.connectlink.notification.dto;

import java.util.List;

/**
 * 보내기 전 메일 미리보기. 지금 보내면 나갈 제목·본문(기본 문구)과 받을 기관을 보여 준다. 보내는 사람은 제목·본문을 고쳐 보낼 수 있고, 고친 문구는 받는 사람 모두에게
 * 같게 간다.
 *
 * @param subject 기본 메일 제목
 * @param body 기본 메일 본문
 * @param subjectMaxLength 제목 최대 글자 수
 * @param bodyMaxLength 본문 최대 글자 수
 * @param organizations 메일을 받을 기관과 기관마다 받을 사람 수
 * @param recipientCount 메일을 받을 사람 수(겹치는 이메일은 한 번)
 * @param skippedOrganizationNames 받을 사람이 없어 건너뛸 기관
 */
public record MailPreviewResponse(
    String subject,
    String body,
    int subjectMaxLength,
    int bodyMaxLength,
    List<Organization> organizations,
    int recipientCount,
    List<String> skippedOrganizationNames) {

  /** 메일 제목 최대 글자 수(알림 제목 칼럼 길이). */
  public static final int SUBJECT_MAX = 200;

  /** 메일 본문 최대 글자 수(알림 본문 칼럼 길이). */
  public static final int BODY_MAX = 1000;

  /** 받을 기관 하나. */
  public record Organization(
      Long organizationId, String organizationName, String organizationCode, int recipientCount) {}

  public static MailPreviewResponse of(
      String subject,
      String body,
      List<Organization> organizations,
      int recipientCount,
      List<String> skippedOrganizationNames) {
    return new MailPreviewResponse(
        subject,
        body,
        SUBJECT_MAX,
        BODY_MAX,
        organizations,
        recipientCount,
        skippedOrganizationNames);
  }
}
