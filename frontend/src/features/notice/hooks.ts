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
  detail: (id: number) => ['notices', 'detail', id] as const,
};

/** 글 한 건. id 가 없으면 부르지 않는다. */
export function useNotice(id: number | null) {
  return useQuery({
    queryKey: noticeKeys.detail(id ?? 0),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/notices/{id}', { params: { path: { id: id ?? 0 } } })),
    enabled: id != null,
  });
}

/** 글은 그대로 두고 회신 받을 의견 요청만 잇는다(공지에서 "회신 받기: 예" 로 올린 뒤 만든 요청). */
export function useLinkNoticeOpinion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      notice,
      opinionRequestId,
    }: {
      notice: Notice;
      opinionRequestId: number;
    }) =>
      unwrap(
        await api.PUT('/api/v1/notices/{id}', {
          params: { path: { id: notice.id } },
          body: {
            business: notice.business,
            category: notice.category,
            title: notice.title,
            content: notice.content,
            pinned: notice.pinned,
            effectiveDate: notice.effectiveDate,
            opinionRequestId,
          },
        }),
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: noticeKeys.all }),
  });
}

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
