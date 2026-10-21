package com.connectlink.attachment;

import com.connectlink.common.Labeled;

/**
 * 첨부파일이 붙는 대상 종류. 명세에서 첨부가 있는 곳은 의견 요청(첨부자료), 기관 회신(근거자료), 일시중단(공문), 기관 테스트(가이드·참고자료), 기관 테스트데이터 회신
 * 양식(결제원이 지정), 기관 테스트데이터(대상 기관별), 소통 채널 메시지(사진·영상·문서)다. 기능 모듈이 자기 대상을 쓰려면 {@link
 * AttachmentOwnerAccess} 빈을 함께 만든다. {@code DEMO} 는 데모 도구 화면 전용이며 운영 프로필에는 권한 빈이 없어 쓰이지 않는다.
 */
public enum AttachmentOwnerType implements Labeled {
  OPINION_REQUEST("의견 요청"),
  OPINION_REPLY("기관 회신"),
  SUSPENSION("일시중단"),
  TEST_PLAN("기관 테스트"),
  /** 기관 테스트에 결제원 담당자가 지정한 테스트데이터 회신 양식. ownerId 는 test_plan.id 다. */
  TEST_DATA_FORM("테스트데이터 회신 양식"),
  /** 기관 테스트 대상 기관 한 곳이 올린 테스트데이터. ownerId 는 test_plan_target.id 다. */
  TEST_DATA("기관 테스트데이터"),
  CHAT_MESSAGE("채팅 메시지"),
  DEMO("데모");

  private final String label;

  AttachmentOwnerType(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
