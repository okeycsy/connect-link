import {
  ActionIcon,
  Alert,
  Button,
  Card,
  Group,
  Skeleton,
  Stack,
  Text,
  Textarea,
  TextInput,
} from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useForm } from '@mantine/form';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import dayjs from 'dayjs';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { fieldErrorsOf } from '../../../api/client';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { useCurrentBusiness } from '../../organization/hooks';
import { type TargetValue, TargetPicker } from '../components/TargetPicker';
import {
  type OpinionRequestDetail,
  useCreateOpinionRequest,
  useOpinionRequest,
  useRecipientCandidates,
  useUpdateOpinionRequest,
} from '../hooks';

interface FormRow {
  /** 화면 안 키. 저장한 양식은 f{id}, 새 양식은 n{번호}. */
  key: string;
  id: number | null;
  name: string;
  description: string;
}

interface Values {
  title: string;
  content: string;
  dueDate: string;
  forms: FormRow[];
}

/** 새 의견 요청과 요청 고치기. 주소에 :id 가 있으면 고치기다. 첨부자료와 양식 파일은 저장한 뒤 상세에서 올린다. */
export function OpinionEditorPage() {
  const { business: slug = '', id } = useParams<{ business: string; id?: string }>();
  const requestId = id ? Number(id) : null;
  const detail = useOpinionRequest(requestId);

  if (requestId == null) return <OpinionEditor slug={slug} />;
  if (detail.isError) {
    return (
      <Stack gap="md">
        <PageHeader title="의견 요청 고치기" />
        <ErrorState error={detail.error} homeLink={`/${slug}/home`} />
      </Stack>
    );
  }
  if (!detail.data) {
    return (
      <Stack gap="md">
        <PageHeader title="의견 요청 고치기" />
        <Skeleton h={320} />
      </Stack>
    );
  }
  if (!detail.data.permissions.edit) {
    return (
      <Stack gap="md">
        <PageHeader title="의견 요청 고치기" />
        <StateView
          kind="forbidden"
          title="고칠 수 없는 요청입니다"
          description="마감했거나 취소한 요청, 다른 업무의 요청은 고칠 수 없습니다."
        />
      </Stack>
    );
  }
  return <OpinionEditor slug={slug} existing={detail.data} />;
}

function OpinionEditor({ slug, existing }: { slug: string; existing?: OpinionRequestDetail }) {
  const navigate = useNavigate();
  const current = useCurrentBusiness();
  const candidates = useRecipientCandidates(slug);
  const create = useCreateOpinionRequest();
  const update = useUpdateOpinionRequest(existing?.id ?? 0);
  const mutation = existing ? update : create;
  const nextKey = useRef(0);

  const form = useForm<Values>({
    initialValues: {
      title: existing?.title ?? '',
      content: existing?.content ?? '',
      dueDate: existing?.dueDate ?? dayjs().add(7, 'day').format('YYYY-MM-DD'),
      forms:
        existing?.forms.map((f) => ({
          key: `f${f.id}`,
          id: f.id,
          name: f.name,
          description: f.description ?? '',
        })) ?? [],
    },
  });
  const [targets, setTargets] = useState<TargetValue[]>(
    () =>
      existing?.targets.map((t) => ({
        organizationId: t.organizationId,
        formKey: t.formId == null ? null : `f${t.formId}`,
        recipientAccountIds: t.recipients.map((r) => r.accountId),
      })) ?? [],
  );
  const locked = useMemo(
    () =>
      new Set(
        existing?.targets.filter((t) => t.replyStatus != null).map((t) => t.organizationId) ?? [],
      ),
    [existing],
  );

  // 서버 검증 오류는 필드 메시지 그대로 칸 아래에 보인다.
  useEffect(() => {
    if (mutation.isError) form.setErrors(fieldErrorsOf(mutation.error));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mutation.isError, mutation.error]);
  const fieldErrors = mutation.isError ? fieldErrorsOf(mutation.error) : {};
  const knownFields = ['title', 'content', 'dueDate'];
  const generalError =
    mutation.isError &&
    (Object.keys(fieldErrors).length === 0 ||
      Object.keys(fieldErrors).some((k) => !knownFields.includes(k) && !k.startsWith('forms.')))
      ? mutation.error.message
      : undefined;

  const removeForm = (index: number) => {
    const key = form.values.forms[index].key;
    form.removeListItem('forms', index);
    setTargets((ts) => ts.map((t) => (t.formKey === key ? { ...t, formKey: null } : t)));
  };

  const submit = (values: Values) => {
    const names = new Map(values.forms.map((f) => [f.key, f.name.trim()]));
    const body = {
      title: values.title,
      content: values.content,
      dueDate: values.dueDate,
      forms: values.forms.map((f) => ({
        id: f.id,
        name: f.name,
        description: f.description.trim() || null,
      })),
      targets: targets.map((t) => ({
        organizationId: t.organizationId,
        formName: t.formKey ? (names.get(t.formKey) ?? null) : null,
        recipientAccountIds: t.recipientAccountIds,
      })),
    };
    if (existing) {
      update.mutate(body, { onSuccess: (d) => navigate(`/${slug}/opinions/${d.id}`) });
    } else {
      create.mutate(
        { ...body, business: current?.code ?? 'ACCOUNT_INFO' },
        { onSuccess: (d) => navigate(`/${slug}/opinions/${d.id}`) },
      );
    }
  };

  const title = existing ? '의견 요청 고치기' : '새 의견 요청';
  return (
    <form onSubmit={form.onSubmit(submit)} noValidate>
      <Stack gap="md">
        <PageHeader
          title={title}
          description={
            existing?.status === 'OPEN'
              ? '회신중인 요청입니다. 저장하면 새로 넣은 담당자에게 요청 메일이, 남은 담당자에게 변경 알림이 갑니다.'
              : `${current?.label ?? '이 업무'} 참가기관에 보낼 의견 요청입니다. 저장하면 작성중으로 남고, 상세에서 첨부자료를 올린 뒤 회신 요청을 시작합니다.`
          }
          actions={
            <>
              <Button
                variant="default"
                component={Link}
                to={existing ? `/${slug}/opinions/${existing.id}` : `/${slug}/opinions`}
              >
                취소
              </Button>
              <Button type="submit" loading={mutation.isPending}>
                저장
              </Button>
            </>
          }
        />

        {generalError && (
          <Alert variant="light" color="red" data-testid="opinion-save-error">
            {generalError}
          </Alert>
        )}

        <Card>
          <Stack gap="sm">
            <Text fw={700} size="xl">
              요청 내용
            </Text>
            <TextInput
              label="요청명"
              placeholder="예: 수수료 체계 개편 의견 요청"
              required
              maxLength={200}
              {...form.getInputProps('title')}
            />
            <Textarea
              label="요청 목적·내용"
              placeholder="무엇을 왜 묻는지, 회신할 때 볼 점을 적어 주세요."
              required
              autosize
              minRows={5}
              maxLength={4000}
              {...form.getInputProps('content')}
            />
            <Group gap="md" align="flex-start">
              <TextInput
                label="해당 업무"
                value={existing?.businessLabel ?? current?.label ?? ''}
                readOnly
                w={220}
              />
              <DatePickerInput
                label="회신 기한"
                required
                w={220}
                valueFormat="YYYY-MM-DD"
                minDate={dayjs().format('YYYY-MM-DD')}
                {...form.getInputProps('dueDate')}
              />
            </Group>
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <Group justify="space-between">
              <div>
                <Text fw={700} size="xl">
                  업권별 회신 양식
                </Text>
                <Text size="sm" c="dimmed">
                  업권마다 양식을 정하고 대상 기관마다 하나를 고릅니다. 양식 파일은 저장한 뒤
                  상세에서 올립니다. 양식이 없으면 기관은 의견만 적어 회신합니다.
                </Text>
              </div>
              <Button
                variant="light"
                leftSection={<IconPlus size={16} stroke={1.8} />}
                onClick={() =>
                  form.insertListItem('forms', {
                    key: `n${nextKey.current++}`,
                    id: null,
                    name: '',
                    description: '',
                  })
                }
              >
                양식 추가
              </Button>
            </Group>
            {form.values.forms.length === 0 && (
              <Text size="sm" c="dimmed">
                정한 양식이 없습니다.
              </Text>
            )}
            {form.values.forms.map((f, index) => (
              <Group key={f.key} gap="sm" align="flex-start" wrap="nowrap">
                <TextInput
                  label={index === 0 ? '양식 이름' : undefined}
                  aria-label="양식 이름"
                  placeholder="예: 은행권"
                  w={200}
                  maxLength={50}
                  {...form.getInputProps(`forms.${index}.name`)}
                />
                <TextInput
                  label={index === 0 ? '작성 안내' : undefined}
                  aria-label="작성 안내"
                  placeholder="예: 시트 2의 항목을 모두 채워 주세요."
                  flex={1}
                  maxLength={500}
                  {...form.getInputProps(`forms.${index}.description`)}
                />
                <ActionIcon
                  variant="subtle"
                  color="red"
                  mt={index === 0 ? 26 : 4}
                  aria-label={`${f.name || '양식'} 빼기`}
                  onClick={() => removeForm(index)}
                >
                  <IconTrash size={18} stroke={1.8} />
                </ActionIcon>
              </Group>
            ))}
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                대상 기관과 회신 담당자
              </Text>
              <Text size="sm" c="dimmed">
                회신 담당자에게 요청·리마인드 메일이 갑니다. 대상 기관의 같은 업무 담당자는 누구나
                요청을 보고 회신할 수 있습니다.
              </Text>
            </div>
            <TargetPicker
              candidates={candidates.data}
              loading={candidates.isPending}
              error={candidates.error}
              value={targets}
              onChange={setTargets}
              forms={form.values.forms.map((f) => ({ key: f.key, name: f.name }))}
              lockedOrganizationIds={locked}
            />
          </Stack>
        </Card>
      </Stack>
    </form>
  );
}
