import type { Chapter } from './types.ts';

/** 기대효과 → 플랫폼화 → 자동 테스트·PR 숫자 → 로고. 심사 기준의 "완성도"와 "창의성·AI 활용"에 답한다. */
export const outro: Chapter = {
  id: 'outro',
  title: '기대효과와 마무리',
  scenes: [
    {
      id: 'effect',
      kind: 'card',
      duration: 5700,
      card: {
        tone: 'light',
        kicker: '업무 하나에 해마다 198시간 · 사업 계층별 분류체계 소분류 94개 업무로 넓히면',
        stat: { value: 18612, unit: '시간 절감', label: '해마다, 우리 원 전체에서' },
      },
      narration: '우리 원 모든 업무로 넓히면 만 8천 612시간.',
    },
    {
      id: 'platform',
      kind: 'card',
      duration: 5100,
      card: {
        tone: 'light',
        kicker: '수기에서 플랫폼으로',
        title: '수기로 처리하던 소통을\n하나의 플랫폼으로.',
        subtitle: '표준 양식, 자동 집계, 자동 리마인드로 더 빠르고 정확한 소통',
      },
      narration: '수기 처리를 하나의 플랫폼으로. (쉼)',
    },
    {
      id: 'quality',
      kind: 'card',
      duration: 2300,
      card: {
        tone: 'light',
        title: '자동 테스트 590개 · PR 60개',
        titleSize: 72,
        subtitle: '서버 433 · 화면 90 · 시나리오 67',
      },
      // 마지막 문장은 이 장면에서 읽기 시작해 로고 화면까지 이어진다.
      narration: '연결이는 이렇게 탄생했습니다.',
    },
    {
      id: 'outro',
      kind: 'card',
      duration: 2700,
      card: {
        tone: 'light',
        image: { src: '/brand/logo.png', width: 520 },
      },
    },
  ],
};
