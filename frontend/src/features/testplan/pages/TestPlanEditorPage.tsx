import {
  ActionIcon,
  Alert,
  Button,
  Card,
  Checkbox,
  Group,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  Textarea,
  TextInput,
  Tooltip,
} from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconArrowDown, IconArrowUp, IconPlus, IconTrash } from '@tabler/icons-react';
import { useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { useMe } from '../../account/hooks';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { PendingAttachments } from '../../attachment/components/PendingAttachments';
import { attachmentKeys, uploadAttachmentFile } from '../../attachment/hooks';
import { useCurrentBusiness } from '../../organization/hooks';
import { TargetOrgPicker } from '../components/TargetOrgPicker';
import {
  type TestPlanDetail,
  canManageTestPlans,
  useCreateTestPlan,
  useTestPlan,
  useTestPlanOrganizations,
  useUpdateTestPlan,
} from '../hooks';

interface ItemDraft {
  /** 화면 안에서만 쓰는 열쇠. */
  key: string;
  /** 기존 요구사항이면 id, 새로 넣으면 null. */
  id: number | null;
  title: string;
  description: string;
}

interface Values {
  subBusiness: string;
  title: string;
  description: string;
  startDate: string | null;
  endDate: string | null;
  items: ItemDraft[];
  /** 수행 전에 기관별 테스트데이터를 받는지. */
  testDataRequested: boolean;
  testDataDueDate: string | null;
  testDataInstruction: string;
}

/**
 * 기관 테스트 만들기와 고치기. 주소에 :id 가 있으면 고치기다. 단위업무/개발 건, 테스트 내용과 수행 기간, 요구사항(번호 001, 002 … 순서),
 * 대상 참가기관, 가이드·참고자료를 받는다. 테스트데이터는 수행 전에 받으므로 그 요청(회신 기한·요청 사항·회신 양식)을 맨 위에 둔다. 저장하면 바로 대상 기관에 열리고 대상 기관 담당자에게 메일이 간다.
 * 만들기 주소에 `?copyFrom=테스트 ID` 가 있으면 그 테스트의 단위업무·내용·요구사항·대상 기관·테스트데이터 요청 사항을 채워 시작한다(복사해서 만들기,
 * 기간·첨부는 새로).
 */
export function TestPlanEditorPage() {
  const { business: slug = '', id } = useParams<{ business: string; id?: string }>();
  const [searchParams] = useSearchParams();
  const copyFromId = id ? null : Number(searchParams.get('copyFrom')) || null;
  const planId = id ? Number(id) : null;
  const detail = useTestPlan(planId ?? copyFromId);
  const { data: me } = useMe();
  const title =
    planId != null
      ? '기관 테스트 고치기'
      : copyFromId != null
        ? '복사해서 기관 테스트 만들기'
        : '기관 테스트 만들기';

  if (me && !canManageTestPlans(me, slug)) {
    return (
      <Stack gap="md">
        <PageHeader title={title} />
        <StateView
          kind="forbidden"
          title="기관 테스트를 만들거나 고칠 수 없습니다"
          description="이 업무를 맡은 결제원 담당자만 기관 테스트를 만들고 고칩니다."
        />
      </Stack>
    );
  }
  if (planId == null && copyFromId == null) return <TestPlanEditor slug={slug} />;
  if (detail.isError) {
    return (
      <Stack gap="md">
        <PageHeader title={title} />
        <ErrorState error={detail.error} homeLink={`/${slug}/home`} />
      </Stack>
    );
  }
  if (!detail.data) {
    return (
      <Stack gap="md">
        <PageHeader title={title} />
        <Skeleton h={320} />
      </Stack>
    );
  }
  if (copyFromId != null) {
    return <TestPlanEditor key={copyFromId} slug={slug} copyFrom={detail.data} />;
  }
  return <TestPlanEditor slug={slug} existing={detail.data} />;
}

function TestPlanEditor({
  slug,
  existing,
  copyFrom,
}: {
  slug: string;
  existing?: TestPlanDetail;
  /** 복사해서 만들 때 원래 테스트. 저장하면 새 테스트로 바로 열린다. */
  copyFrom?: TestPlanDetail;
}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const current = useCurrentBusiness();
  const organizations = useTestPlanOrganizations(slug);
  const create = useCreateTestPlan();
  const update = useUpdateTestPlan(existing?.id ?? 0);
  const mutation = existing ? update : create;
  const nextKey = useRef(0);
  const newKey = () => `n${nextKey.current++}`;
  // 새 테스트의 가이드·참고자료. 저장해 번호가 생긴 뒤 올린다.
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [pendingFormFiles, setPendingFormFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const source = existing ?? copyFrom;
  const [organizationIds, setOrganizationIds] = useState<number[]>(
    () => source?.targets.map((t) => t.organizationId) ?? [],
  );
  const locked = useMemo(
    () =>
      new Set(
        existing?.targets.filter((t) => t.unreported < t.itemCount).map((t) => t.organizationId) ??
          [],
      ),
    [existing],
  );

  const form = useForm<Values>({
    initialValues: {
      subBusiness: source?.subBusiness ?? '',
      title: source?.title ?? '',
      description: source?.description ?? '',
      startDate: existing?.startDate ?? dayjs().format('YYYY-MM-DD'),
      endDate: existing?.endDate ?? dayjs().add(14, 'day').format('YYYY-MM-DD'),
      // 새 테스트는 요청 칸을 펼쳐 둔다. 필요 없으면 "필요 없음" 을 체크해 넘어간다.
      testDataRequested: source ? source.testDataRequest != null : true,
      testDataDueDate:
        existing?.testDataRequest?.dueDate ?? existing?.startDate ?? dayjs().format('YYYY-MM-DD'),
      testDataInstruction: source?.testDataRequest?.instruction ?? '',
      // 복사하면 요구사항은 새 요구사항이다(ID 없이 내용만).
      items: source?.items.map((i) => ({
        key: `i${i.id}`,
        id: existing ? i.id : null,
        title: i.title,
        description: i.description ?? '',
      })) ?? [{ key: 'n-first', id: null, title: '', description: '' }],
    },
  });

  // 서버 검증 오류는 필드 메시지 그대로 칸 아래에 보인다.
  useEffect(() => {
    if (mutation.isError) form.setErrors(fieldErrorsOf(mutation.error));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mutation.isError, mutation.error]);
  const fieldErrors = mutation.isError ? fieldErrorsOf(mutation.error) : {};
  const knownFields = [
    'subBusiness',
    'title',
    'description',
    'startDate',
    'endDate',
    'testDataRequest.dueDate',
    'testDataRequest.instruction',
  ];
  const generalError =
    mutation.isError &&
    (Object.keys(fieldErrors).length === 0 ||
      Object.keys(fieldErrors).some(
        (k) => !knownFields.includes(k) && !/^items\.\d+\.(title|description)$/.test(k),
      ))
      ? (fieldErrors.items ?? fieldErrors.organizationIds ?? mutation.error.message)
      : undefined;

  /** 저장 뒤 고른 파일을 올린다. 가이드(TEST_PLAN)와 테스트데이터 회신 양식(TEST_DATA_FORM)이 같은 길을 쓴다. */
  const uploadPending = async (
    ownerType: 'TEST_PLAN' | 'TEST_DATA_FORM',
    planId: number,
    files: File[],
    what: string,
  ) => {
    if (files.length === 0) return;
    const owner = { ownerType, ownerId: planId };
    setUploading(true);
    const failed: string[] = [];
    for (const file of files) {
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
        title: `${what} 일부를 올리지 못했습니다`,
        message: `${failed.join(', ')}. 상세 화면에서 다시 올려 주세요.`,
        color: 'red',
      });
    }
  };

  const submit = (values: Values) => {
    const body = {
      business: existing?.business ?? current?.code ?? 'ACCOUNT_INFO',
      subBusiness: values.subBusiness,
      title: values.title,
      description: values.description || null,
      // 빈 날짜는 서버가 필드 메시지로 알려 준다.
      startDate: values.startDate ?? '',
      endDate: values.endDate ?? '',
      items: values.items.map((i) => ({
        id: i.id,
        title: i.title,
        description: i.description || null,
      })),
      organizationIds,
      testDataRequest: values.testDataRequested
        ? {
            // 빈 날짜는 서버가 필드 메시지로 알려 준다.
            dueDate: values.testDataDueDate ?? '',
            instruction: values.testDataInstruction.trim() || null,
          }
        : null,
    };
    if (existing) {
      update.mutate(body, { onSuccess: (d) => navigate(`/${slug}/tests/${d.id}`) });
    } else {
      create.mutate(body, {
        onSuccess: async (d) => {
          await uploadPending('TEST_PLAN', d.id, pendingFiles, '가이드');
          if (values.testDataRequested) {
            await uploadPending('TEST_DATA_FORM', d.id, pendingFormFiles, '테스트데이터 회신 양식');
          }
          navigate(`/${slug}/tests/${d.id}`);
        },
      });
    }
  };

  const items = form.values.items;
  return (
    <form onSubmit={form.onSubmit(submit)} noValidate>
      <Stack gap="md">
        <PageHeader
          title={
            existing
              ? '기관 테스트 고치기'
              : copyFrom
                ? '복사해서 기관 테스트 만들기'
                : '기관 테스트 만들기'
          }
          description={
            existing
              ? '내용·요구사항·대상 기관을 고칩니다. 결과를 등록한 요구사항과 기관은 뺄 수 없습니다. 새로 더한 기관 담당자에게 메일이 갑니다.'
              : copyFrom
                ? `"${copyFrom.title}" 의 내용·요구사항·대상 기관을 채웠습니다. 수행 기간과 첨부는 새로 정하고, 저장하면 바로 대상 기관에 열립니다.`
                : '저장하면 바로 대상 기관에 열리고 대상 기관 담당자에게 메일이 갑니다. 만든 사람이 결과 등록 알림 대상이 됩니다.'
          }
          actions={
            <>
              <Button
                variant="default"
                component={Link}
                to={existing ? `/${slug}/tests/${existing.id}` : `/${slug}/tests`}
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
          <Alert variant="light" color="red" data-testid="test-plan-save-error">
            {generalError}
          </Alert>
        )}

        <Card data-testid="test-data-request">
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                테스트데이터 사전 요청
              </Text>
              <Text size="sm" c="dimmed">
                수행 기간 전에 기관마다 테스트데이터를 만들어 올리게 합니다. 저장하면 대상 기관
                담당자에게 요청 메일이 갑니다.
              </Text>
            </div>
            <Checkbox
              label="이 테스트는 테스트데이터 사전 요청이 필요 없습니다"
              checked={!form.values.testDataRequested}
              onChange={(e) => form.setFieldValue('testDataRequested', !e.currentTarget.checked)}
            />
            {!form.values.testDataRequested ? (
              <Text size="sm" c="dimmed" data-testid="test-data-skipped">
                사전 요청 없이 바로 수행합니다. 기관에는 테스트데이터 칸이 보이지 않습니다.
              </Text>
            ) : (
              <>
                <DatePickerInput
                  label="회신 기한"
                  description={`수행 시작일(${form.values.startDate ?? '-'})과 같거나 앞이어야 합니다.`}
                  required
                  w={{ base: '100%', sm: 260 }}
                  valueFormat="YYYY-MM-DD"
                  {...form.getInputProps('testDataDueDate')}
                  error={form.errors.testDataDueDate ?? fieldErrors['testDataRequest.dueDate']}
                />
                <Textarea
                  label="요청 사항"
                  placeholder="예: 해지 예정 계좌를 포함해 테스트 계좌 10개 이상을 만들고 양식에 계좌번호·상태를 적어 주세요."
                  autosize
                  minRows={2}
                  maxLength={2000}
                  {...form.getInputProps('testDataInstruction')}
                  error={
                    form.errors.testDataInstruction ?? fieldErrors['testDataRequest.instruction']
                  }
                />
                <div>
                  <Text size="sm" fw={500} mb={4}>
                    회신 양식
                  </Text>
                  <Text size="xs" c="dimmed" mb={6}>
                    선택입니다. 기관은 이 양식을 내려받아 채운 뒤 테스트데이터로 올립니다.
                  </Text>
                  {existing ? (
                    <AttachmentPanel
                      ownerType="TEST_DATA_FORM"
                      ownerId={existing.id}
                      editable
                      uploadLabel="양식 올리기"
                      emptyMessage="지정한 양식이 없습니다."
                    />
                  ) : (
                    <PendingAttachments
                      files={pendingFormFiles}
                      onChange={setPendingFormFiles}
                      pickLabel="양식 고르기"
                    />
                  )}
                </div>
              </>
            )}
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <Text fw={700} size="xl">
              개발 건
            </Text>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
              <TextInput
                label="해당 업무"
                value={existing?.businessLabel ?? current?.label ?? ''}
                readOnly
              />
              <TextInput
                label="단위업무"
                placeholder="예: 본인계좌 일괄지급정지"
                required
                maxLength={200}
                {...form.getInputProps('subBusiness')}
              />
            </SimpleGrid>
            <TextInput
              label="개발 건 이름"
              placeholder="예: 본인계좌 일괄지급정지 전문 개선 2차"
              required
              maxLength={200}
              {...form.getInputProps('title')}
            />
            <Textarea
              label="테스트 내용·수행 가이드"
              placeholder="무엇을 바꿨고 기관이 어떻게 확인하면 되는지 적어 주세요."
              autosize
              minRows={3}
              maxLength={4000}
              {...form.getInputProps('description')}
            />
            <Group gap="md" align="flex-start">
              <DatePickerInput
                label="수행 시작일"
                required
                w={200}
                valueFormat="YYYY-MM-DD"
                {...form.getInputProps('startDate')}
              />
              <DatePickerInput
                label="수행 종료일"
                required
                w={200}
                valueFormat="YYYY-MM-DD"
                {...form.getInputProps('endDate')}
              />
            </Group>
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                요구사항
              </Text>
              <Text size="sm" c="dimmed">
                기관이 요구사항마다 결과(정상·오류·수행중·미수행)를 등록합니다. 번호는 순서대로
                붙습니다.
              </Text>
            </div>
            {items.map((item, index) => (
              <Card key={item.key} bg="var(--cl-sunken)" bd="none" p="md" data-testid="item-card">
                <Group gap="sm" align="flex-start" wrap="nowrap">
                  <Text fw={700} mt={30} w={36} style={{ flexShrink: 0 }}>
                    {String(index + 1).padStart(3, '0')}
                  </Text>
                  <Stack gap="xs" flex={1}>
                    <TextInput
                      label="요구사항"
                      placeholder="예: 지급정지 등록 전문 응답 확인"
                      required
                      maxLength={500}
                      {...form.getInputProps(`items.${index}.title`)}
                    />
                    <Textarea
                      label="확인 사항"
                      placeholder="기대 결과, 수행 방법 등"
                      autosize
                      minRows={1}
                      maxLength={2000}
                      {...form.getInputProps(`items.${index}.description`)}
                    />
                  </Stack>
                  <Group gap={2} mt={28} wrap="nowrap">
                    <Tooltip label="위로">
                      <ActionIcon
                        variant="subtle"
                        color="gray"
                        disabled={index === 0}
                        onClick={() =>
                          form.reorderListItem('items', { from: index, to: index - 1 })
                        }
                        aria-label={`${index + 1}번 위로`}
                      >
                        <IconArrowUp size={18} stroke={1.8} />
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label="아래로">
                      <ActionIcon
                        variant="subtle"
                        color="gray"
                        disabled={index === items.length - 1}
                        onClick={() =>
                          form.reorderListItem('items', { from: index, to: index + 1 })
                        }
                        aria-label={`${index + 1}번 아래로`}
                      >
                        <IconArrowDown size={18} stroke={1.8} />
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label="빼기">
                      <ActionIcon
                        variant="subtle"
                        color="red"
                        disabled={items.length === 1}
                        onClick={() => form.removeListItem('items', index)}
                        aria-label={`${index + 1}번 빼기`}
                      >
                        <IconTrash size={18} stroke={1.8} />
                      </ActionIcon>
                    </Tooltip>
                  </Group>
                </Group>
              </Card>
            ))}
            <Group>
              <Button
                variant="light"
                leftSection={<IconPlus size={16} stroke={1.8} />}
                onClick={() =>
                  form.insertListItem('items', {
                    key: newKey(),
                    id: null,
                    title: '',
                    description: '',
                  } satisfies ItemDraft)
                }
              >
                요구사항 더하기
              </Button>
            </Group>
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                대상 기관
              </Text>
              <Text size="sm" c="dimmed">
                이 업무에 참여하는 참가기관 중에서 고릅니다. 기관 담당자는 자기 기관 결과만 보고
                등록합니다.
              </Text>
            </div>
            <TargetOrgPicker
              organizations={organizations.data}
              loading={organizations.isPending}
              error={organizations.error}
              value={organizationIds}
              onChange={setOrganizationIds}
              lockedIds={locked}
            />
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                가이드·참고자료
              </Text>
              <Text size="sm" c="dimmed">
                선택입니다. 테스트 시나리오, 전문 규격 같은 파일을 대상 기관과 함께 봅니다.
              </Text>
            </div>
            {existing ? (
              <AttachmentPanel
                ownerType="TEST_PLAN"
                ownerId={existing.id}
                editable
                uploadLabel="파일 올리기"
                emptyMessage="올린 파일이 없습니다."
              />
            ) : (
              <PendingAttachments
                files={pendingFiles}
                onChange={setPendingFiles}
                pickLabel="파일 고르기"
              />
            )}
          </Stack>
        </Card>
      </Stack>
    </form>
  );
}
