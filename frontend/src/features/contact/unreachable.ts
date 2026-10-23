import { formatAuthor, formatDateTime } from '../../shared/format';
import type { ContactUnreachable } from './hooks';

/** 연락 불가 표시 한 줄. "2026-10-24 02:10 결제원(이수진 주임) · 메모". 표시는 결제원 담당자만 남긴다. */
export function unreachableSummary(u: ContactUnreachable): string {
  const head = `${formatDateTime(u.markedAt)} ${formatAuthor('결제원', u.markedByName, u.markedByJobTitle)}`;
  return u.note ? `${head} · ${u.note}` : head;
}
