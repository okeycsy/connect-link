import { screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { SuspensionListPage } from './SuspensionListPage';

const cms = { code: 'CMS', slug: 'cms', label: 'CMS', shortLabel: 'CMS' };

function user(overrides: Record<string, unknown>) {
  return {
    email: 'hong.gildong@kftc.example.com',
    name: '홍길동',
    organizationCode: 'KFTC',
    organizationName: '금융결제원',
    organizationType: 'KFTC',
    organizationTypeLabel: '결제원',
    admin: false,
    businesses: [cms],
    jobRole: 'IT',
    jobRoleLabel: '전산',
    itRoles: ['OPS'],
    roleLabel: '결제원 · 전산',
    networkZone: 'INTERNAL',
    networkZoneLabel: '내부망',
    ...overrides,
  };
}

const KFTC = user({});
const DARA = user({
  email: 'park.dara@dara.example.com',
  name: '박다라',
  organizationCode: 'DARA',
  organizationName: '다라은행',
  organizationType: 'FINANCE_ORGANIZATION',
});

const ROW = {
  id: 7,
  business: 'CMS',
  businessLabel: 'CMS',
  subBusiness: '출금이체',
  organizationId: 3,
  organizationName: '다라은행',
  startAt: '2026-10-17T22:00:00',
  endAt: '2026-10-17T23:30:00',
  period: 'CURRENT',
  periodLabel: '진행 중',
  status: 'IN_REVIEW',
  statusLabel: '검토 중',
  reason: '전산 시스템 정기 점검',
  registrantName: '박다라',
  registrantKftc: false,
  reviewerCount: 1,
};

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
          if (path === '/api/v1/me') return ok(state.me);
          if (path === '/api/v1/businesses') return ok([cms]);
          if (path === '/api/v1/suspensions/counts') {
            return ok({ current: 1, upcoming: 3, past: 5, pendingReview: 2 });
          }
          if (path === '/api/v1/suspensions/organizations') {
            return ok([{ id: 3, code: 'DARA', name: '다라은행' }]);
          }
          if (path === '/api/v1/suspensions') {
            state.queries.push(options?.params?.query ?? {});
            return ok({
              content: [ROW],
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
  return renderWithProviders(<SuspensionListPage />, {
    route: '/cms/suspensions',
    path: '/:business/suspensions',
  });
}

describe('SuspensionListPage', () => {
  it('결제원 담당자는 진행 중 목록, 확정 전 건수, 기관 고르기, 취합 보내기와 등록 버튼을 본다', async () => {
    state.me = KFTC;
    renderPage();

    const table = await screen.findByTestId('suspension-table');
    expect(await within(table).findByText('다라은행')).toBeInTheDocument();
    expect(within(table).getByText('2026-10-17 22:00 ~ 23:30')).toBeInTheDocument();
    expect(within(table).getByText('검토 중')).toBeInTheDocument();
    expect(within(table).getByText('중단 중')).toBeInTheDocument();
    expect(await screen.findByTestId('pending-review-count')).toHaveTextContent('확정 전 2건');
    expect(screen.getByText('예정 3')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: '기관' })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: '수동으로 보내기' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '일시중단 등록' })).toHaveAttribute(
      'href',
      '/cms/suspensions/new',
    );
    expect(state.queries[0]).toMatchObject({ business: 'cms', period: 'CURRENT' });
  });

  it('기관 담당자는 등록 버튼만 보고 기관 고르기, 확정 전 건수, 취합 보내기는 없다', async () => {
    state.me = DARA;
    renderPage();

    await screen.findByTestId('suspension-table');
    expect(await screen.findByRole('link', { name: '일시중단 등록' })).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: '기관' })).not.toBeInTheDocument();
    expect(screen.queryByTestId('pending-review-count')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '수동으로 보내기' })).not.toBeInTheDocument();
  });
});
