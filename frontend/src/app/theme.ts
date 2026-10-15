import { createTheme, type MantineColorsTuple } from '@mantine/core';

/**
 * 디자인 토큰 (docs/design/common-screens.md "레이어드 네이비").
 * 유채색은 남색(과 틴트)과 빨강 하나뿐이다. 다른 색 이름은 쓰지 않는다.
 */
export const navy: MantineColorsTuple = [
  '#E3E9F5', // 0 navy-100: 선택 행, 활성 항목, 칩, 정보 배지 배경
  '#C9D4EE', // 1 다크 모드 활성 글자
  '#A9B4CC', // 2 navy-300: 비활성 점, 스택 바 4단
  '#8A98B9', // 3
  '#6F80A6', // 4 navy-500: 스택 바 3단
  '#4F74C4', // 5 다크 모드 버튼·링
  '#3A4F7A', // 6 navy-700: 스택 바 2단
  '#243760', // 7
  '#1B2F58', // 8 크롬 그라데이션 시작
  '#0F1E3C', // 9 navy: 메인 남색. 버튼, 링크, 포커스, 활성 표시
];

/** 유일한 보조색. 마감 임박(테두리), 초과·미확인·새 알림(채움)에만 쓴다. */
export const red: MantineColorsTuple = [
  '#FCE8E8',
  '#F9D2D1',
  '#F3A9A6',
  '#EC807B',
  '#E25A54',
  '#D23D36',
  '#B3261E',
  '#931F19',
  '#731813',
  '#52110E',
];

/** 크롬(상단 바, 레일, 로그인 패널, 홈 밴드)의 그라데이션 끝 색. */
export const chrome = {
  start: '#1B2F58',
  mid: '#0F1E3C',
  end: '#0B1730',
  glow: 'rgba(90, 125, 205, 0.42)',
} as const;

export const theme = createTheme({
  primaryColor: 'navy',
  primaryShade: { light: 9, dark: 5 },
  colors: { navy, red },
  white: '#FFFFFF',
  black: '#16213B',
  fontFamily:
    'Pretendard Variable, Pretendard, -apple-system, BlinkMacSystemFont, "Malgun Gothic", "Apple SD Gothic Neo", system-ui, sans-serif',
  fontFamilyMonospace: 'ui-monospace, "Cascadia Mono", Consolas, monospace',
  headings: { fontWeight: '700' },
  defaultRadius: 'md',
  radius: { sm: '8px', md: '12px', lg: '14px' },
  fontSizes: { xs: '11px', sm: '12px', md: '13px', lg: '14px', xl: '16px' },
  shadows: {
    card: '0 1px 2px rgba(27, 42, 74, 0.06), 0 12px 28px -18px rgba(15, 30, 60, 0.28)',
  },
  other: {
    sheet: '#F2F4F8',
    sheetDark: '#0B1120',
    card: '#FFFFFF',
    cardDark: '#131C33',
    line: '#D9DEE8',
    inkMuted: '#5B677F',
    railWidth: 64,
    subMenuWidth: 176,
    headerHeight: 56,
  },
  components: {
    Card: {
      defaultProps: { radius: 'md', withBorder: true, padding: 'lg' },
      styles: { root: { boxShadow: 'var(--mantine-shadow-card)' } },
    },
    Paper: {
      defaultProps: { radius: 'md' },
    },
  },
});
