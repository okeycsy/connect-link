import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';

export type NotificationResponse = components['schemas']['NotificationResponse'];
export type NotificationOutboxResponse = components['schemas']['NotificationOutboxResponse'];

export const notificationKeys = {
  all: ['notifications'] as const,
  list: (page: number, unreadOnly: boolean) =>
    ['notifications', 'list', { page, unreadOnly }] as const,
  unreadCount: ['notifications', 'unread-count'] as const,
  outbox: (page: number) => ['notification-outbox', { page }] as const,
};

export function useNotifications({
  page = 0,
  size = 20,
  unreadOnly = false,
}: { page?: number; size?: number; unreadOnly?: boolean } = {}) {
  return useQuery({
    queryKey: [...notificationKeys.list(page, unreadOnly), size],
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/notifications', { params: { query: { page, size, unreadOnly } } }),
      ),
  });
}

/** 벨 배지. 30초마다 다시 읽는다. */
export function useUnreadCount() {
  return useQuery({
    queryKey: notificationKeys.unreadCount,
    queryFn: async () => unwrap(await api.GET('/api/v1/notifications/unread-count')),
    refetchInterval: 30_000,
  });
}

export function useMarkRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      unwrap(await api.POST('/api/v1/notifications/{id}/read', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}

export function useMarkAllRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const result = await api.POST('/api/v1/notifications/read-all');
      if (result.error !== undefined) {
        unwrap(result);
      }
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKeys.all }),
  });
}

export function useNotificationOutbox(page = 0, size = 20) {
  return useQuery({
    queryKey: [...notificationKeys.outbox(page), size],
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/notification-outbox', { params: { query: { page, size } } })),
  });
}
