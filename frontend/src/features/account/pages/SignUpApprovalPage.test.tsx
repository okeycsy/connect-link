import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { SignUpApprovalPage } from './SignUpApprovalPage';

const businesses = [
  { code: 'ACCOUNT_INFO', slug: 'account-info', label: '계좌통합관리', shortLabel: '계통' },
  { code: 'AUTO_TRANSFER', slug: 'auto-transfer', label: '자동이체통합관리', shortLabel: '자통' },
];

function application(overrides: Record<string, unknown>) {
  return {
    id: 1,
    email: 'han.gana@gana.example.com',
    name: '한가나',
    organizationId: 2,
    organizationName: '가나은행',
    organizationType: 'FINANCE_ORGANIZATION',
    organizationTypeLabel: '참가기관',
    organizationEmailDomain: 'gana.example.com',
    emailDomainMatches: true,
    businesses: [{ ...businesses[0], subBusiness: '계좌정보 통합조회' }],
    jobRole: 'BUSINESS',
    jobRoleLabel: '현업',
    department: '디지털금융부',
    team: '계좌서비스팀',
    jobTitle: '대리',
    extensionPhone: '0200001234',
    mobilePhone: null,
    status: 'PENDING_APPROVAL',
    statusLabel: '승인 대기',
    appliedAt: '2026-10-15T10:30:00',
    reviewedAt: null,
    reviewerName: null,
    rejectionReason: null,
    ...overrides,
  };
}

const pending = [
  application({
    id: 2,
    email: 'oh.dara@mail.example.com',
    name: '오다라',
    organizationId: 3,
    organizationName: '다라은행',
    organizationEmailDomain: 'dara.example.com',
    emailDomainMatches: false,
    businesses: [
      { ...businesses[0], subBusiness: null },
      { ...businesses[1], subBusiness: '자동이체 전문 개발' },
    ],
    jobRole: 'IT',
    jobRoleLabel: '전산 · 개발',
    appliedAt: '2026-10-16T09:00:00',
  }),
  application({}),
];

const rejected = [
  application({
    id: 3,
    email: 'seo.saa@saa.example.com',
    name: '서사아',
    organizationName: '사아증권',
    status: 'REJECTED',
    statusLabel: '반려',
    reviewedAt: '2026-10-13T14:00:00',
    reviewerName: '홍길동',
    rejectionReason: '담당자 명단에서 확인되지 않습니다.',
  }),
];

const approved = [
  application({
    id: 4,
    email: 'kim.gana@gana.example.com',
    name: '김가나',
    status: 'ACTIVE',
    statusLabel: '사용 중',
    reviewedAt: '2026-09-26T10:00:00',
    reviewerName: '홍길동',
  }),
];

// vi.mock 은 파일 맨 위로 끌어올려지므로 mock 안에서 쓰는 값은 vi.hoisted 로 만든다.
const calls = vi.hoisted(() => ({
  list: [] as Record<string, unknown>[],
  post: [] as { path: string; id?: number; body?: unknown }[],
}));

vi.mock('../../../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../../api/client')>();
  const page = (content: unknown[]) => ({
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    last: true,
  });
  return {
    ...original,
    api: {
      GET: vi.fn(
        async (
          path: string,
          options?: { params?: { query?: Record<string, unknown>; path?: { id: number } } },
        ) => {
          if (path === '/api/v1/businesses') {
            return { data: businesses, response: new Response() };
          }
          if (path === '/api/v1/sign-ups') {
            const query = options?.params?.query ?? {};
            calls.list.push(query);
            const content =
              query.status === 'PENDING_APPROVAL'
                ? pending
                : query.status === 'REJECTED'
                  ? rejected
                  : approved;
            return { data: page(content), response: new Response() };
          }
          if (path === '/api/v1/sign-ups/counts') {
            return { data: { pending: 2, approved: 1, rejected: 1 }, response: new Response() };
          }
          if (path === '/api/v1/accounts/{id}/withdrawal') {
            return { data: { allowed: true, blockers: [] }, response: new Response() };
          }
          if (path === '/api/v1/sign-ups/{id}') {
            const found = [...pending, ...rejected, ...approved].find(
              (a) => a.id === options?.params?.path?.id,
            );
            return { data: found, response: new Response() };
          }
          throw new Error(`unexpected GET ${path}`);
        },
      ),
      POST: vi.fn(
        async (
          path: string,
          options: { params: { path: { id: number } }; body?: { reason: string } },
        ) => {
          const id = options.params.path.id;
          calls.post.push({ path, id, body: options.body });
          if (path === '/api/v1/accounts/{id}/withdrawal') {
            if (!options.body?.reason.trim()) {
              return {
                error: {
                  status: 400,
                  code: 'VALIDATION_FAILED',
                  detail: '입력값을 확인해 주세요.',
                  fieldErrors: [{ field: 'reason', message: '탈퇴 처리 사유를 적어 주세요.' }],
                },
                response: new Response(null, { status: 400 }),
              };
            }
            return { response: new Response(null, { status: 204 }) };
          }
          const target = pending.find((a) => a.id === id)!;
          if (path === '/api/v1/sign-ups/{id}/rejection') {
            if (!options.body?.reason.trim()) {
              return {
                error: {
                  status: 400,
                  code: 'VALIDATION_FAILED',
                  detail: '입력값을 확인해 주세요.',
                  fieldErrors: [{ field: 'reason', message: '반려 사유를 적어 주세요.' }],
                },
                response: new Response(null, { status: 400 }),
              };
            }
            return {
              data: { ...target, status: 'REJECTED', rejectionReason: options.body.reason },
              response: new Response(),
            };
          }
          return { data: { ...target, status: 'ACTIVE' }, response: new Response() };
        },
      ),
    },
  };
});

function renderPage(route = '/account-info/admin/sign-ups') {
  return renderWithProviders(<SignUpApprovalPage />, {
    route,
    path: '/:business/admin/sign-ups',
  });
}

beforeEach(() => {
  calls.list.length = 0;
  calls.post.length = 0;
});

describe('SignUpApprovalPage', () => {
  it('현재 업무의 승인 대기 신청을 보여 주고, 상태 칩으로 반려한 신청까지 본다', async () => {
    renderPage();

    const table = await screen.findByTestId('sign-up-table');
    expect(await within(table).findByText('오다라')).toBeInTheDocument();
    expect(within(table).getByText('한가나')).toBeInTheDocument();
    // 이메일 도메인이 기관과 다른 신청만 표시가 붙는다.
    expect(within(table).getAllByText('도메인 다름')).toHaveLength(1);
    expect(calls.list[0]).toMatchObject({ business: 'account-info', status: 'PENDING_APPROVAL' });
    expect(await screen.findByLabelText('승인 대기 2')).toBeChecked();

    fireEvent.click(screen.getByLabelText('반려 1'));
    expect(await within(table).findByText('서사아')).toBeInTheDocument();
    expect(within(table).getByText('처리')).toBeInTheDocument();
    expect(within(table).getByText(/홍길동 · 2026-10-13/)).toBeInTheDocument();
    expect(within(table).queryByText('한가나')).not.toBeInTheDocument();
  });

  it('주소의 id 로 들어오면 그 신청의 상세 패널이 열린다', async () => {
    renderPage('/account-info/admin/sign-ups?id=2');

    const detail = await screen.findByTestId('sign-up-detail');
    expect(detail).toHaveTextContent('oh.dara@mail.example.com');
    expect(detail).toHaveTextContent('등록된 도메인(dara.example.com)과 다릅니다');
    expect(detail).toHaveTextContent('자동이체 전문 개발');
    expect(screen.getByRole('button', { name: '승인' })).toBeInTheDocument();
  });

  it('반려는 사유가 있어야 하고, 서버의 필드 메시지를 그대로 보인다', async () => {
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: '한가나 가입 신청' }));
    fireEvent.click(await screen.findByRole('button', { name: '반려' }));
    const textarea = await screen.findByLabelText(/반려 사유/);
    fireEvent.click(screen.getByRole('button', { name: '반려하기' }));
    expect(await screen.findByText('반려 사유를 적어 주세요.')).toBeInTheDocument();

    fireEvent.change(textarea, { target: { value: '부서장 확인 뒤 다시 신청해 주세요.' } });
    // 칸을 고치면 서버 메시지가 지워진다.
    expect(screen.queryByText('반려 사유를 적어 주세요.')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '반려하기' }));
    await waitFor(() => expect(calls.post).toHaveLength(2));
    expect(calls.post[1]).toEqual({
      path: '/api/v1/sign-ups/{id}/rejection',
      id: 1,
      body: { reason: '부서장 확인 뒤 다시 신청해 주세요.' },
    });
    await waitFor(() => expect(screen.queryByTestId('sign-up-detail')).not.toBeInTheDocument());
  });

  it('승인은 확인 창을 한 번 거쳐 승인 API 를 부른다', async () => {
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: '오다라 가입 신청' }));
    fireEvent.click(await screen.findByRole('button', { name: '승인' }));
    const confirm = await screen.findByRole('dialog', { name: '가입 승인' });
    expect(confirm).toHaveTextContent('다라은행 오다라 님의 가입을 승인합니다.');
    expect(confirm).toHaveTextContent('이메일 도메인이 기관과 다릅니다.');
    expect(calls.post).toHaveLength(0);

    fireEvent.click(within(confirm).getByRole('button', { name: '승인하기' }));
    await waitFor(() =>
      expect(calls.post).toEqual([
        { path: '/api/v1/sign-ups/{id}/approval', id: 2, body: undefined },
      ]),
    );
  });

  it('승인한 참가기관 담당자는 사유를 적어 탈퇴 처리한다', async () => {
    renderPage('/account-info/admin/sign-ups?id=4');

    const detail = await screen.findByTestId('sign-up-detail');
    expect(detail).toHaveTextContent('kim.gana@gana.example.com');
    expect(screen.queryByRole('button', { name: '승인' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '탈퇴 처리' }));
    const dialog = await screen.findByRole('dialog', { name: '탈퇴 처리' });
    expect(dialog).toHaveTextContent('가나은행 김가나 님의 계정을 탈퇴 처리합니다.');

    const submit = within(dialog).getByRole('button', { name: '탈퇴 처리' });
    await waitFor(() => expect(submit).toBeEnabled());
    fireEvent.click(submit);
    expect(await within(dialog).findByText('탈퇴 처리 사유를 적어 주세요.')).toBeInTheDocument();

    fireEvent.change(within(dialog).getByLabelText(/탈퇴 처리 사유/), {
      target: { value: '인사이동으로 담당이 끝났습니다.' },
    });
    fireEvent.click(submit);
    await waitFor(() => expect(calls.post).toHaveLength(2));
    expect(calls.post[1]).toEqual({
      path: '/api/v1/accounts/{id}/withdrawal',
      id: 4,
      body: { reason: '인사이동으로 담당이 끝났습니다.' },
    });
    await waitFor(() => expect(screen.queryByTestId('sign-up-detail')).not.toBeInTheDocument());
  });
});
