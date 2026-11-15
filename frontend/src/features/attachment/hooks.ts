import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import { useMe } from '../account/hooks';

export type Attachment = components['schemas']['AttachmentResponse'];
export type AttachmentOwnerType = Attachment['ownerType'];

export interface AttachmentOwner {
  ownerType: AttachmentOwnerType;
  ownerId: number;
}

export const attachmentKeys = {
  all: ['attachments'] as const,
  list: (owner: AttachmentOwner) => ['attachments', owner.ownerType, owner.ownerId] as const,
};

/** 내려받기 주소. 링크(a href)로 열면 브라우저가 원래 이름으로 저장한다. */
export function attachmentDownloadUrl(id: number): string {
  return `/api/v1/attachments/${id}/download`;
}

/**
 * 결제원 계정이 인터넷망에 있으면 첨부파일을 올리거나 내려받지 못한다(결제원 보안 규정: 인터넷PC 업무자료 취급 금지). 서버가 403
 * INTERNAL_ONLY 로 막고, 화면은 이 값으로 올리기 버튼과 내려받기 링크를 감춘다.
 */
export function useFileTransferBlocked(): boolean {
  const { data: me } = useMe();
  return me?.organizationType === 'KFTC' && me.networkZone === 'EXTERNAL';
}

/**
 * 결제원 계정이 올리는 파일은 외부망 반출이라 같은 부서·팀 팀장의 결재를 받아야 참가기관에 보인다(서버가 정한다). 화면은 이 값으로 올리기
 * 칸에 결재 안내를 보인다.
 */
export function useExportApprovalRequired(): boolean {
  const { data: me } = useMe();
  return me?.organizationType === 'KFTC';
}

export function useAttachments(owner: AttachmentOwner) {
  return useQuery({
    queryKey: attachmentKeys.list(owner),
    queryFn: async () => unwrap(await api.GET('/api/v1/attachments', { params: { query: owner } })),
  });
}

/** 파일 하나를 올린다. 대상이 저장 뒤에야 정해지는 화면(새 의견 요청)은 이 함수를 직접 부른다. */
export async function uploadAttachmentFile(owner: AttachmentOwner, file: File) {
  return unwrap(
    await api.POST('/api/v1/attachments', {
      params: { query: owner },
      // OpenAPI 는 파일을 string(binary) 으로 적는다. 실제 전송은 아래 serializer 가 FormData 로 만든다.
      body: { file: file as unknown as string },
      bodySerializer: () => {
        const form = new FormData();
        form.append('file', file);
        return form;
      },
    }),
  );
}

export function useUploadAttachment(owner: AttachmentOwner) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => uploadAttachmentFile(owner, file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: attachmentKeys.list(owner) }),
  });
}

export function useDeleteAttachment(owner: AttachmentOwner) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      const result = await api.DELETE('/api/v1/attachments/{id}', { params: { path: { id } } });
      if (result.error !== undefined) {
        unwrap(result);
      }
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: attachmentKeys.list(owner) }),
  });
}

// ---- 파일 반출 결재 ----

export type AttachmentApproval = components['schemas']['AttachmentApprovalResponse'];
export type AttachmentApprovalStatus = Attachment['approvalStatus'];
export type AttachmentApprovalBox = 'TO_APPROVE' | 'REQUESTED';

export const approvalKeys = {
  all: ['attachment-approvals'] as const,
  list: (box: AttachmentApprovalBox, status: AttachmentApprovalStatus | undefined, page: number) =>
    ['attachment-approvals', 'list', box, status ?? 'ALL', page] as const,
  pendingCount: ['attachment-approvals', 'pending-count'] as const,
};

/** 파일 결재함. TO_APPROVE 는 내가 결재할 파일, REQUESTED 는 내가 올린 파일. */
export function useAttachmentApprovals(
  box: AttachmentApprovalBox,
  status: AttachmentApprovalStatus | undefined,
  page: number,
) {
  return useQuery({
    queryKey: approvalKeys.list(box, status, page),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/attachment-approvals', {
          params: { query: { box, status, page, size: 20 } },
        }),
      ),
  });
}

/** 내가 결재할 대기 파일 수(사이드바 배지). 결제원 계정만 묻는다. */
export function usePendingApprovalCount(enabled: boolean) {
  return useQuery({
    queryKey: approvalKeys.pendingCount,
    queryFn: async () => unwrap(await api.GET('/api/v1/attachment-approvals/pending-count')),
    enabled,
    refetchInterval: 60_000,
  });
}

function useApprovalDecision<T>(decide: (id: number, input: T) => Promise<AttachmentApproval>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: T }) => decide(id, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: approvalKeys.all });
      queryClient.invalidateQueries({ queryKey: attachmentKeys.all });
    },
  });
}

export function useApproveAttachment() {
  return useApprovalDecision<void>(async (id) =>
    unwrap(
      await api.POST('/api/v1/attachment-approvals/{id}/approval', { params: { path: { id } } }),
    ),
  );
}

export function useRejectAttachment() {
  return useApprovalDecision<string>(async (id, reason) =>
    unwrap(
      await api.POST('/api/v1/attachment-approvals/{id}/rejection', {
        params: { path: { id } },
        body: { reason },
      }),
    ),
  );
}
