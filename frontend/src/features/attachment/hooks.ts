import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import { useMe } from '../account/hooks';

export type Attachment = components['schemas']['AttachmentResponse'];
export type AttachmentOwnerType = Attachment['ownerType'];
export type AttachmentInspectionStatus = Attachment['inspectionStatus'];

/** 반입 보안 검사를 통과했거나 검사가 필요 없는 파일만 내려받을 수 있다(서버가 409 로 막는다). */
export function inspectionCleared(attachment: Pick<Attachment, 'inspectionStatus'>): boolean {
  return attachment.inspectionStatus === 'NOT_REQUIRED' || attachment.inspectionStatus === 'PASSED';
}

/** 보안 검사 중인 파일이 있을 때 목록을 다시 읽는 간격(ms). */
const INSPECTION_REFETCH_MS = 1000;

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
    // 보안 검사 중인 파일이 있으면 끝날 때까지 짧게 다시 읽어, 새로고침 없이 내려받기 링크로 바뀐다.
    refetchInterval: (query) =>
      query.state.data?.some((a) => a.inspectionStatus === 'PENDING')
        ? INSPECTION_REFETCH_MS
        : false,
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

export type AttachmentApprovalBatch = components['schemas']['AttachmentApprovalBatchResponse'];
export type AttachmentApproval = AttachmentApprovalBatch['files'][number];
export type AttachmentApprovalStatus = Attachment['approvalStatus'];

export const approvalKeys = {
  all: ['attachment-approvals'] as const,
  batch: (owner: AttachmentOwner) =>
    ['attachment-approvals', owner.ownerType, owner.ownerId] as const,
};

const approvalPath = '/api/v1/attachment-approvals/{ownerType}/{ownerId}' as const;

/** 결재 한 건(등록 한 건에 붙인 파일). 팀장이 알림 센터에서 결재 창을 열 때 읽는다. */
export function useAttachmentApproval(owner: AttachmentOwner | null) {
  return useQuery({
    queryKey: owner ? approvalKeys.batch(owner) : approvalKeys.all,
    queryFn: async () => unwrap(await api.GET(approvalPath, { params: { path: owner! } })),
    enabled: owner !== null,
    retry: false,
  });
}

function useApprovalDecision<T>(
  decide: (owner: AttachmentOwner, input: T) => Promise<AttachmentApprovalBatch>,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ owner, input }: { owner: AttachmentOwner; input: T }) => decide(owner, input),
    onSuccess: (data, { owner }) => {
      queryClient.setQueryData(approvalKeys.batch(owner), data);
      queryClient.invalidateQueries({ queryKey: attachmentKeys.all });
    },
  });
}

/** 결재 대기 파일을 모두 승인한다. */
export function useApproveAttachments() {
  return useApprovalDecision<void>(async (owner) =>
    unwrap(await api.POST(`${approvalPath}/approval`, { params: { path: owner } })),
  );
}

/** 결재 대기 파일을 모두 반려한다(사유 필수). */
export function useRejectAttachments() {
  return useApprovalDecision<string>(async (owner, reason) =>
    unwrap(
      await api.POST(`${approvalPath}/rejection`, { params: { path: owner }, body: { reason } }),
    ),
  );
}
