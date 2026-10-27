import type { Chapter } from './types.ts';

/** 기대효과 → 플랫폼화 → AI 활용 → 마무리. 심사 기준의 "완성도"와 "창의성·AI 활용"에 답한다. */
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
        kicker: '업무 하나에 해마다 305시간 · 사업 계층별 분류체계 소분류 94개 업무로 넓히면',
        stat: { value: 28670, unit: '시간 절감', label: '해마다, 우리 원 전체에서' },
      },
      narration: '업무 하나에 해마다 305시간. 소분류 94개 업무로 넓히면, 해마다 2만 8천 670시간을 절감합니다.',
    },
    {
      id: 'platform',
      kind: 'card',
      duration: 4200,
      card: {
        tone: 'light',
        kicker: '수기에서 플랫폼으로',
        title: '수기로 처리하던 소통을\n하나의 플랫폼으로.',
        subtitle: '표준 양식 · 자동 집계 · 변경 이력 · 알림 · 설치 없이 바로 도입',
      },
      narration:
        '수기와 메일로 처리하던 소통을 하나의 플랫폼으로 옮겼습니다. 표준 양식, 자동 집계, 변경 이력, 알림까지. 설치 없이 바로 도입할 수 있습니다.',
    },
    {
      id: 'ai',
      kind: 'card',
      duration: 4000,
      card: {
        tone: 'light',
        title: 'Claude와 함께 만들었습니다.',
        subtitle: '요구사항 → 설계 → 코드 → 테스트 → 리뷰 · 3명 · 12일 · PR 60개',
      },
      narration: '요구사항 정리부터 설계, 코드, 테스트, 리뷰까지 Claude와 함께, 3명이 12일에 만들었습니다.',
    },
    {
      id: 'outro',
      kind: 'card',
      duration: 4600,
      card: {
        tone: 'light',
        image: { src: '/brand/logo.png', width: 340 },
        title: '더 긴밀한 소통,\n더 빠른 대응.',
        subtitle: '결제원과 참가기관을 잇는 연결이',
      },
      narration: '연결이. 결제원과 참가기관의 소통을 더 긴밀하게, 대응을 더 빠르게.',
    },
  ],
};
