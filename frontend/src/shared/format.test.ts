import { describe, expect, it } from 'vitest';

import {
  formatBytes,
  formatDate,
  formatShortDate,
  formatDateTime,
  formatAuthor,
  formatDateTimeRange,
  formatPhone,
} from './format';

describe('format', () => {
  it('날짜와 일시를 정해진 형식으로 보여 준다', () => {
    expect(formatDate('2026-10-15T10:05:00+09:00')).toBe('2026-10-15');
    expect(formatDateTime(new Date(2026, 9, 15, 9, 5))).toBe('2026-10-15 09:05');
    expect(formatDate(null)).toBe('');
    expect(formatShortDate('2026-10-04')).toBe("'26.10.04");
    expect(formatShortDate(null)).toBe('');
  });

  it('일시 구간은 같은 날이면 끝에 시각만 붙인다', () => {
    expect(formatDateTimeRange('2026-10-18T02:00:00', '2026-10-18T04:00:00')).toBe(
      '2026-10-18 02:00 ~ 04:00',
    );
    expect(formatDateTimeRange('2026-10-18T23:00:00', '2026-10-19T01:30:00')).toBe(
      '2026-10-18 23:00 ~ 2026-10-19 01:30',
    );
  });

  it('전화번호를 하이픈으로 붙이고 마스킹된 값은 그대로 둔다', () => {
    expect(formatPhone('0212345678')).toBe('02-1234-5678');
    expect(formatPhone('021234567')).toBe('02-123-4567');
    expect(formatPhone('0311234567')).toBe('031-123-4567');
    expect(formatPhone('01012345678')).toBe('010-1234-5678');
    expect(formatPhone('010-****-5678')).toBe('010-****-5678');
    expect(formatPhone('')).toBe('');
  });
});

describe('formatBytes', () => {
  it('1,024 단위로 올리고 10 미만만 소수 한 자리다', () => {
    expect(formatBytes(0)).toBe('0 B');
    expect(formatBytes(2048)).toBe('2.0 KB');
    expect(formatBytes(15 * 1024 * 1024)).toBe('15 MB');
    expect(formatBytes(null)).toBe('');
  });

  it('글쓴이는 소속 뒤 괄호에 이름과 직급을 붙인다', () => {
    expect(formatAuthor('결제원', '김민찬', '과장')).toBe('결제원(김민찬 과장)');
    expect(formatAuthor('KB국민은행', '김민준', null)).toBe('KB국민은행(김민준)');
    expect(formatAuthor('결제원', undefined)).toBe('결제원');
  });
});
