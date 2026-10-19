import { screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { OpinionListPage } from './OpinionListPage';

const accountInfo = {
  code: 'ACCOUNT_INFO',
  slug: 'account-info',
  label: '계좌통합관리',
  shortLabel: '계통',
};

function user(overrides: Record<string, unknown>) {
  return {
    email: 'hong.gildong@kftc.example.com',
    name: '홍길동',
    organizationCode: 'KFTC',
    organizationName: '금융결제원',
    organizationType: 'KFTC',
    organizationTypeLabel: '결제원',
    admin: false,
    businesses: [accountInfo],
    jobRole: 'BUSINESS',
    jobRoleLabel: '현업',
    itRoles: [],
    roleLabel: '결제원 · 현업',
    networkZone: 'INTERNAL',
    networkZoneLabel: '내부망',
    ...overrides,
  };
}

const KFTC = user({});
const GANA = user({
  email: 'kim.gana@kb.example.com',
  name: '김가나',
  organizationCode: '004',
  organizationName: 'KB국민은행',
  organizationType: 'FINANCE_ORGANIZATION',
});

function summary(overrides: Record<string, unknown>) {
  return {
    id: 1,
    title: '수수료 체계 개편 의견 요청',
    status: 'OPEN',
    statusLabel: '회신중',
    cancelled: false,
    dueDate: '2026-10-20',
    daysLeft: 2,
    ownerName: '홍길동',
    targetCount: 6,
    repliedCount: 4,
    myReplyStatus: null,
    myReplyStatusLabel: null,
    openedAt: '2026-10-10T09:00:00',
    updatedAt: '2026-10-15T09:00:00',
    ...overrides,
  };
}

// vi.mock 은 파일 맨 위로 끌어올려지므로 mock 안에서 쓰는 값은 vi.hoisted 로 만든다.
const state = vi.hoisted(() => ({
  me: null as unknown,
  rows: [] as unknown[],
  queries: [] as Record<string, unknown>[],
}));

vi.mock('../../../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../../api/client')>();
  return {
    ...original,
    api: {
      GET: vi.fn(
        async (path: string, options?: { params?: { query?: Record<string, unknown> } }) => {
          if (path === '/api/v1/me') return { data: state.me, response: new Response() };
          if (path === '/api/v1/businesses')
            return { data: [accountInfo], response: new Response() };
          if (path === '/api/v1/opinion-requests/counts') {
            return { data: { draft: 1, open: 1, closed: 0 }, response: new Response() };
          }
          if (path === '/api/v1/opinion-requests') {
            state.queries.push(options?.params?.query ?? {});
            const content = state.rows;
            return {
              data: {
                content,
                page: 0,
                size: 20,
                totalElements: content.length,
                totalPages: 1,
                last: true,
              },
              response: new Response(),
            };
          }
          throw new Error(`unexpected GET ${path}`);
        },
      ),
    },
  };
});

beforeEach(() => {
  state.queries.length = 0;
});

function renderPage() {
  return renderWithProviders(<OpinionListPage />, {
    route: '/account-info/opinions',
    path: '/:business/opinions',
  });
}

describe('OpinionListPage', () => {
  it('결제원 담당자는 작성중 칩과 새 요청 버튼, 기관별 회신 수를 본다', async () => {
    state.me = KFTC;
    state.rows = [summary({})];
    renderPage();

    const table = await screen.findByTestId('opinion-table');
    expect(await within(table).findByText('4/6곳')).toBeInTheDocument();
    expect(within(table).getByText('D-2')).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: '새 의견 요청' })).toHaveAttribute(
      'href',
      '/account-info/opinions/new',
    );
    expect(screen.getByText('작성중 1')).toBeInTheDocument();
    expect(state.queries[0]).toMatchObject({ business: 'account-info', status: 'OPEN' });
  });

  it('기관 담당자는 우리 기관 회신 상태를 보고, 작성중 칩과 새 요청 버튼은 없다', async () => {
    state.me = GANA;
    state.rows = [summary({ targetCount: null, repliedCount: null, myReplyStatus: 'DRAFT' })];
    renderPage();

    const table = await screen.findByTestId('opinion-table');
    expect(await within(table).findByText('임시저장')).toBeInTheDocument();
    expect(within(table).getByText('우리 기관 회신')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '새 의견 요청' })).not.toBeInTheDocument();
    expect(screen.queryByText(/작성중/)).not.toBeInTheDocument();
  });
});
