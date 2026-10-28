import type { Chapter } from './types.ts';

/** 기대효과 → 플랫폼화 → 코드 품질 → 로고. 심사 기준의 "완성도"와 "창의성·AI 활용"에 답한다. */
export const outro: Chapter = {
  id: 'outro',
  title: '기대효과와 마무리',
  scenes: [
    {
      id: 'effect',
      kind: 'card',
      duration: 5000,
      card: {
        tone: 'light',
        kicker: '업무 하나에 해마다 198시간 · 사업 계층별 분류체계 소분류 94개 업무로 넓히면',
        stat: { value: 18612, unit: '시간 절감', label: '해마다, 우리 원 전체에서' },
      },
      narration: '업무 하나에 해마다 198시간. 소분류 94개 업무로 넓히면, 해마다 1만 8천 612시간을 절감합니다.',
    },
    {
      id: 'platform',
      kind: 'card',
      duration: 4200,
      card: {
        tone: 'light',
        kicker: '수기에서 플랫폼으로',
        title: '수기로 처리하던 소통을\n하나의 플랫폼으로.',
        subtitle: '표준 양식, 자동 집계, 자동 리마인드로 더 빠르고 정확한 소통',
      },
      narration: '수기와 메일로 처리하던 소통을 하나의 플랫폼으로 옮겼습니다. 표준 양식, 자동 집계, 자동 리마인드로 더 빠르고 정확한 소통.',
    },
    {
      id: 'quality',
      kind: 'card',
      duration: 4200,
      card: {
        tone: 'light',
        kicker: '코드 품질',
        title: '자동 테스트 590개가\n지키는 코드.',
        subtitle: '서버 433 · 화면 90 · 시나리오 67 — 요구사항부터 리뷰까지 AI 페어 프로그래밍으로 12일 · PR 60개',
      },
      narration: '서버, 화면, 시나리오 자동 테스트 590개가 코드를 지킵니다. 요구사항부터 리뷰까지 AI 페어 프로그래밍으로 12일, PR 60개.',
    },
    {
      id: 'outro',
      kind: 'card',
      duration: 4000,
      card: {
        tone: 'light',
        image: { src: '/brand/logo.png', width: 520 },
      },
      narration: '연결이.',
    },
  ],
};
