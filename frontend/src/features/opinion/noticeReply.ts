/** 회의 공지의 회신 요청명. 제목 끝의 "개최 안내"·"안내" 를 떼고 "참석 여부 회신" 을 붙인다. */
export function meetingReplyTitle(noticeTitle: string): string {
  return `${noticeTitle.replace(/\s*(개최\s*)?안내\s*$/, '').trim()} 참석 여부 회신`;
}
