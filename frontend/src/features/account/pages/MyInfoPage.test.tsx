import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { MyInfoPage } from './MyInfoPage';

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async () => ({
      data: {
        email: 'admin@kftc.example.com',
        name: '데모 관리자',
        organizationCode: 'KFTC',
        organizationName: '금융결제원',
        organizationType: 'KFTC',
        organizationTypeLabel: '결제원',
        admin: true,
        businesses: [
          { code: 'ACCOUNT_INFO', slug: 'account-info', label: '계좌통합관리', shortLabel: '계통' },
          { code: 'CMS', slug: 'cms', label: 'CMS', shortLabel: 'CMS' },
        ],
        jobRole: 'IT',
        jobRoleLabel: '전산(운영)',
        itRoles: ['OPS'],
        roleLabel: '결제원 · 전산(운영)',
        networkZone: 'EXTERNAL',
        networkZoneLabel: '인터넷망',
      },
      response: new Response(),
    })),
  },
  unwrap: (r: { data: unknown }) => r.data,
  errorCodeOf: () => undefined,
  ApiError: class extends Error {},
}));

describe('MyInfoPage', () => {
  it('계정 정보, 담당 업무·직무와 접속 망을 보여 주고 연락처·탈퇴는 준비 중으로 둔다', async () => {
    renderWithProviders(<MyInfoPage />, { route: '/cms/me', path: '/:business/me' });
    expect(await screen.findByText('데모 관리자')).toBeInTheDocument();
    expect(screen.getByText('금융결제원 · 결제원')).toBeInTheDocument();
    expect(screen.getByText('admin@kftc.example.com')).toBeInTheDocument();
    expect(screen.getByText('관리자')).toBeInTheDocument();
    expect(screen.getByText('계좌통합관리')).toBeInTheDocument();
    expect(screen.getByText('전산(운영)')).toBeInTheDocument();
    expect(screen.getByText('인터넷망')).toBeInTheDocument();
    expect(screen.getByText('준비 중 · 연락처 모듈')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '연락처 수정' })).toBeDisabled();
    expect(screen.getByRole('button', { name: /탈퇴/ })).toBeDisabled();
  });
});
