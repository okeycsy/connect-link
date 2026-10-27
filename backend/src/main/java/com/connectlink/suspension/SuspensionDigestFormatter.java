package com.connectlink.suspension;

import java.time.LocalDate;
import java.util.List;

/**
 * 전 영업일 취합 전달의 글 양식. 명세에서 "전달 양식은 나중에 결정" 이라 기본 양식({@link DefaultSuspensionDigestFormatter})을 두었다.
 * 양식이 정해지면 이 인터페이스의 빈을 새로 만들고 기본 양식 빈을 지운다(다른 코드는 바꾸지 않는다).
 */
public interface SuspensionDigestFormatter {

  /** 매터모스트 메시지 제목. */
  String subject(LocalDate fromDate, LocalDate toDate, int count);

  /**
   * 매터모스트 메시지 본문. 발송함 칼럼이 2000자이므로 넘지 않게 만든다.
   *
   * @param suspensions 모은 확정 건. 업무, 시작 시각, 기관 이름순
   */
  String body(LocalDate fromDate, LocalDate toDate, List<Suspension> suspensions);

  /** 확정 즉시 알림의 매터모스트 메시지 제목. */
  String confirmedSubject(Suspension suspension);

  /** 확정 즉시 알림의 매터모스트 메시지 본문. 관련 업무와 확인자별 조치 및 인수인계 사항을 담고 2000자를 넘지 않는다. */
  String confirmedBody(Suspension suspension);
}
