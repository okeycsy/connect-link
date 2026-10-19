import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { ChangeHistoryList } from './ChangeHistoryList';

const histories = [
  {
    id: 2,
    targetType: 'ORGANIZATION',
    targetId: 3,
    business: null,
    action: 'UPDATED',
    actionLabel: '수정',
    summary: '다라은행 수정',
    changes: [
      {
        field: 'businesses',
        label: '참여 업무',
        before: '계좌통합관리, 자동이체통합관리',
        after: '계좌통합관리, 자동이체통합관리, CMS',
      },
    ],
    changedBy: 'admin@kftc.example.com',
    changedByName: '데모 관리자',
    changedAt: '2026-07-14T14:05:00',
  },
  {
    id: 1,
    targetType: 'ORGANIZATION',
    targetId: 3,
    business: null,
    action: 'CREATED',
    actionLabel: '등록',
    summary: '다라은행 등록',
    changes: [{ field: 'code', label: '기관 코드', before: null, after: '088' }],
    changedBy: 'admin@kftc.example.com',
    changedByName: '데모 관리자',
    changedAt: '2026-03-02T09:30:00',
  },
];

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async (path: string, options?: { params?: { query?: { targetId?: number } } }) => {
      if (path !== '/api/v1/change-histories') throw new Error(`unexpected GET ${path}`);
      const content = options?.params?.query?.targetId === 3 ? histories : [];
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
    }),
  },
  unwrap: (r: { data: unknown }) => r.data,
}));

describe('ChangeHistoryList', () => {
  it('이력을 최신순 타임라인으로 보여 주고 변경 전·후 값을 적는다', async () => {
    renderWithProviders(<ChangeHistoryList targetType="ORGANIZATION" targetId={3} />);

    expect(await screen.findByText('다라은행 수정')).toBeInTheDocument();
    expect(screen.getByText('계좌통합관리, 자동이체통합관리')).toBeInTheDocument();
    expect(screen.getByText('계좌통합관리, 자동이체통합관리, CMS')).toBeInTheDocument();
    expect(
      screen.getByText('데모 관리자 (admin@kftc.example.com) · 2026-07-14 14:05'),
    ).toBeInTheDocument();
    expect(screen.getByText('다라은행 등록')).toBeInTheDocument();
    expect(screen.getByText('088')).toBeInTheDocument();
    const items = screen.getByTestId('change-history').querySelectorAll('.mantine-Timeline-item');
    expect(items[0]).toHaveTextContent('다라은행 수정');
  });

  it('이력이 없으면 빈 안내를 보여 준다', async () => {
    renderWithProviders(<ChangeHistoryList targetType="ORGANIZATION" targetId={9} />);
    expect(await screen.findByTestId('state-empty')).toHaveTextContent('변경 이력 없음');
  });
});
