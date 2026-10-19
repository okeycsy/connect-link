import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { OrganizationAdminPage } from './OrganizationAdminPage';

const businesses = [
  { code: 'ACCOUNT_INFO', slug: 'account-info', label: '계좌통합관리', shortLabel: '계통' },
  { code: 'AUTO_TRANSFER', slug: 'auto-transfer', label: '자동이체통합관리', shortLabel: '자통' },
  { code: 'LOAN_TRANSFER', slug: 'loan-transfer', label: '대출이동', shortLabel: '대출' },
  { code: 'CMS', slug: 'cms', label: 'CMS', shortLabel: 'CMS' },
];

const organizations = [
  {
    id: 1,
    code: 'KFTC',
    name: '금융결제원',
    type: 'KFTC',
    typeLabel: '결제원',
    emailDomain: 'kftc.example.com',
    active: true,
    businesses,
    createdAt: '2026-10-15T09:00:00',
    createdBy: 'seed',
    updatedAt: '2026-10-15T09:00:00',
    updatedBy: 'seed',
  },
  {
    id: 2,
    code: '004',
    name: 'KB국민은행',
    type: 'FINANCE_ORGANIZATION',
    typeLabel: '참가기관',
    emailDomain: 'kb.example.com',
    active: true,
    businesses: businesses.slice(0, 2),
    createdAt: '2026-10-15T09:00:00',
    createdBy: 'seed',
    updatedAt: '2026-10-15T09:00:00',
    updatedBy: 'seed',
  },
  {
    id: 3,
    code: 'HAHA',
    name: '하하통신',
    type: 'USE_ORGANIZATION',
    typeLabel: '이용기관',
    emailDomain: null,
    active: true,
    businesses: [businesses[3]],
    createdAt: '2026-10-15T09:00:00',
    createdBy: 'seed',
    updatedAt: '2026-10-15T09:00:00',
    updatedBy: 'seed',
  },
];

// vi.mock 은 파일 맨 위로 끌어올려지므로 mock 안에서 쓰는 값은 vi.hoisted 로 만든다.
const calls = vi.hoisted(() => ({ search: [] as Record<string, unknown>[] }));

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(
      async (
        path: string,
        options?: { params?: { query?: Record<string, unknown>; path?: { id: number } } },
      ) => {
        if (path === '/api/v1/businesses') {
          return { data: businesses, response: new Response() };
        }
        if (path === '/api/v1/organizations/search') {
          const query = options?.params?.query ?? {};
          calls.search.push(query);
          const content = organizations.filter(
            (o) =>
              (!query.business || o.businesses.some((b) => b.slug === query.business)) &&
              (!query.type || o.type === query.type),
          );
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
        if (path === '/api/v1/organizations/{id}') {
          const found = organizations.find((o) => o.id === options?.params?.path?.id);
          return { data: found, response: new Response() };
        }
        throw new Error(`unexpected GET ${path}`);
      },
    ),
    POST: vi.fn(),
    PUT: vi.fn(),
  },
  unwrap: (r: { data: unknown }) => r.data,
  fieldErrorsOf: () => ({}),
  ApiError: class extends Error {},
}));

describe('OrganizationAdminPage', () => {
  it('현재 업무에 참여하는 기관만 보여 주고, 전체 업무 칩으로 넓힐 수 있다', async () => {
    renderWithProviders(<OrganizationAdminPage />, {
      route: '/loan-transfer/admin/organizations',
      path: '/:business/admin/organizations',
    });

    expect(await screen.findByText('금융결제원')).toBeInTheDocument();
    expect(screen.queryByText('KB국민은행')).not.toBeInTheDocument();
    expect(screen.getByTestId('organization-count')).toHaveTextContent('1곳');
    expect(calls.search[0]).toMatchObject({ business: 'loan-transfer', active: true });

    fireEvent.click(screen.getByLabelText('전체 업무'));
    expect(await screen.findByText('KB국민은행')).toBeInTheDocument();
    expect(screen.getByText('하하통신')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTestId('organization-count')).toHaveTextContent('3곳'));

    fireEvent.click(screen.getByLabelText('이용기관'));
    await waitFor(() => expect(screen.queryByText('KB국민은행')).not.toBeInTheDocument());
    expect(screen.getByText('하하통신')).toBeInTheDocument();
  });

  it('행을 누르면 오른쪽 패널에 수정 폼이 열린다', async () => {
    renderWithProviders(<OrganizationAdminPage />, {
      route: '/account-info/admin/organizations',
      path: '/:business/admin/organizations',
    });

    fireEvent.click(await screen.findByRole('button', { name: 'KB국민은행' }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('004 · 참가기관');
    await waitFor(() => expect(screen.getByLabelText(/기관명/)).toHaveValue('KB국민은행'));
    expect(screen.getByLabelText('계좌통합관리')).toBeChecked();
    expect(screen.getByLabelText('CMS')).not.toBeChecked();
    expect(screen.getByRole('button', { name: '저장' })).toBeInTheDocument();
  });

  it('기관 추가를 누르면 코드와 구분까지 받는 등록 폼이 열린다', async () => {
    renderWithProviders(<OrganizationAdminPage />, {
      route: '/cms/admin/organizations',
      path: '/:business/admin/organizations',
    });
    await screen.findByText('금융결제원');

    fireEvent.click(screen.getByRole('button', { name: '기관 추가' }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('기관 등록');
    expect(within(dialog).getByLabelText(/기관 코드/)).toBeInTheDocument();
    // 현재 레일의 업무가 미리 체크돼 있다. (레일 칩에도 'CMS' 가 있어 패널 안에서만 찾는다.)
    expect(within(dialog).getByLabelText('CMS')).toBeChecked();
    expect(within(dialog).getByLabelText('계좌통합관리')).not.toBeChecked();
    expect(screen.getByRole('button', { name: '등록' })).toBeInTheDocument();
  });
});
