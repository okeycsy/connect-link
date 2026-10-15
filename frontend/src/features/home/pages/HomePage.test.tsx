import { screen } from '@testing-library/react';
import dayjs from 'dayjs';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { HomePage } from './HomePage';

const home = {
  business: 'ACCOUNT_INFO',
  businessLabel: '계좌통합관리',
  userName: '데모 관리자',
  kpis: [
    { label: '확인 필요 담당자', value: 7, unit: '명', urgent: true },
    { label: '회신 대기 의견 요청', value: 2, unit: '건', urgent: false },
  ],
  cards: [
    {
      key: 'CONTACT_CONFIRM',
      title: '확인이 필요한 담당자',
      source: 'SAMPLE',
      urgency: 'OVERDUE',
      summary: '6개월을 넘긴 담당자',
      metrics: [{ label: '6개월 초과', value: 7, unit: '명' }],
      progress: null,
      items: [
        {
          title: '가나은행 · 김가나 (현업)',
          subtitle: '최근 확인 2026-02-02',
          dueAt: null,
          urgency: 'OVERDUE',
          link: '/account-info/contacts',
        },
      ],
      link: '/account-info/contacts',
    },
    {
      key: 'OPINION_REPLIES',
      title: '회신 대기 의견 요청',
      source: 'SAMPLE',
      urgency: 'SOON',
      summary: null,
      metrics: [{ label: '회신중', value: 2, unit: '건' }],
      progress: { done: 12, total: 18, label: '기관 회신' },
      items: [],
      link: '/account-info/opinions',
    },
    {
      key: 'SUSPENSIONS',
      title: '이번 주 일시중단',
      source: 'SAMPLE',
      urgency: 'SOON',
      summary: '오늘 일시중단이 1건 있습니다.',
      metrics: [{ label: '오늘', value: 1, unit: '건' }],
      progress: null,
      items: [
        {
          title: '다라은행 23:00',
          subtitle: '코어뱅킹 점검 · 확정',
          dueAt: dayjs().format('YYYY-MM-DD'),
          urgency: 'SOON',
          link: '/account-info/suspensions',
        },
      ],
      link: '/account-info/suspensions',
    },
    {
      key: 'TEST_PROGRESS',
      title: '진행 중 기관 테스트',
      source: 'UNAVAILABLE',
      urgency: 'NONE',
      summary: null,
      metrics: [],
      progress: null,
      items: [],
      link: '/account-info/tests',
    },
  ],
};

const notifications = {
  content: [
    {
      id: 1,
      type: 'OPINION_REQUEST_OPENED',
      typeLabel: '의견 요청',
      businessLabel: '계좌통합관리',
      title: '수수료 체계 개편 의견 요청이 등록되었습니다',
      body: null,
      link: null,
      read: false,
      createdAt: '2026-10-15T09:00:00',
    },
  ],
  page: 0,
  size: 6,
  totalElements: 1,
  totalPages: 1,
};

const RESPONSES: Record<string, unknown> = {
  '/api/v1/home': home,
  '/api/v1/notifications': notifications,
  '/api/v1/notifications/unread-count': { count: 1 },
};

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async (path: string) => ({ data: RESPONSES[path], response: new Response() })),
  },
  unwrap: (r: { data: unknown }) => r.data,
}));

describe('HomePage', () => {
  it('업무 영역, 인사말, KPI, 최근 소식, 이번 주 일시중단을 보여 주고 확인 필요 담당자는 작게 둔다', async () => {
    renderWithProviders(<HomePage />);

    expect(await screen.findByText(/· 계좌통합관리$/)).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { level: 1, name: '안녕하세요, 데모 관리자 님' }),
    ).toBeInTheDocument();
    expect(screen.getByText('지금 할 일을 모았습니다.')).toBeInTheDocument();
    expect(screen.getAllByText('확인 필요 담당자').length).toBeGreaterThan(0);

    // 확인이 필요한 담당자: 한 줄 띠로 작게, 항목 목록은 보이지 않는다.
    expect(screen.getByTestId('home-card-CONTACT_CONFIRM')).toHaveAttribute('data-compact');
    expect(screen.queryByText('가나은행 · 김가나 (현업)')).not.toBeInTheDocument();
    expect(screen.getByText('초과')).toBeInTheDocument();

    // 최근 소식: 알림 센터와 같은 내용
    expect(
      await screen.findByText('수수료 체계 개편 의견 요청이 등록되었습니다'),
    ).toBeInTheDocument();

    // 이번 주 일시중단: 오늘 칸에 칩
    expect(screen.getByTestId('suspension-week')).toHaveTextContent('다라은행 23:00');
    expect(
      screen.getByText('해당 기능 프로젝트가 이 카드를 연결하면 여기에 보입니다.'),
    ).toBeInTheDocument();
  });
});
