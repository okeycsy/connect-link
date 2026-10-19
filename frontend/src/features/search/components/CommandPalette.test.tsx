import { fireEvent, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { CommandPalette } from './CommandPalette';

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async (path: string, options?: { params?: { query?: { q?: string } } }) => {
      if (path !== '/api/v1/search') throw new Error(`unexpected GET ${path}`);
      const q = options?.params?.query?.q ?? '';
      const groups =
        q === '다라'
          ? [
              {
                category: 'ORGANIZATION',
                label: '기관',
                source: 'LIVE',
                hits: [
                  {
                    title: '다라은행',
                    subtitle: '참가기관 · 088',
                    link: '/cms/admin/organizations?org=3',
                  },
                ],
              },
              {
                category: 'CONTACT',
                label: '담당자',
                source: 'SAMPLE',
                hits: [
                  { title: '이다라', subtitle: '다라은행 · 전산(대외계)', link: '/cms/contacts' },
                ],
              },
            ]
          : [];
      return { data: { query: q, business: 'CMS', groups }, response: new Response() };
    }),
  },
  unwrap: (r: { data: unknown }) => r.data,
}));

describe('CommandPalette', () => {
  it('검색어가 없으면 바로 가기만, 검색어를 치면 서버 결과가 묶음별로 보인다', async () => {
    renderWithProviders(<CommandPalette opened onClose={() => {}} />, {
      route: '/cms/home',
      path: '/:business/home',
    });

    expect(await screen.findByRole('option', { name: '연락처' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: '관리 › 기관·업무 기준정보' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('검색어'), { target: { value: '다라' } });
    expect(await screen.findByRole('option', { name: '다라은행' })).toBeInTheDocument();
    expect(screen.getByText('참가기관 · 088')).toBeInTheDocument();
    expect(screen.getByRole('option', { name: '이다라' })).toBeInTheDocument();
    expect(screen.getByText('샘플')).toBeInTheDocument();
    // 바로 가기는 검색어에 맞는 것만 남는다.
    expect(screen.queryByRole('option', { name: '연락처' })).not.toBeInTheDocument();
    // 첫 결과가 선택돼 있다.
    expect(screen.getByRole('option', { name: '다라은행' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  it('맞는 결과가 없으면 안내가 뜨고, Enter 로 선택한 항목을 열면 닫힌다', async () => {
    const onClose = vi.fn();
    renderWithProviders(<CommandPalette opened onClose={onClose} />, {
      route: '/cms/home',
      path: '/:business/home',
    });
    const input = await screen.findByLabelText('검색어');

    fireEvent.change(input, { target: { value: 'zzz' } });
    expect(await screen.findByText("'zzz' 에 맞는 결과가 없습니다.")).toBeInTheDocument();

    fireEvent.change(input, { target: { value: '다라' } });
    await screen.findByRole('option', { name: '다라은행' });
    fireEvent.keyDown(input, { key: 'ArrowDown' });
    await waitFor(() =>
      expect(screen.getByRole('option', { name: '이다라' })).toHaveAttribute(
        'aria-selected',
        'true',
      ),
    );
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onClose).toHaveBeenCalled();
  });
});
