import type { Chapter } from './types.ts';

/** 문제 제기 → 소개 → 로그인·홈. 심사 기준의 "업무 관련성"과 "확산 가능성"에 답한다. */
export const intro: Chapter = {
  id: 'intro',
  title: '문제와 소개',
  scenes: [
    {
      id: 'p3',
      kind: 'card',
      // 첫 장면. 숫자 하나로 시작해 내레이션이 읽힐 만큼 머문다(첫 녹화에 붙는 1~2초 지연을 뺀 값).
      duration: 8800,
      card: {
        tone: 'light',
        kicker: '업무 하나에서만, 해마다',
        kickerSize: 44,
        stat: {
          value: 198,
          prefix: '약',
          unit: '시간',
          label: '메일을 하나하나 열고 이를 내부망에서 취합하는 데 걸리는 시간',
        },
      },
      narration: '업무 하나에서만, 해마다 약 198시간. 메일을 하나하나 열어 내부망에서 취합하는 데 걸리는 시간입니다.',
    },
    {
      id: 'intro',
      kind: 'card',
      duration: 4600,
      card: {
        tone: 'light',
        image: { src: '/brand/logo-mascot.png', width: 640 },
        title: '우리 원과 참가기관을 잇는 하나의 포털',
        titleSize: 46,
      },
      narration: '우리 원과 참가기관을 잇는 하나의 포털, 연결이입니다.',
    },
    {
      id: 'login-home',
      kind: 'app',
      summary:
        '로그인 화면 → 데모 계정 "홍성민(결제원·현업)" 강조·클릭 → 홈에 "결제원 · 홍성민 과장 · 결제원 담당자" 알약(이후 결제원 시점은 갈색, 기관 시점은 파랑) → 홈의 할 일 카드 4개를 모두 강조 → 왼쪽 메뉴(연락처·의견 요청·기관 테스트·일시중단·소통 채널) 강조',
      run: async (d, page) => {
        await page.goto('/login');
        const demo = page.getByTestId('demo-accounts');
        await demo.waitFor();
        await d.pause(300);
        d.say('결제원 담당자가 로그인하면 지금 할 일만 보입니다.');
        const hong = page.getByRole('button', { name: /^홍성민 .*로 로그인/ });
        await d.focus(hong, { note: '결제원도, 참가기관도 같은 입구. <b>결제원 담당자 홍성민</b>으로 로그인합니다.', scale: 1.3, hold: 500 });
        await d.click(hong);
        await page.waitForURL(/\/home$/);
        await page.getByRole('heading', { name: /안녕하세요/ }).waitFor();
        await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
        await d.pause(400);
        const cards = page.locator('[data-testid^="home-card-"]');
        const cardCount = await cards.count();
        await d.focus(Array.from({ length: cardCount }, (_, i) => cards.nth(i)), {
          note: '로그인하면 <b>지금 할 일</b>만 보입니다.',
          scale: 1.08,
          hold: 850,
        });
        const nav = page.getByRole('navigation');
        await d.moveTo(nav.getByRole('link', { name: '일시중단', exact: true }), { steps: 10 });
        d.say('어느 업무든 같은 구조입니다.');
        await d.focus(nav, { note: '어느 업무든 <b>같은 구조</b>. 94개 업무까지 그대로.', scale: 1, hold: 1500 });
        await d.pause(150);
      },
    },
  ],
};
