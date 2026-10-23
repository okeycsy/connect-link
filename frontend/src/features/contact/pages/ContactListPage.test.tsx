import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { ContactListPage } from './ContactListPage';

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

const HONG = user({});
const GANA = user({
  email: 'kim.minjun@kb.example.com',
  name: '김민준',
  organizationCode: '004',
  organizationName: 'KB국민은행',
  organizationType: 'FINANCE_ORGANIZATION',
  networkZone: 'EXTERNAL',
  networkZoneLabel: '인터넷망',
});

function contact(overrides: Record<string, unknown>) {
  return {
    id: 1,
    business: 'ACCOUNT_INFO',
    businessLabel: '계좌통합관리',
    organizationId: 1,
    organizationCode: 'KFTC',
    organizationName: '금융결제원',
    organizationType: 'KFTC',
    subBusiness: '계좌정보 조회·해지',
    name: '홍성민',
    department: '계좌서비스부',
    team: '계좌통합팀',
    jobTitle: '과장',
    jobRole: 'BUSINESS',
    jobRoleLabel: '현업',
    itRoles: [],
    rank: 'PRIMARY',
    rankLabel: '주담당',
    extensionPhone: '1101',
    mobilePhone: '01000001101',
    email: 'hong.sungmin@kftc.example.com',
    lastConfirmedAt: '2026-09-29T09:00:00',
    lastConfirmedByName: '홍성민',
    needsConfirm: false,
    unreachable: null,
    self: false,
    permissions: { edit: false, delete: false, markUnreachable: false, clearUnreachable: false },
    ...overrides,
  };
}

const ROWS = [
  contact({}),
  contact({
    id: 3,
    organizationId: 3,
    organizationCode: '004',
    organizationName: 'KB국민은행',
    organizationType: 'FINANCE_ORGANIZATION',
    name: '김민준',
    department: '디지털금융부',
    mobilePhone: '010-0000-2101',
    lastConfirmedAt: '2026-04-01T09:00:00',
    needsConfirm: true,
    self: true,
    permissions: { edit: true, delete: false, markUnreachable: true, clearUnreachable: false },
  }),
  contact({
    id: 4,
    organizationId: 3,
    organizationCode: '004',
    organizationName: 'KB국민은행',
    organizationType: 'FINANCE_ORGANIZATION',
    name: '남궁민',
    jobRole: 'IT',
    jobRoleLabel: '전산',
    mobilePhone: null,
    unreachable: {
      markedAt: '2026-10-23T02:10:00',
      note: '내선·휴대폰 모두 받지 않음',
      markedByName: '이수진',
      markedByJobTitle: '주임',
    },
    permissions: { edit: true, delete: false, markUnreachable: true, clearUnreachable: true },
  }),
];

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
        async (
          path: string,
          options?: { params?: { query?: Record<string, unknown>; path?: { id: number } } },
        ) => {
          const ok = (data: unknown) => ({ data, response: new Response() });
          if (path === '/api/v1/me') return ok(state.me);
          if (path === '/api/v1/businesses') return ok([accountInfo]);
          if (path === '/api/v1/job-roles') return ok([]);
          if (path === '/api/v1/contacts/counts')
            return ok({ total: 3, needsConfirm: 1, unreachable: 1 });
          if (path === '/api/v1/contacts/organizations') {
            return ok([
              { id: 1, code: 'KFTC', name: '금융결제원', type: 'KFTC' },
              { id: 3, code: '004', name: 'KB국민은행', type: 'FINANCE_ORGANIZATION' },
              { id: 5, code: '088', name: '신한은행', type: 'FINANCE_ORGANIZATION' },
            ]);
          }
          if (path === '/api/v1/update-campaigns/current') {
            return ok({
              campaign: {
                id: 9,
                business: 'ACCOUNT_INFO',
                businessLabel: '계좌통합관리',
                title: '하반기 담당자 정기 변경',
                startDate: '2026-10-16',
                endDate: '2026-10-23',
                status: 'OPEN',
                statusLabel: '진행 중',
                registrantName: '홍성민',
                createdAt: '2026-10-14T09:00:00',
                targetCount: 6,
                completedCount: 2,
                changedCount: 2,
                progressRate: 33,
              },
              ownOrganization: null,
            });
          }
          if (path === '/api/v1/contacts/{id}') {
            return ok(ROWS.find((r) => r.id === options?.params?.path?.id));
          }
          if (path === '/api/v1/change-histories') {
            return ok({
              content: [],
              page: 0,
              size: 1,
              totalElements: 0,
              totalPages: 0,
              last: true,
            });
          }
          if (path === '/api/v1/contacts') {
            state.queries.push(options?.params?.query ?? {});
            return ok({
              content: ROWS,
              page: 0,
              size: 50,
              totalElements: 3,
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

function renderPage(route = '/account-info/contacts') {
  return renderWithProviders(<ContactListPage />, { route, path: '/:business/contacts' });
}

describe('ContactListPage', () => {
  it('기관별로 묶어 보이고 6개월 경과 담당자에 확인 필요를 붙인다', async () => {
    state.me = HONG;
    renderPage();

    const table = await screen.findByTestId('contact-table');
    expect(await within(table).findByText('김민준')).toBeInTheDocument();
    const groups = within(table).getAllByTestId('contact-group');
    expect(groups.map((g) => g.textContent)).toEqual([
      expect.stringContaining('금융결제원'),
      expect.stringContaining('KB국민은행'),
    ]);
    expect(groups[1]).toHaveTextContent('2명');
    expect(within(table).getByText('010-0000-1101')).toBeInTheDocument();
    expect(within(table).getByText('확인 필요')).toBeInTheDocument();
    expect(screen.getByTestId('contact-needs-confirm')).toHaveTextContent('확인 필요 1명');
  });

  it('결제원 담당자는 내부망에서 검색·필터·엑셀·등록과 정기 변경 진행률을 본다', async () => {
    state.me = HONG;
    renderPage();

    await screen.findByTestId('contact-table');
    expect(screen.getByRole('button', { name: '엑셀' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '담당자 등록' })).toBeInTheDocument();
    const banner = await screen.findByTestId('campaign-banner');
    expect(banner).toHaveTextContent('하반기 담당자 정기 변경');
    expect(banner).toHaveTextContent('완료 2 / 대상 6개 기관');
    expect(within(banner).getByRole('link', { name: '진행 현황 보기' })).toHaveAttribute(
      'href',
      '/account-info/contacts/update-campaigns?id=9',
    );

    fireEvent.change(screen.getByRole('textbox', { name: '담당자 검색' }), {
      target: { value: '김가' },
    });
    await waitFor(() => expect(state.queries.at(-1)).toMatchObject({ q: '김가' }));
  });

  it('인터넷망의 기관 담당자는 검색·필터 없이 목록을 보고, 거르기 조건을 보내지 않는다', async () => {
    state.me = GANA;
    renderPage();

    await screen.findByTestId('contact-table');
    expect(screen.getByTestId('contact-filter-internal-only')).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: '담당자 검색' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '엑셀' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '담당자 등록' })).toBeInTheDocument();
    expect(Object.keys(state.queries[0]).sort()).toEqual(['business', 'page', 'size']);
  });

  it('연락 불가 담당자에 빨간 배지를 붙이고, 행 끝 버튼으로 연락 불가 창을 연다', async () => {
    state.me = HONG;
    renderPage();

    const table = await screen.findByTestId('contact-table');
    await within(table).findByText('남궁민');
    expect(within(table).getAllByTestId('contact-unreachable-badge')).toHaveLength(1);
    expect(screen.getByTestId('contact-unreachable')).toHaveTextContent('연락 불가 1명');
    expect(within(table).queryByRole('button', { name: '홍성민 연락 불가 표시' })).toBeNull();

    fireEvent.click(within(table).getByRole('button', { name: '김민준 연락 불가 표시' }));
    expect(await screen.findByRole('button', { name: '연락 불가로 표시' })).toBeInTheDocument();
    expect(
      screen.getByLabelText(/KB국민은행 계좌통합관리 담당자에게도 메일로 알리기/),
    ).not.toBeChecked();
  });

  it('상세 패널은 연락 불가 표시와 남긴 사람을 맨 위에 보이고 해제 버튼을 둔다', async () => {
    state.me = GANA;
    renderPage('/account-info/contacts?id=4');

    const alert = await screen.findByTestId('contact-unreachable-alert');
    expect(alert).toHaveTextContent(
      '2026-10-23 02:10 결제원(이수진 주임) · 내선·휴대폰 모두 받지 않음',
    );
    expect(within(alert).getByRole('button', { name: '연락됨 · 해제' })).toBeInTheDocument();
  });

  it('주소의 id 로 상세 패널을 열고 권한대로 버튼을 보인다', async () => {
    state.me = GANA;
    renderPage('/account-info/contacts?id=4');

    const detail = await screen.findByTestId('contact-detail');
    expect(detail).toHaveTextContent('전산');
    expect(await screen.findByRole('button', { name: '수정' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '변동 없음' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
    expect(await screen.findByText('고친 적이 없습니다.')).toBeInTheDocument();
  });
});
