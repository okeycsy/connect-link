import {
  keepPreviousData,
  type QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api, ApiError, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import { fileNameOf, saveBlob } from '../../shared/download';
import type { CurrentUserResponse } from '../account/hooks';
import { historyKeys } from '../history/hooks';
import { homeKeys } from '../home/hooks';

export type Contact = components['schemas']['ContactResponse'];
export type ContactSaveRequest = components['schemas']['ContactSaveRequest'];
export type ContactOrganization = components['schemas']['ContactOrganizationResponse'];
export type ContactRank = Contact['rank'];
export type JobRole = Contact['jobRole'];
export type ItRole = Contact['itRoles'][number];
export type BusinessCode = Contact['business'];
export type UpdateCampaign = components['schemas']['UpdateCampaignResponse'];
export type UpdateCampaignSaveRequest = components['schemas']['UpdateCampaignSaveRequest'];
export type UpdateCampaignOrganization =
  components['schemas']['UpdateCampaignOrganizationResponse'];
export type UpdateCampaignProgressFilter = 'ALL' | 'COMPLETED' | 'INCOMPLETE' | 'CHANGED';

export interface ContactListParams {
  /** 업무 영역 slug. */
  business: string;
  /** 아래 조건은 내부망 전용이다. 인터넷망에서는 보내지 않는다. */
  q?: string;
  organizationId?: number;
  jobRole?: JobRole;
  needsConfirm?: boolean;
  page?: number;
  size?: number;
}

export const contactKeys = {
  all: ['contacts'] as const,
  list: (params: ContactListParams) => ['contacts', 'list', params] as const,
  counts: (business: string) => ['contacts', 'counts', business] as const,
  detail: (id: number) => ['contacts', 'detail', id] as const,
  organizations: (business: string) => ['contacts', 'organizations', business] as const,
  mine: ['contacts', 'me'] as const,
  reconfirmDue: ['contacts', 'reconfirm-due'] as const,
};

export const campaignKeys = {
  all: ['update-campaigns'] as const,
  list: (business: string) => ['update-campaigns', 'list', business] as const,
  current: (business: string) => ['update-campaigns', 'current', business] as const,
  progress: (id: number, filter: UpdateCampaignProgressFilter) =>
    ['update-campaigns', 'progress', id, filter] as const,
  schedule: (business: string) => ['update-campaigns', 'schedule', business] as const,
};

/** 이 업무에서 결제원 담당자로서 연락처를 관리하는지(전 기관 수정·삭제, 정기 변경 주간). 서버도 같은 규칙으로 막는다. */
export function canManageContacts(me: CurrentUserResponse | undefined, slug: string): boolean {
  if (!me) return false;
  return me.admin || (me.organizationType === 'KFTC' && me.businesses.some((b) => b.slug === slug));
}

/** 이 업무에서 기관 담당자로서 자기 기관 담당자를 다루는지. */
export function isContactOrganizationMember(
  me: CurrentUserResponse | undefined,
  slug: string,
): boolean {
  if (!me || me.admin) return false;
  return (
    me.organizationType === 'FINANCE_ORGANIZATION' && me.businesses.some((b) => b.slug === slug)
  );
}

export function useContacts(params: ContactListParams) {
  return useQuery({
    queryKey: contactKeys.list(params),
    queryFn: async () => unwrap(await api.GET('/api/v1/contacts', { params: { query: params } })),
    placeholderData: keepPreviousData,
    enabled: Boolean(params.business),
  });
}

export function useContactCounts(business: string) {
  return useQuery({
    queryKey: contactKeys.counts(business),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/contacts/counts', { params: { query: { business } } })),
    enabled: Boolean(business),
  });
}

export function useContact(id: number | null) {
  return useQuery({
    queryKey: contactKeys.detail(id ?? 0),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/contacts/{id}', { params: { path: { id: id! } } })),
    enabled: id != null,
  });
}

/** 등록 양식과 기관 거르기의 기관 목록. 결제원 담당자는 결제원과 그 업무 참가기관, 기관 담당자는 결제원과 자기 기관이다. */
export function useContactOrganizations(business: string) {
  return useQuery({
    queryKey: contactKeys.organizations(business),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/contacts/organizations', { params: { query: { business } } })),
    enabled: Boolean(business),
    staleTime: 60_000,
  });
}

/** 로그인한 사람 본인의 담당자 정보(업무별). 내 정보 화면이 쓴다. */
export function useMyContacts() {
  return useQuery({
    queryKey: contactKeys.mine,
    queryFn: async () => unwrap(await api.GET('/api/v1/contacts/me')),
  });
}

/** 로그인 때 확인 알림을 띄울 최근 확인일 6개월 경과 담당자(본인, 기관 담당자는 자기 기관 담당자까지). */
export function useReconfirmDue(enabled: boolean) {
  return useQuery({
    queryKey: contactKeys.reconfirmDue,
    queryFn: async () => unwrap(await api.GET('/api/v1/contacts/reconfirm-due')),
    enabled,
  });
}

/** 담당자가 바뀐 뒤 다시 읽을 것들. 목록·건수·상세·이력·정기 변경 현황·홈 카드가 함께 바뀐다. */
function invalidate(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: contactKeys.all });
  void queryClient.invalidateQueries({ queryKey: campaignKeys.all });
  void queryClient.invalidateQueries({ queryKey: historyKeys.all });
  void queryClient.invalidateQueries({ queryKey: homeKeys.all });
}

export function useCreateContact() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: ContactSaveRequest) =>
      unwrap(await api.POST('/api/v1/contacts', { body })),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useUpdateContact() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: ContactSaveRequest }) =>
      unwrap(await api.PUT('/api/v1/contacts/{id}', { params: { path: { id } }, body })),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useDeleteContact() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      const result = await api.DELETE('/api/v1/contacts/{id}', { params: { path: { id } } });
      if (result.error !== undefined || !result.response.ok) {
        throw new ApiError(result.response.status, result.error);
      }
    },
    onSuccess: () => invalidate(queryClient),
  });
}

/** "변동 없음" 확인. 최근 확인일만 바뀌고 이력은 남지 않는다. */
export function useConfirmContact() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      unwrap(await api.POST('/api/v1/contacts/{id}/confirmation', { params: { path: { id } } })),
    onSuccess: () => invalidate(queryClient),
  });
}

/** 기관 하나의 한 업무 담당자 전부를 확인하고, 진행 중인 정기 변경 주간에 "현행화 완료" 로 남긴다. */
export function useConfirmOrganization() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: components['schemas']['ContactOrganizationConfirmRequest']) =>
      unwrap(await api.POST('/api/v1/contacts/organization-confirmations', { body })),
    onSuccess: () => invalidate(queryClient),
  });
}

/** 담당자 엑셀 내려받기(내부망 전용). 지금 거른 조건 그대로 받는다. */
export function useDownloadContactExcel() {
  return useMutation({
    mutationFn: async (params: ContactListParams) => {
      const query = {
        business: params.business,
        q: params.q,
        organizationId: params.organizationId,
        jobRole: params.jobRole,
        needsConfirm: params.needsConfirm,
      };
      const result = await api.GET('/api/v1/contacts/excel', {
        params: { query },
        parseAs: 'blob',
      });
      if (result.error !== undefined || !result.response.ok || !result.data) {
        throw new ApiError(result.response.status, result.error);
      }
      saveBlob(result.data as unknown as Blob, fileNameOf(result.response) ?? '담당자.xlsx');
    },
  });
}

export type ContactImportResponse = components['schemas']['ContactImportResponse'];

/** 담당자 일괄 등록 양식 내려받기(그 업무 결제원 담당자, 내부망 전용). */
export function useDownloadContactImportTemplate() {
  return useMutation({
    mutationFn: async (business: string) => {
      const result = await api.GET('/api/v1/contacts/import-template', {
        params: { query: { business } },
        parseAs: 'blob',
      });
      if (result.error !== undefined || !result.response.ok || !result.data) {
        throw new ApiError(result.response.status, result.error);
      }
      saveBlob(
        result.data as unknown as Blob,
        fileNameOf(result.response) ?? '담당자일괄등록양식.xlsx',
      );
    },
  });
}

/**
 * 담당자 일괄 등록. dryRun 이면 검사만 하고, 아니면 오류가 없을 때만 등록한다. 등록했으면 목록·건수를 다시 읽는다. 파일을 읽지 못하면 400
 * CONTACT_IMPORT_INVALID_FILE(file 칸).
 */
export function useImportContacts() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      business,
      file,
      dryRun,
    }: {
      business: string;
      file: File;
      dryRun: boolean;
    }) =>
      unwrap(
        await api.POST('/api/v1/contacts/imports', {
          params: { query: { business, dryRun } },
          // OpenAPI 는 파일을 string(binary) 으로 적는다. 실제 전송은 아래 serializer 가 FormData 로 만든다.
          body: { file: file as unknown as string },
          bodySerializer: () => {
            const form = new FormData();
            form.append('file', file);
            return form;
          },
        }),
      ),
    onSuccess: (result) => {
      if (result.saved) invalidate(queryClient);
    },
  });
}

// ---- 정기 변경 주간 ----

export function useUpdateCampaigns(business: string) {
  return useQuery({
    queryKey: campaignKeys.list(business),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/update-campaigns', { params: { query: { business } } })),
    enabled: Boolean(business),
  });
}

/** 연락처 화면 위쪽 안내의 진행 중인 주간(기관 담당자는 자기 기관 현행화까지). */
export function useCurrentUpdateCampaign(business: string) {
  return useQuery({
    queryKey: campaignKeys.current(business),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/update-campaigns/current', { params: { query: { business } } }),
      ),
    enabled: Boolean(business),
  });
}

export function useUpdateCampaignProgress(id: number | null, filter: UpdateCampaignProgressFilter) {
  return useQuery({
    queryKey: campaignKeys.progress(id ?? 0, filter),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/update-campaigns/{id}/progress', {
          params: { path: { id: id! }, query: { filter } },
        }),
      ),
    enabled: id != null,
    placeholderData: keepPreviousData,
  });
}

function invalidateCampaigns(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: campaignKeys.all });
  void queryClient.invalidateQueries({ queryKey: homeKeys.all });
}

export function useSaveUpdateCampaign() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id?: number; body: UpdateCampaignSaveRequest }) =>
      id == null
        ? unwrap(await api.POST('/api/v1/update-campaigns', { body }))
        : unwrap(
            await api.PUT('/api/v1/update-campaigns/{id}', { params: { path: { id } }, body }),
          ),
    onSuccess: () => invalidateCampaigns(queryClient),
  });
}

export function useDeleteUpdateCampaign() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      const result = await api.DELETE('/api/v1/update-campaigns/{id}', {
        params: { path: { id } },
      });
      if (result.error !== undefined || !result.response.ok) {
        throw new ApiError(result.response.status, result.error);
      }
    },
    onSuccess: () => invalidateCampaigns(queryClient),
  });
}

/** 정기 변경 주기(기본 6개월)와 다음 요청일, 리마인드 간격. 결제원 담당자만 본다. */
export function useUpdateCampaignSchedule(business: string) {
  return useQuery({
    queryKey: campaignKeys.schedule(business),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/update-campaigns/schedule', { params: { query: { business } } }),
      ),
    enabled: Boolean(business),
  });
}

/** 리마인드 메일 미리보기: 지금 보내면 나갈 기본 제목·본문과 받을 기관. 기관을 비우면 미완료 기관 전부다. */
export function useCampaignReminderPreview(id: number, organizationIds: number[], enabled = true) {
  return useQuery({
    queryKey: ['update-campaigns', 'reminder-preview', id, organizationIds] as const,
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/update-campaigns/{id}/reminder-preview', {
          params: { path: { id }, query: { organizationIds } },
        }),
      ),
    enabled,
    refetchOnWindowFocus: false,
  });
}

/**
 * 진행 중 주간의 미회신 기관에 리마인드 메일을 지금 보낸다. 기관을 비우면 미완료 기관 전부이고, 제목·본문은 미리보기에서 확인(고친)
 * 문구다.
 */
export function useSendCampaignReminders() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      organizationIds,
      subject,
      body,
    }: {
      id: number;
      organizationIds?: number[];
      subject: string;
      body: string;
    }) =>
      unwrap(
        await api.POST('/api/v1/update-campaigns/{id}/reminders', {
          params: { path: { id } },
          body: { organizationIds: organizationIds ?? null, subject, body },
        }),
      ),
    onSuccess: () => invalidateCampaigns(queryClient),
  });
}
