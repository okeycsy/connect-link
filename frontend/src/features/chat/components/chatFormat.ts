import dayjs from 'dayjs';

import { formatDate } from '../../../shared/format';

/** 방 목록·말풍선 옆 시각. 오늘이면 HH:mm, 아니면 날짜(YYYY-MM-DD). */
export function formatChatTime(value: string | null | undefined, now: Date = new Date()): string {
  if (!value) return '';
  const time = dayjs(value);
  return time.isSame(now, 'day') ? time.format('HH:mm') : formatDate(value);
}

/** 말풍선 옆에는 시각만 둔다(날짜는 구분선이 보인다). */
export function formatClock(value: string): string {
  return dayjs(value).format('HH:mm');
}

/** 본문을 멘션(@기관)과 나머지 글로 나눈다. 서버가 알려 준 멘션 이름만 강조한다. */
export function splitMentions(
  content: string,
  labels: string[],
): { text: string; mention: boolean }[] {
  if (labels.length === 0) return [{ text: content, mention: false }];
  const escaped = [...labels]
    .sort((a, b) => b.length - a.length)
    .map((l) => l.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
  // 잡는 괄호가 있는 split 은 홀수 자리에 잡힌 멘션을 둔다.
  return content
    .split(new RegExp(`(@(?:${escaped.join('|')}))`))
    .map((text, i) => ({ text, mention: i % 2 === 1 }))
    .filter((part) => part.text !== '');
}
