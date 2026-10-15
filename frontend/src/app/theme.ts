import {
  createTheme,
  defaultVariantColorsResolver,
  type MantineColorsTuple,
  type VariantColorsResolver,
} from '@mantine/core';

/**
 * 디자인 토큰 (frontend/CLAUDE.md "디자인 시스템: B안 메신저형").
 * 색 이름은 아래 다섯 가지(clay, sky, amber, red, gray)만 쓴다. 화면에서 hex 를 직접 쓰지 않는다.
 * 각 단계의 쓰임은 frontend/CLAUDE.md 의 색 토큰 표가 기준이다.
 */

/** 메인색(클레이). 버튼, 링크, 활성 메뉴, 레일 활성 토큰, 진행률 막대. */
export const clay: MantineColorsTuple = [
  '#F8F1EF', // 0 아주 연한 면(hover)
  '#F1E2DE', // 1 활성 메뉴·연한 배지 배경
  '#E4CBC4', // 2 연한 배지 hover
  '#D6B5AC', // 3 사용자 아바타, 레일 활성 링
  '#BE978C', // 4
  '#A47A6E', // 5 장식 선
  '#8A5A50', // 6 메인. 채움 버튼, 링크, 강조 숫자
  '#7A4A40', // 7 연한 배지 글자
  '#5E3B34', // 8 활성 메뉴 글자, 링크 hover
  '#452A25', // 9
];

/** 정보·완료·진행 중. 결제원 사람 아바타, "진행 중"·"완료" 배지, 차트 1·2단. */
export const sky: MantineColorsTuple = [
  '#EEF4FB',
  '#DCE8F6', // 1 배지 배경
  '#CFE0F3', // 2 날짜 태그·파스텔 배경
  '#AFC9E6',
  '#8FB3DB', // 4 차트 2단
  '#5F8DC0',
  '#2F5A8A', // 6 차트 1단, 채움
  '#24456B', // 7 배지 글자
  '#1C3756',
  '#142840',
];

/** 주의·임박. "D-3", "확인 필요", "미확인", "회신 중" 배지, 차트 경고 단. */
export const amber: MantineColorsTuple = [
  '#FCF4EA',
  '#F8E3CC', // 1 배지 배경
  '#F0CBA3',
  '#E5AE79',
  '#D58D51',
  '#C2672E', // 5 차트 경고 단
  '#A65524', // 6 채움
  '#7A4210', // 7 배지 글자
  '#5E320C',
  '#432308',
];

/** 긴급·초과·새 알림. 읽지 않은 수 배지, 초과 배지, 오류 문구. 면적을 넓게 쓰지 않는다. */
export const red: MantineColorsTuple = [
  '#FBEDE9',
  '#F5D9D0', // 1 배지 배경
  '#EBB3A3',
  '#E0866E', // 3 레일 새 소식 점
  '#D0715A',
  '#C25F46',
  '#B4533A', // 6 채움 배지, 오류 글자
  '#96432F', // 7 연한 배지 글자
  '#7A3626',
  '#5E291D',
];

/** 중립 회색. 0~3 은 면과 선, 6 은 보조 글자(dimmed), 8 은 본문 보조 글자. */
export const gray: MantineColorsTuple = [
  '#F8F8F9', // 0 페이지 바탕, 사이드바
  '#F3F4F6', // 1 레일, 검색 칸, 목록 항목 면
  '#EDEEF1', // 2 표 행 구분선
  '#E3E4E8', // 3 기본 선(헤더·패널 경계, 카드 테두리)
  '#D2D4DA', // 4 입력 칸 강조 테두리
  '#B9BBC2', // 5 점선, 비활성
  '#62656D', // 6 보조 글자(dimmed)
  '#4F525A', // 7
  '#3E4048', // 8 본문 보조 글자
  '#2A2C33', // 9
];

/** 화면 이름이 붙은 면 색. CSS 에서는 `--cl-*` 변수(global.css)로 쓴다. */
export const surface = {
  page: '#F8F8F9',
  card: '#FFFFFF',
  sunken: '#F3F4F6',
  line: '#E3E4E8',
  lineSoft: '#EDEEF1',
  ink: '#1D1715',
  inkSoft: '#3E4048',
  inkMuted: '#62656D',
  online: '#3FA36B',
} as const;

/** 파스텔 번짐(시그니처). 헤더 오른쪽, 사이드바 아래, 홈 머리에만 radial-gradient 로 깐다. */
export const pastel = {
  peach: '#F6D6CC',
  sky: '#CFE0F3',
  butter: '#F7EFB8',
} as const;

/** light 변형은 1단 배경 + 7단 글자로 고정한다(시안의 배지 색과 같게). */
const variantColorResolver: VariantColorsResolver = (input) => {
  const resolved = defaultVariantColorsResolver(input);
  const color = input.color ?? input.theme.primaryColor;
  const tuple = input.theme.colors[color];
  if (input.variant === 'light' && tuple) {
    return {
      ...resolved,
      background: tuple[1],
      hover: tuple[2],
      color: tuple[7],
      border: 'transparent',
    };
  }
  return resolved;
};

export const theme = createTheme({
  primaryColor: 'clay',
  primaryShade: 6,
  colors: { clay, sky, amber, red, gray },
  variantColorResolver,
  white: '#FFFFFF',
  black: surface.ink,
  fontFamily:
    'Pretendard Variable, Pretendard, -apple-system, BlinkMacSystemFont, "Malgun Gothic", "Apple SD Gothic Neo", system-ui, sans-serif',
  fontFamilyMonospace: 'ui-monospace, "Cascadia Mono", Consolas, monospace',
  headings: {
    fontWeight: '700',
    sizes: {
      h1: { fontSize: '34px', lineHeight: '1.3' },
      h2: { fontSize: '22px', lineHeight: '1.35' },
      h3: { fontSize: '18px', lineHeight: '1.4' },
      h4: { fontSize: '16px', lineHeight: '1.45' },
      h5: { fontSize: '15px', lineHeight: '1.5' },
    },
  },
  defaultRadius: 'md',
  radius: { xs: '4px', sm: '8px', md: '12px', lg: '14px', xl: '24px' },
  fontSizes: { xs: '12px', sm: '13px', md: '14px', lg: '15px', xl: '17px' },
  shadows: {
    card: '0 24px 48px -28px rgba(90, 55, 45, 0.28)',
  },
  other: {
    railWidth: 72,
    sidebarWidth: 240,
    headerHeight: 56,
    pageGutter: 24,
  },
  components: {
    Button: {
      defaultProps: { radius: 'xl' },
      styles: { root: { fontWeight: 600 } },
    },
    ActionIcon: {
      defaultProps: { radius: 'xl' },
    },
    Badge: {
      defaultProps: { radius: 'xl', variant: 'light' },
      styles: { root: { textTransform: 'none', fontWeight: 600, letterSpacing: 0 } },
    },
    Card: {
      defaultProps: { radius: 'xl', withBorder: true, padding: 'lg' },
      styles: { root: { borderColor: surface.line } },
    },
    Paper: {
      defaultProps: { radius: 'xl' },
    },
    Input: {
      defaultProps: { radius: 'lg' },
    },
    TextInput: { defaultProps: { radius: 'lg' } },
    PasswordInput: { defaultProps: { radius: 'lg' } },
    Select: { defaultProps: { radius: 'lg' } },
    MultiSelect: { defaultProps: { radius: 'lg' } },
    Textarea: { defaultProps: { radius: 'lg' } },
    NavLink: {
      styles: { root: { borderRadius: 8 } },
    },
    Avatar: {
      defaultProps: { radius: 'sm' },
    },
    Tooltip: {
      defaultProps: { color: 'dark' },
    },
    Menu: {
      defaultProps: { radius: 'lg', shadow: 'card' },
    },
    Popover: {
      defaultProps: { radius: 'lg', shadow: 'card' },
    },
    Modal: {
      defaultProps: { radius: 'xl' },
    },
    Drawer: {
      styles: { content: { borderTopLeftRadius: 24, borderBottomLeftRadius: 24 } },
    },
    Table: {
      styles: {
        th: { color: surface.inkMuted, fontWeight: 600, fontSize: 13 },
      },
    },
  },
});
