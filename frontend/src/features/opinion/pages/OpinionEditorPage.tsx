import {
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
import { notifications } from '@mantine/notifications';
import { useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { PendingAttachments } from '../../attachment/components/PendingAttachments';
import { attachmentKeys, uploadAttachmentFile } from '../../attachment/hooks';
import { type Notice, useLinkNoticeOpinion, useNotice } from '../../notice/hooks';
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
import {
  backwardJumpErrors,
  formBody,
  JUMP_NEXT,
  jumpValueOf,
  type QuestionDraft,
  type SectionDraft,
} from '../questions';

interface Values {
  title: string;
  content: string;
  dueDate: string;
  sections: SectionDraft[];
  questions: QuestionDraft[];
}

/** 공지에서 "회신 받기: 예" 로 넘어왔을 때 채우는 질문. 회의 참석 회신이 흔한 모양이라 참석 여부와 참석자를 묻고, 고쳐 쓸 수 있다. */
function noticeReplyForm(): Pick<Values, 'sections' | 'questions'> {
  return {
    sections: [{ key: 's-first', title: '', description: '', jump: JUMP_NEXT }],
    questions: [
      {
        key: 'n-attend',
        sectionKey: 's-first',
        type: 'SINGLE_CHOICE',
        title: '참석하십니까?',
        required: true,
        branching: false,
        options: [
          { label: '참석', jump: JUMP_NEXT },
          { label: '불참', jump: JUMP_NEXT },
        ],
      },
      {
        key: 'n-attendees',
        sectionKey: 's-first',
        type: 'TEXT',
        title: '참석자 이름과 직위를 적어 주세요.',
        required: false,
        branching: false,
        options: [],
      },
    ],
  };
}

/** 고치는 요청의 섹션·질문을 편집 값으로. 섹션 키는 서버 ID 로 만든다. */
function initialForm(existing?: OpinionRequestDetail): Pick<Values, 'sections' | 'questions'> {
  if (!existing) {
    // 새 요청은 섹션 하나와 빈 객관식 질문 하나로 시작한다. 질문이 있어야 회신 요청을 시작할 수 있다.
    return {
      sections: [{ key: 's-first', title: '', description: '', jump: JUMP_NEXT }],
      questions: [
        {
          key: 'n-first',
          sectionKey: 's-first',
          type: 'SINGLE_CHOICE',
          title: '',
          required: true,
          branching: false,
          options: [
            { label: '', jump: JUMP_NEXT },
            { label: '', jump: JUMP_NEXT },
          ],
        },
      ],
    };
  }
  const keys = existing.sections.map((s) => `s${s.id}`);
  return {
    sections: existing.sections.map((s, i) => ({
      key: keys[i],
      title: s.title ?? '',
      description: s.description ?? '',
      jump: jumpValueOf(s.jump, keys),
    })),
    questions: existing.questions.map((q) => ({
      key: `q${q.id}`,
      sectionKey: keys[q.sectionIndex] ?? keys[0],
      type: q.type,
      title: q.title,
      required: q.required,
      branching: q.options.some((o) => o.jump != null),
      options: q.options.map((o) => ({ label: o.label, jump: jumpValueOf(o.jump, keys) })),
    })),
  };
}

/**
 * 새 의견 요청과 요청 고치기. 주소에 :id 가 있으면 고치기다. 첨부자료는 여기서 고르고, 새 요청이면 저장한 뒤 바로 올린다.
 * 질문은 작성중일 때만 고친다(회신을 받기 시작하면 답과 어긋나지 않게 서버가 막는다).
 * 새 요청 주소에 `?copyFrom=요청 ID` 가 있으면 그 요청의 내용·질문·대상을 채워 시작한다(복사해서 만들기, 회신 기한·첨부자료는 새로).
 * `?notice=공지 ID` 가 있으면 공지에서 "회신 받기: 예" 로 넘어온 것이라 제목·질문을 채우고, 저장하면 그 공지에 잇는다.
 */
export function OpinionEditorPage() {
  const { business: slug = '', id } = useParams<{ business: string; id?: string }>();
  const [searchParams] = useSearchParams();
  const copyFromId = id ? null : Number(searchParams.get('copyFrom')) || null;
  const requestId = id ? Number(id) : null;
  const noticeId = id || copyFromId ? null : Number(searchParams.get('notice')) || null;
  const detail = useOpinionRequest(requestId ?? copyFromId);
  const notice = useNotice(noticeId);

  if (noticeId != null) {
    if (notice.isError) {
      return (
        <Stack gap="md">
          <PageHeader title="회신 받을 의견 요청" />
          <ErrorState error={notice.error} homeLink={`/${slug}/home`} />
        </Stack>
      );
    }
    if (!notice.data) {
      return (
        <Stack gap="md">
          <PageHeader title="회신 받을 의견 요청" />
          <Skeleton h={320} />
        </Stack>
      );
    }
    return <OpinionEditor key={`notice-${noticeId}`} slug={slug} fromNotice={notice.data} />;
  }
  if (requestId == null && copyFromId == null) return <OpinionEditor slug={slug} />;
  if (copyFromId != null) {
    if (detail.isError) {
      return (
        <Stack gap="md">
          <PageHeader title="복사해서 새 의견 요청" />
          <ErrorState error={detail.error} homeLink={`/${slug}/home`} />
        </Stack>
      );
    }
    if (!detail.data) {
      return (
        <Stack gap="md">
          <PageHeader title="복사해서 새 의견 요청" />
          <Skeleton h={320} />
        </Stack>
      );
    }
    return <OpinionEditor key={copyFromId} slug={slug} copyFrom={detail.data} />;
  }
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

function OpinionEditor({
  slug,
  existing,
  copyFrom,
  fromNotice,
}: {
  slug: string;
  existing?: OpinionRequestDetail;
  /** 복사해서 만들 때 원래 요청. 저장하면 새 작성중 요청이다. */
  copyFrom?: OpinionRequestDetail;
  /** 회신을 받을 공지. 저장하면 새 요청을 이 공지에 잇는다. */
  fromNotice?: Notice;
}) {
  const navigate = useNavigate();
  const current = useCurrentBusiness();
  const candidates = useRecipientCandidates(slug);
  const create = useCreateOpinionRequest();
  const linkNotice = useLinkNoticeOpinion();
  const update = useUpdateOpinionRequest(existing?.id ?? 0);
  const mutation = existing ? update : create;
  const queryClient = useQueryClient();
  const nextKey = useRef(0);
  const newKey = () => `n${nextKey.current++}`;
  const questionsLocked = existing != null && existing.status !== 'DRAFT';
  // 새 요청의 첨부자료. 요청을 저장해 번호가 생긴 뒤 올린다.
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);

  const source = existing ?? copyFrom;
  const form = useForm<Values>({
    initialValues: {
      title: source?.title ?? (fromNotice ? `${fromNotice.title} 회신` : ''),
      content:
        source?.content ??
        (fromNotice ? `공지·자료실의 "${fromNotice.title}" 글에 대한 회신을 요청합니다.` : ''),
      dueDate: existing?.dueDate ?? dayjs().add(7, 'day').format('YYYY-MM-DD'),
      ...(fromNotice ? noticeReplyForm() : initialForm(source)),
    },
  });
  const [targets, setTargets] = useState<TargetValue[]>(
    () =>
      source?.targets.map((t) => ({
        organizationId: t.organizationId,
        recipientEmails: t.recipients.map((r) => r.email),
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
      Object.keys(fieldErrors).some(
        (k) =>
          !knownFields.includes(k) && !k.startsWith('questions.') && !k.startsWith('sections.'),
      ))
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
    if (!questionsLocked) {
      const backward = backwardJumpErrors(values.sections, values.questions);
      if (Object.keys(backward).length > 0) {
        form.setErrors(backward);
        return;
      }
    }
    const body = {
      title: values.title,
      content: values.content,
      dueDate: values.dueDate,
      ...formBody(values.sections, values.questions),
      targets: targets.map((t) => ({
        organizationId: t.organizationId,
        recipientEmails: t.recipientEmails,
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
            if (fromNotice) {
              try {
                await linkNotice.mutateAsync({ notice: fromNotice, opinionRequestId: d.id });
              } catch {
                notifications.show({
                  title: '공지에 잇지 못했습니다',
                  message: '공지 고치기에서 회신 받기를 다시 골라 주세요.',
                  color: 'red',
                });
              }
            }
            navigate(`/${slug}/opinions/${d.id}`);
          },
        },
      );
    }
  };

  const title = existing
    ? '의견 요청 고치기'
    : copyFrom
      ? '복사해서 새 의견 요청'
      : fromNotice
        ? '회신 받을 의견 요청'
        : '새 의견 요청';
  return (
    <form onSubmit={form.onSubmit(submit)} noValidate>
      <Stack gap="md">
        <PageHeader
          title={title}
          description={
            existing?.status === 'OPEN'
              ? '회신중인 요청입니다. 저장하면 새로 넣은 담당자에게 요청 메일이, 남은 담당자에게 변경 알림이 갑니다.'
              : copyFrom
                ? `"${copyFrom.title}" 의 내용·질문·대상 기관을 채웠습니다. 회신 기한과 첨부자료는 새로 정하고, 저장하면 작성중인 새 요청이 됩니다.`
                : fromNotice
                  ? `공지 "${fromNotice.title}" 의 회신을 받을 요청입니다. 참석 여부·참석자 질문을 채워 두었으니 대상 기관과 회신 기한을 고르세요. 저장하면 공지에 이어지고, 상세에서 회신 요청을 시작합니다.`
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
              <PendingAttachments files={pendingFiles} onChange={setPendingFiles} />
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
                  : '기관에 물을 내용을 질문으로 나눠 넣습니다. 객관식(하나 고르기), 체크박스(여러 개 고르기), 주관식(글로 답하기) 중에서 고릅니다. 섹션을 더하면 객관식 답에 따라 다른 섹션으로 넘어가게 할 수 있고, 기관은 지나는 섹션의 질문에만 답합니다. 회신 요청을 시작하려면 질문이 하나 이상 있어야 합니다.'}
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
