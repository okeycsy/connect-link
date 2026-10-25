import { describe, expect, it } from 'vitest';

import { meetingReplyTitle } from './noticeReply';

describe('meetingReplyTitle', () => {
  it('제목 끝의 개최 안내·안내를 떼고 참석 여부 회신을 붙인다', () => {
    expect(meetingReplyTitle('11월 전문 변경 회의 개최 안내')).toBe(
      '11월 전문 변경 회의 참석 여부 회신',
    );
    expect(meetingReplyTitle('분기 실무 회의 안내')).toBe('분기 실무 회의 참석 여부 회신');
    expect(meetingReplyTitle('실무자 회의')).toBe('실무자 회의 참석 여부 회신');
  });
});
