import {
  ActionIcon,
  Alert,
  Button,
  Card,
  FileButton,
  Group,
  Skeleton,
  Stack,
  Text,
  Textarea,
  TextInput,
  ThemeIcon,
} from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconFile, IconTrash, IconUpload } from '@tabler/icons-react';
import { useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { formatBytes } from '../../../shared/format';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { attachmentKeys, uploadAttachmentFile } from '../../attachment/hooks';
import { useCurrentBusiness } from '../../organization/hooks';
import { QuestionBuilder } from '../components/QuestionBuilder';
import { type TargetValue, TargetPicker } from '../components/TargetPicker';
import {
  type OpinionRequestDetail,
  useCreateOpinionRequest,
  useOpinionRequest,
  useRecipientCandidates,
  useUpdateOpinionRequest,
} from '../hooks';
import { isChoice, type QuestionDraft } from '../questions';

interface Values {
  title: string;
  content: string;
  dueDate: string;
  questions: QuestionDraft[];
}

/**
 * 새 의견 요청과 요청 고치기. 주소에 :id 가 있으면 고치기다. 첨부자료는 여기서 고르고, 새 요청이면 저장한 뒤 바로 올린다.
 * 질문은 작성중일 때만 고친다(회신을 받기 시작하면 답과 어긋나지 않게 서버가 막는다).
 */
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
  const queryClient = useQueryClient();
  const nextKey = useRef(0);
  const newKey = () => `n${nextKey.current++}`;
  const questionsLocked = existing != null && existing.status !== 'DRAFT';
  // 새 요청의 첨부자료. 요청을 저장해 번호가 생긴 뒤 올린다.
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);

  const form = useForm<Values>({
    initialValues: {
      title: existing?.title ?? '',
      content: existing?.content ?? '',
      dueDate: existing?.dueDate ?? dayjs().add(7, 'day').format('YYYY-MM-DD'),
      questions: existing?.questions.map((q) => ({
        key: `q${q.id}`,
        type: q.type,
        title: q.title,
        required: q.required,
        options: q.options.map((o) => o.label),
      })) ?? [
        // 새 요청은 빈 객관식 질문 하나로 시작한다. 질문이 있어야 회신 요청을 시작할 수 있다.
        { key: 'n-first', type: 'SINGLE_CHOICE', title: '', required: true, options: ['', ''] },
      ],
    },
  });
  const [targets, setTargets] = useState<TargetValue[]>(
    () =>
      existing?.targets.map((t) => ({
        organizationId: t.organizationId,
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
      Object.keys(fieldErrors).some((k) => !knownFields.includes(k) && !k.startsWith('questions.')))
      ? mutation.error.message
      : undefined;

  // 새 요청을 저장한 뒤 고른 첨부자료를 올린다. 못 올린 파일은 알리고 상세에서 다시 올리게 한다.
  const uploadPending = async (requestId: number) => {
    if (pendingFiles.length === 0) return;
    const owner = { ownerType: 'OPINION_REQUEST' as const, ownerId: requestId };
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
        title: '첨부자료 일부를 올리지 못했습니다',
        message: `${failed.join(', ')}. 상세 화면에서 다시 올려 주세요.`,
        color: 'red',
      });
    }
  };

  const submit = (values: Values) => {
    const body = {
      title: values.title,
      content: values.content,
      dueDate: values.dueDate,
      questions: values.questions.map((q) => ({
        type: q.type,
        title: q.title,
        required: q.required,
        options: isChoice(q.type) ? q.options : [],
      })),
      targets: targets.map((t) => ({
        organizationId: t.organizationId,
        recipientAccountIds: t.recipientAccountIds,
      })),
    };
    if (existing) {
      update.mutate(body, { onSuccess: (d) => navigate(`/${slug}/opinions/${d.id}`) });
    } else {
      create.mutate(
        { ...body, business: current?.code ?? 'ACCOUNT_INFO' },
        {
          onSuccess: async (d) => {
            await uploadPending(d.id);
            navigate(`/${slug}/opinions/${d.id}`);
          },
        },
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
              : `${current?.label ?? '이 업무'} 참가기관에 보낼 의견 요청입니다. 저장하면 작성중으로 남고, 상세에서 회신 요청을 시작합니다.`
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
              <Button type="submit" loading={mutation.isPending || uploading}>
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
            <div>
              <Text fw={700} size="xl">
                첨부자료
              </Text>
              <Text size="sm" c="dimmed">
                개편안, 회신 양식 파일처럼 기관이 보고 회신할 자료입니다. 대상 기관 담당자가
                내려받습니다.
              </Text>
            </div>
            {existing ? (
              <AttachmentPanel
                ownerType="OPINION_REQUEST"
                ownerId={existing.id}
                editable
                uploadLabel="첨부자료 올리기"
                emptyMessage="올린 첨부자료가 없습니다."
              />
            ) : (
              <PendingFiles files={pendingFiles} onChange={setPendingFiles} />
            )}
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                질문
              </Text>
              <Text size="sm" c="dimmed">
                {questionsLocked
                  ? '회신을 받기 시작한 요청이라 질문은 고칠 수 없습니다.'
                  : '기관에 물을 내용을 질문으로 나눠 넣습니다. 객관식(하나 고르기), 체크박스(여러 개 고르기), 주관식(글로 답하기) 중에서 고릅니다. 기관은 이 질문에만 답해 회신합니다. 회신 요청을 시작하려면 질문이 하나 이상 있어야 합니다.'}
              </Text>
            </div>
            <QuestionBuilder form={form} locked={questionsLocked} newKey={newKey} />
          </Stack>
        </Card>

        <Card>
          <Stack gap="sm">
            <div>
              <Text fw={700} size="xl">
                대상 기관과 회신 담당자
              </Text>
              <Text size="sm" c="dimmed">
                {current?.label ?? '이 업무'} 담당자만 후보입니다. 업무 전체, 세부업무, 업권으로
                묶어 고르고 한 명씩 더하거나 뺍니다. 회신 담당자에게 요청·리마인드 메일이 가고, 대상
                기관의 같은 업무 담당자는 누구나 요청을 보고 회신할 수 있습니다.
              </Text>
            </div>
            <TargetPicker
              candidates={candidates.data}
              loading={candidates.isPending}
              error={candidates.error}
              value={targets}
              onChange={setTargets}
              lockedOrganizationIds={locked}
            />
          </Stack>
        </Card>
      </Stack>
    </form>
  );
}

/** 새 요청에서 저장 전에 고른 첨부자료. 저장하면 이 순서대로 올린다. */
function PendingFiles({ files, onChange }: { files: File[]; onChange: (files: File[]) => void }) {
  return (
    <Stack gap="sm" data-testid="pending-attachments">
      <Group justify="space-between" align="center">
        <Text size="sm" c="dimmed">
          {files.length > 0 ? `${files.length}개, 저장하면 올립니다.` : '저장하면 함께 올립니다.'}
        </Text>
        <FileButton multiple onChange={(picked) => onChange([...files, ...picked])}>
          {(props) => (
            <Button {...props} size="xs" variant="light" leftSection={<IconUpload size={14} />}>
              첨부자료 고르기
            </Button>
          )}
        </FileButton>
      </Group>
      {files.length > 0 && (
        <Stack gap={6} role="list" aria-label="올릴 첨부자료">
          {files.map((file, index) => (
            <Group
              key={`${file.name}-${index}`}
              gap="sm"
              wrap="nowrap"
              role="listitem"
              px="sm"
              py={6}
              style={{
                border: '1px solid var(--mantine-color-default-border)',
                borderRadius: 'var(--mantine-radius-sm)',
              }}
            >
              <ThemeIcon variant="light" color="clay" size={32} radius="sm">
                <IconFile size={18} stroke={1.6} />
              </ThemeIcon>
              <Stack gap={0} style={{ flex: 1, minWidth: 0 }}>
                <Text size="sm" fw={600} truncate="end">
                  {file.name}
                </Text>
                <Text size="xs" c="dimmed">
                  {formatBytes(file.size)}
                </Text>
              </Stack>
              <ActionIcon
                variant="subtle"
                color="gray"
                aria-label={`${file.name} 빼기`}
                onClick={() => onChange(files.filter((_, i) => i !== index))}
              >
                <IconTrash size={16} />
              </ActionIcon>
            </Group>
          ))}
        </Stack>
      )}
    </Stack>
  );
}
