import { fireEvent, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { NoticeListPage } from './NoticeListPage';

const businesses = [
  { code: 'ACCOUNT_INFO', slug: 'account-info', label: '계좌통합관리', shortLabel: '계통' },
  { code: 'CMS', slug: 'cms', label: 'CMS', shortLabel: 'CMS' },
];

const notices = [
  {
    id: 1,
    business: null,
    businessLabel: null,
    category: 'NOTICE',
    categoryLabel: '공지',
    title: '연결이 이용 안내',
    content: '처음 쓰시는 분은 이용 가이드를 읽어 주세요.',
    pinned: true,
    effectiveDate: null,
    authorName: '데모 관리자',
    createdAt: '2026-10-05T09:00:00',
    updatedAt: '2026-10-05T09:00:00',
    canEdit: false,
  },
  {
    id: 2,
    business: 'ACCOUNT_INFO',
    businessLabel: '계좌통합관리',
    category: 'RULE',
    categoryLabel: '규약',
    title: '계좌통합관리업무 규약(예시)',
    content: '규약 본문',
    pinned: false,
    effectiveDate: '2026-09-01',
    authorName: '홍성민',
    createdAt: '2026-08-26T09:00:00',
    updatedAt: '2026-08-26T09:00:00',
    canEdit: false,
  },
];

const calls = vi.hoisted(() => ({ list: [] as Record<string, unknown>[] }));
const me = vi.hoisted(() => ({
  current: { admin: false, organizationType: 'FINANCE_ORGANIZATION', businesses: [] as unknown[] },
}));

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async (path: string, options?: { params?: { query?: Record<string, unknown> } }) => {
      if (path === '/api/v1/businesses') return { data: businesses, response: new Response() };
      if (path === '/api/v1/me') return { data: me.current, response: new Response() };
      if (path === '/api/v1/attachments') return { data: [], response: new Response() };
      if (path === '/api/v1/notices') {
        const query = options?.params?.query ?? {};
        calls.list.push(query);
        const content = notices.filter((n) => !query.category || n.category === query.category);
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
    }),
    POST: vi.fn(),
    PUT: vi.fn(),
    DELETE: vi.fn(),
  },
  unwrap: (r: { data: unknown }) => r.data,
  fieldErrorsOf: () => ({}),
  ApiError: class extends Error {},
}));

describe('NoticeListPage', () => {
  it('공통 공지와 업무 자료를 보여 주고, 분류로 거르고, 글을 누르면 내용이 열린다', async () => {
    renderWithProviders(<NoticeListPage />, {
      route: '/account-info/notices',
      path: '/:business/notices',
    });

    expect(await screen.findByText('연결이 이용 안내')).toBeInTheDocument();
    expect(screen.getByText('전 업무 공통')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '글 쓰기' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('규약'));
    await waitFor(() => expect(screen.queryByText('연결이 이용 안내')).not.toBeInTheDocument());
    expect(calls.list.at(-1)).toMatchObject({ business: 'account-info', category: 'RULE' });

    fireEvent.click(screen.getByText('계좌통합관리업무 규약(예시)'));
    expect(await screen.findByText('규약 본문')).toBeInTheDocument();
    expect(screen.getByText('2026-09-01')).toBeInTheDocument();
  });

  it('그 업무 결제원 담당자에게는 글 쓰기 버튼이 보인다', async () => {
    me.current = { admin: false, organizationType: 'KFTC', businesses: [businesses[0]] };
    renderWithProviders(<NoticeListPage />, {
      route: '/account-info/notices',
      path: '/:business/notices',
    });
    expect(await screen.findByRole('button', { name: '글 쓰기' })).toBeInTheDocument();
  });
});
