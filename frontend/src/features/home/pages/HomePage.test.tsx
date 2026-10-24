import { screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { HomePage } from './HomePage';

// 다가오는 일시중단은 오늘을 기준으로 묶는다. 오늘을 토요일로 고정한다.
const TODAY = '2026-10-24';

const home = {
  business: 'ACCOUNT_INFO',
  businessLabel: '계좌통합관리',
  userName: '데모 관리자',
  cards: [
    {
      key: 'OPINION_REPLIES',
      title: '회신 대기 의견 요청',
      source: 'SAMPLE',
      urgency: 'SOON',
      summary: null,
      metrics: [{ label: '회신중', value: 2, unit: '건' }],
      progress: { done: 12, total: 18, label: '기관 회신' },
      items: [
        {
          title: '야간 배치 시간 조정',
          subtitle: '회신 12/18 기관',
          dueAt: '2026-10-28',
          urgency: 'NONE',
          link: '/account-info/opinions/1',
        },
      ],
      link: '/account-info/opinions',
    },
    {
      key: 'SUSPENSIONS',
      title: '다가오는 일시중단',
      source: 'REAL',
      urgency: 'SOON',
      summary: '지금 진행 중인 일시중단이 1건 있습니다.',
      metrics: [{ label: '진행 중', value: 1, unit: '건' }],
      progress: null,
      items: [
        {
          title: '우리은행',
          subtitle: '22:00 ~ 10/24 12:00 · 전산 이전 · 확정',
          dueAt: '2026-10-23',
          urgency: 'SOON',
          link: '/account-info/suspensions?id=1',
        },
        {
          title: '신한은행',
          subtitle: '23:00 ~ 10/25 06:00 · 코어뱅킹 점검 · 확정',
          dueAt: TODAY,
          urgency: 'SOON',
          link: '/account-info/suspensions?id=2',
        },
        {
          title: 'KB국민은행',
          subtitle: '01:00 ~ 05:00 · 정기 점검 · 검토 대기',
          dueAt: '2026-10-25',
          urgency: 'NONE',
          link: '/account-info/suspensions?id=3',
        },
        {
          title: '하나은행',
          subtitle: '00:00 ~ 02:00 · DB 작업 · 확정',
          dueAt: '2026-10-27',
          urgency: 'NONE',
          link: '/account-info/suspensions?id=4',
        },
      ],
      link: '/account-info/suspensions',
    },
  ],
};

const RESPONSES: Record<string, unknown> = {
  '/api/v1/home': home,
};

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async (path: string) => ({ data: RESPONSES[path], response: new Response() })),
  },
  unwrap: (r: { data: unknown }) => r.data,
}));

describe('HomePage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(`${TODAY}T10:00:00+09:00`));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('건수 요약 없이 인사말과 카드를 보인다', async () => {
    renderWithProviders(<HomePage />);

    expect(await screen.findByText(/· 계좌통합관리$/)).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { level: 1, name: '안녕하세요, 데모 관리자 님' }),
    ).toBeInTheDocument();
    expect(screen.getByText('지금 할 일을 모았습니다.')).toBeInTheDocument();
    // 건수 요약(KPI 타일)은 없다.
    expect(screen.queryByText('확인 필요 담당자')).not.toBeInTheDocument();

    expect(screen.getByTestId('home-card-OPINION_REPLIES')).toBeInTheDocument();

    // 최근 알림은 홈에 두지 않는다(알림 벨에서 본다).
    expect(screen.queryByText('최근 소식')).not.toBeInTheDocument();
  });

  it('다가오는 일시중단은 진행 중 건을 맨 위에, 그다음 오늘부터 날짜별로 묶고 주말을 표시한다', async () => {
    renderWithProviders(<HomePage />);

    const list = await screen.findByTestId('suspension-upcoming');
    const days = within(list)
      .getAllByTestId(/^suspension-day-/)
      .map((d) => d.dataset.testid);
    expect(days).toEqual([
      'suspension-day-ongoing',
      `suspension-day-${TODAY}`,
      'suspension-day-2026-10-25',
      'suspension-day-2026-10-27',
    ]);
    expect(screen.getByTestId('suspension-day-ongoing')).toHaveTextContent('지금 진행 중');
    expect(screen.getByTestId('suspension-day-ongoing')).toHaveTextContent('우리은행');
    expect(screen.getByTestId(`suspension-day-${TODAY}`)).toHaveTextContent(
      '오늘 · 10월 24일(토)주말',
    );
    expect(screen.getByTestId('suspension-day-2026-10-25')).toHaveTextContent(
      '내일 · 10월 25일(일)주말',
    );
    expect(screen.getByTestId('suspension-day-2026-10-27')).toHaveTextContent('10월 27일(화)');
    expect(screen.getByTestId('suspension-day-2026-10-27')).not.toHaveTextContent('주말');
  });

  it('할 일이 있는 카드가 없으면 빈 안내를 보인다', async () => {
    RESPONSES['/api/v1/home'] = { ...home, cards: [] };
    renderWithProviders(<HomePage />);

    expect(await screen.findByText('지금 할 일이 없습니다')).toBeInTheDocument();
    expect(screen.queryByTestId('home-cards')).not.toBeInTheDocument();
    RESPONSES['/api/v1/home'] = home;
  });
});
