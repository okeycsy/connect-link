import { screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { TestPlanListPage } from './TestPlanListPage';

const accountInfo = {
  code: 'ACCOUNT_INFO',
  slug: 'account-info',
  label: '계좌통합관리',
  shortLabel: '계통',
};

function user(overrides: Record<string, unknown>) {
  return {
    email: 'hong.sungmin@kftc.example.com',
    name: '홍성민',
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
  email: 'kim.minjun@kb.example.com',
  name: '김민준',
  organizationCode: '004',
  organizationName: 'KB국민은행',
  organizationType: 'FINANCE_ORGANIZATION',
});

/** 서버는 결제원 담당자에게 전체 수행률·기관 수를, 기관 담당자에게 자기 기관 수행률·완료 요구사항을 준다. */
function row(manager: boolean) {
  return {
    id: 5,
    business: 'ACCOUNT_INFO',
    businessLabel: '계좌통합관리',
    subBusiness: '본인계좌 일괄지급정지',
    title: '본인계좌 일괄지급정지 전문 개선 2차',
    startDate: '2026-10-12',
    endDate: '2026-10-16',
    state: 'IN_PROGRESS',
    stateLabel: '진행 중',
    overdue: true,
    progressRate: manager ? 49 : 56,
    itemCount: 5,
    targetCount: manager ? 4 : null,
    completedTargetCount: manager ? 1 : null,
    ownCompletedCount: manager ? null : 2,
    ownLastReportedAt: manager ? null : '2026-10-18T04:00:00',
    ownerName: '홍성민',
  };
}

// vi.mock 은 파일 맨 위로 끌어올려지므로 mock 안에서 쓰는 값은 vi.hoisted 로 만든다.
const state = vi.hoisted(() => ({
  me: null as unknown,
  queries: [] as Record<string, unknown>[],
}));

vi.mock('../../../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../../api/client')>();
  return {
    ...original,
    api: {
      GET: vi.fn(
        async (path: string, options?: { params?: { query?: Record<string, unknown> } }) => {
          const ok = (data: unknown) => ({ data, response: new Response() });
          const manager = (state.me as { organizationType: string }).organizationType === 'KFTC';
          if (path === '/api/v1/me') return ok(state.me);
          if (path === '/api/v1/businesses') return ok([accountInfo]);
          if (path === '/api/v1/test-plans/counts') return ok({ inProgress: 3, completed: 1 });
          if (path === '/api/v1/test-plans') {
            state.queries.push(options?.params?.query ?? {});
            return ok({
              content: [row(manager)],
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1,
              last: true,
            });
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
  return renderWithProviders(<TestPlanListPage />, {
    route: '/account-info/tests',
    path: '/:business/tests',
  });
}

describe('TestPlanListPage', () => {
  it('결제원 담당자는 진행 중 목록의 전체 수행률과 완료 기관, 만들기 버튼을 본다', async () => {
    state.me = KFTC;
    renderPage();

    const table = await screen.findByTestId('test-plan-table');
    expect(
      await within(table).findByText('본인계좌 일괄지급정지 전문 개선 2차'),
    ).toBeInTheDocument();
    expect(within(table).getByText('전체 수행률')).toBeInTheDocument();
    expect(within(table).getByTestId('progress-rate')).toHaveTextContent('49%');
    expect(within(table).getByText('1/4곳')).toBeInTheDocument();
    expect(within(table).getByText('기간 지남')).toBeInTheDocument();
    expect(screen.getByText('진행 중 3')).toBeInTheDocument();
    expect(screen.getByText('수행 완료 1')).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: '기관 테스트 만들기' })).toHaveAttribute(
      'href',
      '/account-info/tests/new',
    );
    expect(state.queries[0]).toMatchObject({ business: 'account-info', state: 'IN_PROGRESS' });
  });

  it('기관 담당자는 자기 기관 수행률과 완료 요구사항을 보고 만들기 버튼은 없다', async () => {
    state.me = GANA;
    renderPage();

    const table = await screen.findByTestId('test-plan-table');
    expect(await within(table).findByText('우리 기관 수행률')).toBeInTheDocument();
    expect(within(table).getByTestId('progress-rate')).toHaveTextContent('56%');
    expect(within(table).getByText('2/5개')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '기관 테스트 만들기' })).not.toBeInTheDocument();
  });
});
