import { fireEvent, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { NotificationCenterPage } from './NotificationCenterPage';

const notifications = [
  {
    id: 1,
    type: 'SIGNUP_REQUESTED',
    typeLabel: '가입 신청',
    business: null,
    businessSlug: null,
    businessLabel: null,
    title: '가나은행 김가나 님이 가입을 신청했습니다',
    body: '이메일 도메인이 기관과 일치합니다.',
    link: '/account-info/admin',
    read: false,
    readAt: null,
    createdAt: '2026-10-15T10:00:00',
  },
  {
    id: 2,
    type: 'SUSPENSION_CHANGED',
    typeLabel: '일시중단 변경',
    business: 'CMS',
    businessSlug: 'cms',
    businessLabel: 'CMS',
    title: '다라은행 CMS 일시중단이 확정되었습니다',
    body: null,
    link: '/cms/suspensions',
    read: true,
    readAt: '2026-10-14T09:00:00',
    createdAt: '2026-10-14T08:00:00',
  },
];

// vi.mock 은 파일 맨 위로 끌어올려지므로 mock 안에서 쓰는 값은 vi.hoisted 로 만든다.
const post = vi.hoisted(() => vi.fn(async () => ({ data: {}, response: new Response() })));

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(
      async (path: string, options?: { params?: { query?: { unreadOnly?: boolean } } }) => {
        if (path === '/api/v1/notifications/unread-count') {
          return { data: { count: 1 }, response: new Response() };
        }
        const unreadOnly = options?.params?.query?.unreadOnly ?? false;
        const content = unreadOnly ? notifications.filter((n) => !n.read) : notifications;
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
      },
    ),
    POST: post,
  },
  unwrap: (r: { data: unknown }) => r.data,
}));

describe('NotificationCenterPage', () => {
  it('알림 목록과 읽지 않은 수를 보여 주고, 읽지 않음 필터가 동작한다', async () => {
    renderWithProviders(<NotificationCenterPage />, {
      route: '/account-info/notifications',
      path: '/:business/notifications',
    });

    expect(await screen.findByText('가나은행 김가나 님이 가입을 신청했습니다')).toBeInTheDocument();
    expect(screen.getByText('다라은행 CMS 일시중단이 확정되었습니다')).toBeInTheDocument();
    expect(screen.getByText('읽지 않은 알림 1건')).toBeInTheDocument();
    expect(screen.getByText('2건')).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('읽지 않음'));
    await waitFor(() =>
      expect(screen.queryByText('다라은행 CMS 일시중단이 확정되었습니다')).not.toBeInTheDocument(),
    );
    await waitFor(() => expect(screen.getByText('1건')).toBeInTheDocument());
  });

  it('알림을 누르면 읽음 처리 API 를 부른다', async () => {
    renderWithProviders(<NotificationCenterPage />, {
      route: '/account-info/notifications',
      path: '/:business/notifications',
    });

    fireEvent.click(
      await screen.findByRole('button', { name: '가나은행 김가나 님이 가입을 신청했습니다' }),
    );
    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/v1/notifications/{id}/read', {
        params: { path: { id: 1 } },
      }),
    );
  });
});
