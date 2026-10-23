import {
  ActionIcon,
  Alert,
  Button,
  Card,
  Checkbox,
  Group,
  Select,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  Textarea,
  TextInput,
  Tooltip,
} from '@mantine/core';
import { DateTimePicker } from '@mantine/dates';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { canViewBusiness, useMe } from '../../account/hooks';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { PendingAttachments } from '../../attachment/components/PendingAttachments';
import { attachmentKeys, uploadAttachmentFile } from '../../attachment/hooks';
import { type BusinessCode, useBusinesses, useCurrentBusiness } from '../../organization/hooks';
import {
  type SuspensionDetail,
  canRegisterSuspensions,
  registersAsKftc,
  useCreateSuspension,
  useSuspension,
  useSuspensionRegistrableOrganizations,
  useUpdateSuspension,
} from '../hooks';

/** DateTimePicker 의 값 꼴. */
const PICKER_FORMAT = 'YYYY-MM-DD HH:mm:ss';

/** 담당자 연락처 한 행. key 는 행을 지우거나 더할 때 입력 칸을 제자리에 두려는 화면 전용 값이다. */
interface ContactDraft {
  key: string;
  name: string;
  phone: string;
  email: string;
}

interface Values {
  businesses: BusinessCode[];
  organizationId: string | null;
  startAt: string | null;
  endAt: string | null;
  reason: string;
  kftcAction: string;
  contacts: ContactDraft[];
  extraNote: string;
}

/** 담당자는 이 수까지 더한다(서버와 같다). */
const MAX_CONTACTS = 10;

let contactKeySeq = 0;
function contactDraft(name = '', phone = '', email = ''): ContactDraft {
  contactKeySeq += 1;
  return { key: `contact-${contactKeySeq}`, name, phone, email };
}

/**
 * 일시중단 등록과 고치기. 주소에 :id 가 있으면 고치기다. 명세의 정해진 양식(일시, 대상 업무, 중단 이유, 결제원 조치사항,
 * 담당자 연락처, 공문, 추가 사항)을 그대로 받는다. 기관 담당자는 자기 기관으로, 결제원 담당자는 기관을 골라 등록한다. 등록은 사람이 아니라 기관
 * 기준이라 대상 업무는 올리는 사람의 담당 업무가 아니라 그 기관이 참여하는 업무 전부에서 고른다(대외계·네트워크 작업은 한 업무에 국한되지 않는다).
 */
export function SuspensionEditorPage() {
  const { business: slug = '', id } = useParams<{ business: string; id?: string }>();
  const suspensionId = id ? Number(id) : null;
  const detail = useSuspension(suspensionId);
  const { data: me } = useMe();

  if (suspensionId == null) {
    if (me && !canRegisterSuspensions(me)) {
      return (
        <Stack gap="md">
          <PageHeader title="일시중단 등록" />
          <StateView
            kind="forbidden"
            title="일시중단을 등록할 수 없습니다"
            description="결제원 담당자나 참가기관 담당자만 등록합니다."
          />
        </Stack>
      );
    }
    return <SuspensionEditor slug={slug} />;
  }
  if (detail.isError) {
    return (
      <Stack gap="md">
        <PageHeader title="일시중단 고치기" />
        <ErrorState error={detail.error} homeLink={`/${slug}/home`} />
      </Stack>
    );
  }
  if (!detail.data) {
    return (
      <Stack gap="md">
        <PageHeader title="일시중단 고치기" />
        <Skeleton h={320} />
      </Stack>
    );
  }
  if (!detail.data.permissions.edit) {
    return (
      <Stack gap="md">
        <PageHeader title="일시중단 고치기" />
        <StateView
          kind="forbidden"
          title="고칠 수 없는 일시중단입니다"
          description="완료한 일시중단, 다른 사람이 올린 일시중단은 고칠 수 없습니다."
        />
      </Stack>
    );
  }
  return <SuspensionEditor slug={slug} existing={detail.data} />;
}

function toPicker(value: string | null | undefined): string | null {
  return value ? dayjs(value).format(PICKER_FORMAT) : null;
}

function SuspensionEditor({ slug, existing }: { slug: string; existing?: SuspensionDetail }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const current = useCurrentBusiness();
  const { data: me } = useMe();
  const { data: allBusinesses } = useBusinesses();
  const kftcSide = registersAsKftc(me);
  const organizations = useSuspensionRegistrableOrganizations();
  const create = useCreateSuspension();
  const update = useUpdateSuspension(existing?.id ?? 0);
  const mutation = existing ? update : create;
  // 새 일시중단의 공문. 저장해 번호가 생긴 뒤 올린다.
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);

  const form = useForm<Values>({
    initialValues: {
      businesses: existing
        ? existing.businesses.map((b) => b.code)
        : current?.code
          ? [current.code]
          : [],
      organizationId: existing ? String(existing.organizationId) : null,
      startAt: toPicker(existing?.startAt),
      endAt: toPicker(existing?.endAt),
      reason: existing?.reason ?? '',
      kftcAction: existing?.kftcAction ?? '',
      // 기관 담당자는 본인이 작업 담당자인 경우가 많아 첫 행에 이름·이메일을 미리 채운다.
      contacts: existing
        ? existing.contacts.map((c) => contactDraft(c.name, c.phone, c.email ?? ''))
        : [contactDraft(kftcSide ? '' : (me?.name ?? ''), '', kftcSide ? '' : (me?.email ?? ''))],
      extraNote: existing?.extraNote ?? '',
    },
  });

  // 대상 업무는 그 기관이 참여하는 업무 전부다. 고치기는 등록한 기관, 결제원은 고른 기관, 기관 담당자는 자기 기관이다.
  const organizationId = existing
    ? existing.organizationId
    : kftcSide
      ? Number(form.values.organizationId)
      : organizations.data?.[0]?.id;
  const selectedOrganization = organizations.data?.find((o) => o.id === organizationId);
  const businessOptions = (allBusinesses ?? [])
    .filter((b) => selectedOrganization?.businesses.includes(b.code))
    .map((b) => ({ value: b.code, label: b.label }));
  const businessCodes = businessOptions.map((b) => b.value).join(',');

  // 기관을 바꾸면 그 기관이 참여하지 않는 업무는 고른 목록에서 뺀다. 남는 업무가 없으면 지금 업무 영역(참여하면)이나 첫 업무를 고른다.
  useEffect(() => {
    if (!selectedOrganization) return;
    const codes = businessCodes.split(',').filter(Boolean) as BusinessCode[];
    const kept = form.values.businesses.filter((b) => codes.includes(b));
    if (kept.length === form.values.businesses.length && kept.length > 0) return;
    if (kept.length > 0) {
      form.setFieldValue('businesses', kept);
      return;
    }
    const fallback = current?.code && codes.includes(current.code) ? current.code : codes[0];
    form.setFieldValue('businesses', fallback ? [fallback] : []);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedOrganization?.id, businessCodes]);

  // 서버 검증 오류는 필드 메시지 그대로 칸 아래에 보인다.
  useEffect(() => {
    if (mutation.isError) form.setErrors(fieldErrorsOf(mutation.error));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mutation.isError, mutation.error]);
  const fieldErrors = mutation.isError ? fieldErrorsOf(mutation.error) : {};
  const generalError =
    mutation.isError && Object.keys(fieldErrors).length === 0 ? mutation.error.message : undefined;

  const uploadPending = async (suspensionId: number) => {
    if (pendingFiles.length === 0) return;
    const owner = { ownerType: 'SUSPENSION' as const, ownerId: suspensionId };
    setUploading(true);
    const failed: string[] = [];
    for (const file of pendingFiles) {
      try {
        await uploadAttachmentFile(owner, file);
      } catch (e) {
        failed.push(`${file.name}${e instanceof ApiError ? `(${e.message})` : ''}`);
      }
    }
    setUploading(false);
    await queryClient.invalidateQueries({ queryKey: attachmentKeys.list(owner) });
    if (failed.length > 0) {
      notifications.show({
        title: '공문 일부를 올리지 못했습니다',
        message: `${failed.join(', ')}. 상세 화면에서 다시 올려 주세요.`,
        color: 'red',
      });
    }
  };

  const submit = (values: Values) => {
    const body = {
      // 비어 있으면 서버가 "대상 업무를 하나 이상 골라 주세요." 를 필드 메시지로 알려 준다. 업무 순서로 보낸다.
      businesses: businessOptions.map((b) => b.value).filter((b) => values.businesses.includes(b)),
      organizationId: values.organizationId ? Number(values.organizationId) : null,
      // 서버는 시간대 없는 일시(서버 시간대 Asia/Seoul)를 받는다. 빈 값은 서버가 필드 메시지로 알려 준다.
      startAt: values.startAt ? dayjs(values.startAt).format('YYYY-MM-DDTHH:mm:ss') : '',
      endAt: values.endAt ? dayjs(values.endAt).format('YYYY-MM-DDTHH:mm:ss') : '',
      reason: values.reason,
      kftcAction: values.kftcAction || null,
      contacts: values.contacts.map((c) => ({
        name: c.name,
        phone: c.phone,
        email: c.email || null,
      })),
      extraNote: values.extraNote || null,
    };
    // 지금 업무 영역이 대상 업무에 들면 그대로, 아니면 맡은 대상 업무 영역에서 연다. 맡은 업무가 없으면 지금 업무 영역에서 연다(상세는 업무 영역과
    // 상관없이 열린다).
    const detailPath = (d: SuspensionDetail) => {
      const slugs = d.businesses.map((b) => b.slug);
      const target = slugs.includes(slug)
        ? slug
        : (slugs.find((s) => me && canViewBusiness(me, s)) ?? slug);
      return `/${target}/suspensions/${d.id}`;
    };
    if (existing) {
      update.mutate(body, { onSuccess: (d) => navigate(detailPath(d)) });
    } else {
      create.mutate(body, {
        onSuccess: async (d) => {
          await uploadPending(d.id);
          navigate(detailPath(d));
        },
      });
    }
  };

  const ownOrganization = !kftcSide ? organizations.data?.[0] : undefined;
  const title = existing ? '일시중단 고치기' : '일시중단 등록';
  return (
    <form onSubmit={form.onSubmit(submit)} noValidate>
      <Stack gap="md">
        <PageHeader
          title={title}
          description={
            existing
              ? kftcSide
                ? '고치면 이 업무 결제원 담당자에게 매터모스트 알림이 갑니다. 기관이 올린 건이면 등록한 사람에게도 알립니다.'
                : '고치면 결제원 담당자에게 매터모스트 알림이 가고, 확인이 시작된 건은 다시 검토 대기로 돌아갑니다.'
              : '정해진 양식으로 등록합니다. 저장하면 검토 대기가 되고 고른 업무의 결제원 담당자(업무·개발·운영)에게 메일과 매터모스트 알림이 갑니다.'
          }
          actions={
            <>
              <Button
                variant="default"
                component={Link}
                to={existing ? `/${slug}/suspensions/${existing.id}` : `/${slug}/suspensions`}
              >
                취소
              </Button>
              <Button type="submit" loading={mutation.isPending || uploading}>
                저장
              </Button>
            </>
          }
        />

        {generalError && (
          <Alert variant="light" color="red" data-testid="suspension-save-error">
            {generalError}
          </Alert>
        )}

        <Card>
          <Stack gap="sm">
            <Text fw={700} size="xl">
              대상과 일시
            </Text>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
              {existing ? (
                <TextInput label="기관" value={existing.organizationName} readOnly />
              ) : kftcSide ? (
                <Select
                  label="기관"
                  placeholder="중단하는 참가기관을 고르세요"
                  required
                  searchable
                  data={(organizations.data ?? []).map((o) => ({
                    value: String(o.id),
                    label: o.name,
                  }))}
                  {...form.getInputProps('organizationId')}
                />
              ) : (
                <TextInput
                  label="기관"
                  value={ownOrganization?.name ?? me?.organizationName ?? ''}
                  readOnly
                />
              )}
              <Checkbox.Group
                label="대상 업무"
                description={
                  selectedOrganization
                    ? '이 기관이 참여하는 업무입니다. 함께 멈추는 업무를 모두 고르세요.'
                    : '기관을 먼저 고르세요.'
                }
                required
                data-testid="suspension-businesses"
                {...form.getInputProps('businesses')}
              >
                <Group gap="md" mt={6}>
                  {businessOptions.map((b) => (
                    <Checkbox key={b.value} value={b.value} label={b.label} />
                  ))}
                </Group>
              </Checkbox.Group>
              <DateTimePicker
                label="중단 시작"
                required
                valueFormat="YYYY-MM-DD HH:mm"
                placeholder="날짜와 시각"
                {...form.getInputProps('startAt')}
              />
              <DateTimePicker
                label="중단 종료"
                required
                valueFormat="YYYY-MM-DD HH:mm"
                placeholder="날짜와 시각"
                {...form.getInputProps('endAt')}
              />
            </SimpleGrid>
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <Text fw={700} size="xl">
              중단 내용
            </Text>
            <Textarea
              label="중단 이유(작업 내용)"
              placeholder="무슨 작업으로 어떤 거래가 멈추는지 적어 주세요."
              required
              autosize
              minRows={3}
              maxLength={2000}
              {...form.getInputProps('reason')}
            />
            <Textarea
              label="결제원 조치사항"
              description="중단 동안 결제원이 해 주어야 할 일입니다. 예: 조회 요청 차단, 고객 안내 문구"
              autosize
              minRows={2}
              maxLength={2000}
              {...form.getInputProps('kftcAction')}
            />
            <Textarea
              label="추가 사항"
              autosize
              minRows={2}
              maxLength={2000}
              {...form.getInputProps('extraNote')}
            />
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                담당자 연락처
              </Text>
              <Text size="sm" c="dimmed">
                작업 중 연락할 사람을 모두 적습니다. 첫 행부터 연락할 순서입니다.
              </Text>
            </div>
            {form.values.contacts.map((contact, index) => (
              <Group
                key={contact.key}
                align="flex-start"
                gap="sm"
                wrap="nowrap"
                data-testid="suspension-contact-row"
              >
                <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md" style={{ flex: 1 }}>
                  <TextInput
                    label="이름"
                    required
                    maxLength={50}
                    {...form.getInputProps(`contacts.${index}.name`)}
                  />
                  <TextInput
                    label="연락처"
                    placeholder="010-0000-0000"
                    required
                    maxLength={30}
                    {...form.getInputProps(`contacts.${index}.phone`)}
                  />
                  <TextInput
                    label="이메일"
                    placeholder="name@example.com"
                    maxLength={200}
                    {...form.getInputProps(`contacts.${index}.email`)}
                  />
                </SimpleGrid>
                <Tooltip label="행 빼기">
                  <ActionIcon
                    variant="subtle"
                    color="red"
                    mt={28}
                    disabled={form.values.contacts.length === 1}
                    onClick={() => form.removeListItem('contacts', index)}
                    aria-label={`담당자 ${index + 1} 빼기`}
                  >
                    <IconTrash size={18} stroke={1.8} />
                  </ActionIcon>
                </Tooltip>
              </Group>
            ))}
            <Group>
              <Button
                variant="light"
                leftSection={<IconPlus size={16} stroke={1.8} />}
                disabled={form.values.contacts.length >= MAX_CONTACTS}
                onClick={() => form.insertListItem('contacts', contactDraft())}
              >
                행 추가
              </Button>
            </Group>
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                공문
              </Text>
              <Text size="sm" c="dimmed">
                선택입니다. 일시중단을 알리는 공문 파일을 붙입니다.
              </Text>
            </div>
            {existing ? (
              <AttachmentPanel
                ownerType="SUSPENSION"
                ownerId={existing.id}
                editable
                uploadLabel="공문 올리기"
                emptyMessage="올린 공문이 없습니다."
              />
            ) : (
              <PendingAttachments
                files={pendingFiles}
                onChange={setPendingFiles}
                pickLabel="공문 고르기"
              />
            )}
          </Stack>
        </Card>
      </Stack>
    </form>
  );
}
