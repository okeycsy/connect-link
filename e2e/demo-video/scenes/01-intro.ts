import type { Chapter } from './types.ts';

/** 문제 제기 → 소개 → 로그인·홈. 심사 기준의 "업무 관련성"과 "확산 가능성"에 답한다. */
export const intro: Chapter = {
  id: 'intro',
  title: '문제와 소개',
  scenes: [
    {
      id: 'p1',
      kind: 'card',
      duration: 3400,
      card: {
        tone: 'light',
        kicker: '결제원 업무의 중심',
        title: '참가기관과의 소통과 취합,\n우리 원 업무의 핵심입니다.',
        subtitle: '담당자 변경 · 의견 회신 · 기관 테스트 · 일시중단 일정, 매일 수십 번',
      },
      narration:
        '참가기관과의 소통과 취합은 우리 원 업무의 핵심입니다. 담당자 변경, 의견 회신, 기관 테스트, 일시중단 일정까지, 매일 수십 번 오갑니다.',
    },
    {
      id: 'p2',
      kind: 'card',
      duration: 4000,
      card: {
        tone: 'light',
        title: '수십 개의 기관에 전화해서\n회신을 독촉하고, 메일로 받고,\n내부망으로 옮겨서 취합합니다.',
      },
      narration: '수십 개의 기관에 전화해서 회신을 독촉하고, 메일로 받고, 내부망으로 옮겨서 취합합니다.',
    },
    {
      id: 'p3',
      kind: 'card',
      duration: 4100,
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
      narration: '업무 하나에서만, 해마다 약 198시간이 메일을 하나하나 열고 내부망에서 취합하는 데 들어갑니다.',
    },
    {
      id: 'intro',
      kind: 'card',
      duration: 3800,
      card: {
        tone: 'light',
        image: { src: '/brand/logo-mascot.png', width: 430 },
        title: '우리 원과 참가기관을 잇는\n하나의 포털',
      },
      narration: '그래서 하나의 포털을 만들었습니다. 연결이입니다.',
    },
    {
      id: 'login-home',
      kind: 'app',
      summary:
        '로그인 화면의 회사 이메일 칸·로그인 버튼 강조 → 데모 계정 "홍성민(결제원·현업)" 강조·클릭 → 홈에 "결제원 · 홍성민 과장 · 결제원 담당자" 알약(이후 결제원 시점은 갈색, 기관 시점은 파랑) → 홈의 할 일 카드 4개를 모두 강조 → 왼쪽 메뉴(연락처·의견 요청·기관 테스트·일시중단·소통 채널)를 비추며 커서 이동',
      narration:
        '결제원 담당자 홍성민이 회사 이메일로 로그인합니다. 로그인하면 지금 할 일만 보입니다. 어느 업무든 같은 메뉴, 같은 구조라 94개 업무로 그대로 넓힐 수 있습니다.',
      run: async (d, page) => {
        await page.goto('/login');
        const demo = page.getByTestId('demo-accounts');
        await demo.waitFor();
        await d.pause(500);
        await d.focus([page.getByLabel('회사 이메일'), page.getByRole('button', { name: '로그인', exact: true })], {
          note: '결제원도, 참가기관도 같은 입구. <b>회사 이메일 하나</b>로.',
          scale: 1.2,
          hold: 1100,
        });
        const hong = page.getByRole('button', { name: /^홍성민 .*로 로그인/ });
        await d.focus(hong, { note: '<b>결제원 담당자 홍성민</b>으로 로그인합니다.', scale: 1.3, hold: 700 });
        await d.click(hong);
        await page.waitForURL(/\/home$/);
        await page.getByRole('heading', { name: /안녕하세요/ }).waitFor();
        await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
        await d.pause(400);
        const cards = page.locator('[data-testid^="home-card-"]');
        const cardCount = await cards.count();
        await d.focus(Array.from({ length: cardCount }, (_, i) => cards.nth(i)), {
          note: '로그인하면 <b>지금 할 일</b>만 보입니다. 네 칸이 모두 할 일.',
          scale: 1.08,
          hold: 1300,
        });
        const nav = page.getByRole('navigation');
        await d.focus(nav, { note: '어느 업무든 <b>같은 구조</b>. 94개 업무까지 그대로.', scale: 1 });
        for (const name of ['연락처', '의견 요청', '기관 테스트', '일시중단', '소통 채널']) {
          await d.moveTo(nav.getByRole('link', { name, exact: true }), { steps: 8 });
          await d.pause(120);
        }
        await d.pause(400);
        await d.unfocus();
        await d.pause(200);
      },
    },
  ],
};
