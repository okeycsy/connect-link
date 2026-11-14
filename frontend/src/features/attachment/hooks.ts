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
