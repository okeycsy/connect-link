import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';

export type Notice = components['schemas']['NoticeResponse'];
export type NoticeSaveRequest = components['schemas']['NoticeSaveRequest'];
export type NoticeCategory = Notice['category'];

export const NOTICE_CATEGORY_LABEL: Record<NoticeCategory, string> = {
  NOTICE: '공지',
  RULE: '규약',
  ENFORCEMENT_RULE: '시행세칙',
};

export const noticeKeys = {
  all: ['notices'] as const,
  list: (business: string, category: NoticeCategory | null, page: number) =>
    ['notices', 'list', business, category, page] as const,
};

/** 이 업무의 글과 전 업무 공통 공지. 고정 글이 먼저다. */
export function useNotices(business: string, category: NoticeCategory | null, page: number) {
  return useQuery({
    queryKey: noticeKeys.list(business, category, page),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/notices', {
          params: { query: { business, category: category ?? undefined, page, size: 20 } },
        }),
      ),
    enabled: Boolean(business),
    placeholderData: keepPreviousData,
  });
}

export function useSaveNotice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number | null; body: NoticeSaveRequest }) =>
      unwrap(
        id == null
          ? await api.POST('/api/v1/notices', { body })
          : await api.PUT('/api/v1/notices/{id}', { params: { path: { id } }, body }),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: noticeKeys.all }),
  });
}

export function useDeleteNotice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      unwrap(await api.DELETE('/api/v1/notices/{id}', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: noticeKeys.all }),
  });
}
