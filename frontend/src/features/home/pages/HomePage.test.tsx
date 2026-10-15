import { screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { HomePage } from './HomePage';

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async (path: string) => {
      if (path === '/api/v1/businesses') {
        return {
          data: [
            {
              code: 'ACCOUNT_INFO',
              slug: 'account-info',
              label: '계좌통합관리',
              shortLabel: '계통',
            },
            { code: 'CMS', slug: 'cms', label: 'CMS', shortLabel: 'CMS' },
          ],
          response: new Response(),
        };
      }
      return {
        data: [
          { id: 1, code: 'KFTC', name: '금융결제원', type: 'KFTC', typeLabel: '결제원' },
          {
            id: 2,
            code: 'GANA',
            name: '가나은행',
            type: 'FINANCE_ORGANIZATION',
            typeLabel: '참가기관',
          },
        ],
        response: new Response(),
      };
    }),
  },
  unwrap: (r: { data: unknown }) => r.data,
}));

describe('HomePage', () => {
  it('현재 업무 영역과 기관 목록을 보여 준다', async () => {
    renderWithProviders(<HomePage />);

    expect(await screen.findByText('계좌통합관리')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('가나은행')).toBeInTheDocument());
    expect(screen.getByText('금융결제원')).toBeInTheDocument();
    expect(screen.getAllByText('참가기관').length).toBeGreaterThan(0);
  });
});
