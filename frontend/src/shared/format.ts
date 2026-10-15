import dayjs from 'dayjs';

/** 날짜 표기 YYYY-MM-DD. 서버 값은 ISO-8601(Asia/Seoul). */
export function formatDate(value: string | Date | null | undefined): string {
  if (!value) return '';
  return dayjs(value).format('YYYY-MM-DD');
}

/** 일시 표기 YYYY-MM-DD HH:mm. */
export function formatDateTime(value: string | Date | null | undefined): string {
  if (!value) return '';
  return dayjs(value).format('YYYY-MM-DD HH:mm');
}

/**
 * 전화번호 표기. 숫자만 남겨 02-1234-5678, 031-123-4567, 010-1234-5678 꼴로 붙인다.
 * 마스킹된 값(010-****-1234)은 서버가 이미 만든 것이므로 그대로 둔다.
 */
export function formatPhone(value: string | null | undefined): string {
  if (!value) return '';
  if (value.includes('*')) return value;
  const digits = value.replace(/\D/g, '');
  if (digits.startsWith('02')) {
    return digits.length === 9
      ? digits.replace(/(\d{2})(\d{3})(\d{4})/, '$1-$2-$3')
      : digits.replace(/(\d{2})(\d{4})(\d{4})/, '$1-$2-$3');
  }
  if (digits.length === 10) return digits.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
  if (digits.length === 11) return digits.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
  return value;
}
