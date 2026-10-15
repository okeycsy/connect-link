import { screen } from '@testing-library/react';
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

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async () => ({ data: home, response: new Response() })),
  },
  unwrap: (r: { data: unknown }) => r.data,
}));

describe('HomePage', () => {
  it('업무 영역, 인사말, KPI 와 카드를 보여 주고 가장 급한 카드를 크게 둔다', async () => {
    renderWithProviders(<HomePage />);

    expect(await screen.findByText('계좌통합관리')).toBeInTheDocument();
    expect(
      screen.getByText('안녕하세요, 데모 관리자 님. 지금 할 일을 모았습니다.'),
    ).toBeInTheDocument();
    expect(screen.getAllByText('확인 필요 담당자').length).toBeGreaterThan(0);

    const featured = screen.getByTestId('home-card-CONTACT_CONFIRM');
    expect(featured).toHaveAttribute('data-featured');
    expect(screen.getByTestId('home-card-OPINION_REPLIES')).not.toHaveAttribute('data-featured');
    expect(screen.getByText('가나은행 · 김가나 (현업)')).toBeInTheDocument();
    expect(screen.getByText('초과')).toBeInTheDocument();
    expect(
      screen.getByText('해당 기능 프로젝트가 이 카드를 연결하면 여기에 보입니다.'),
    ).toBeInTheDocument();
  });
});
