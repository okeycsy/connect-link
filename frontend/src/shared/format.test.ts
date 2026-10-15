import { describe, expect, it } from 'vitest';

import { formatDate, formatDateTime, formatPhone } from './format';

describe('format', () => {
  it('날짜와 일시를 정해진 형식으로 보여 준다', () => {
    expect(formatDate('2026-10-15T10:05:00+09:00')).toBe('2026-10-15');
    expect(formatDateTime(new Date(2026, 9, 15, 9, 5))).toBe('2026-10-15 09:05');
    expect(formatDate(null)).toBe('');
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
