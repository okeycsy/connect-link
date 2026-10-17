import { Alert, Button, Card, Group, Radio, Skeleton, Stack, Text, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useEffect, useState } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import { formatDateTime } from '../../../shared/format';
import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import {
  type OpinionMyReply,
  type OpinionReplyType,
  type OpinionRequestDetail,
  REPLY_TYPE_OPTIONS,
  useMyOpinionReply,
  useRequestOpinionReplyRevision,
  useSaveOpinionReplyDraft,
  useSubmitOpinionReply,
} from '../hooks';
import { ReplyStatusBadge, ReplyTypeBadge } from './OpinionBadges';
import { ReasonModal } from './OpinionModals';
import classes from './OpinionTable.module.css';

/**
 * 우리 기관 회신(기관 담당자). 의견 유형과 세부의견을 쓰고 근거자료를 붙여 임시저장하거나 최종제출한다. 제출한 뒤에는 제출 내용을 보고,
 * 고치려면 수정 요청을 한다(결제원 담당자가 승인하면 다시 고칠 수 있다).
 */
export function MyReplyPanel({ request }: { request: OpinionRequestDetail }) {
  const { data, isPending, error } = useMyOpinionReply(request.id);
  if (error) return <StateView kind={stateKindOf(error)} compact />;
  if (isPending || !data) return <Skeleton h={280} />;
  return <MyReplyBody request={request} reply={data} />;
}

function MyReplyBody({ request, reply }: { request: OpinionRequestDetail; reply: OpinionMyReply }) {
  const [revisionOpen, setRevisionOpen] = useState(false);
  const revision = useRequestOpinionReplyRevision(request.id);

  return (
    <Card data-testid="my-reply">
      <Stack gap="md">
        <Group justify="space-between">
          <Group gap="xs">
            <Text fw={700} size="xl">
              {reply.organizationName} 회신
            </Text>
            <ReplyStatusBadge status={reply.status} />
          </Group>
          {reply.revisionRequestable && (
            <Button variant="light" onClick={() => setRevisionOpen(true)}>
              수정 요청
            </Button>
          )}
        </Group>

        {reply.status === 'REVISION_REQUESTED' && (
          <Alert variant="light" color="clay" title="수정 요청을 보냈습니다">
            결제원 담당자가 승인하면 제출한 내용을 고쳐 다시 제출할 수 있습니다. 요청 사유:{' '}
            {reply.revisionReason}
          </Alert>
        )}
        {reply.status === 'SUBMITTED' && reply.revisionRejectionReason && (
          <Alert variant="light" color="red" title="수정 요청이 반려되었습니다">
            {reply.revisionRejectionReason}
          </Alert>
        )}
        {reply.status === 'REVISING' && (
          <Alert variant="light" color="amber">
            수정 요청이 승인되었습니다. 고친 뒤 다시 최종제출해야 반영됩니다. 그때까지는 처음 제출한
            내용이 회신으로 남습니다.
          </Alert>
        )}
        {!reply.editable && request.status !== 'OPEN' && reply.submittedAt == null && (
          <Alert variant="light" color="gray">
            {request.cancelled ? '취소된 요청입니다.' : '마감한 요청이라 회신을 받지 않습니다.'}
          </Alert>
        )}

        {reply.editable ? (
          <ReplyEditor request={request} reply={reply} />
        ) : (
          reply.submittedAt != null && <SubmittedReply reply={reply} />
        )}
      </Stack>

      <ReasonModal
        opened={revisionOpen}
        onClose={() => setRevisionOpen(false)}
        title="회신 수정 요청"
        message="제출한 회신을 고치고 싶다고 결제원 담당자에게 요청합니다. 승인하면 다시 고쳐 제출할 수 있습니다."
        label="수정 사유"
        placeholder="예: 근거자료의 수치가 잘못되어 고치려고 합니다."
        confirmLabel="요청 보내기"
        pending={revision.isPending}
        onConfirm={(reason, done) => revision.mutate(reason, done)}
      />
    </Card>
  );
}

/** 제출한 내용(읽기). */
function SubmittedReply({ reply }: { reply: OpinionMyReply }) {
  return (
    <Stack gap="sm" data-testid="submitted-reply">
      <Group gap="xs">
        <Text size="sm" c="dimmed" fw={600}>
          의견 유형
        </Text>
        <ReplyTypeBadge type={reply.type} />
      </Group>
      <div>
        <Text size="sm" c="dimmed" fw={600}>
          세부의견
        </Text>
        <Text className={classes.content}>{reply.content ?? '(적지 않음)'}</Text>
      </div>
      <Text size="sm" c="dimmed">
        {reply.replierName} · {formatDateTime(reply.submittedAt)} 제출
      </Text>
      {reply.replyId != null && (
        <div>
          <Text size="sm" c="dimmed" fw={600} mb={4}>
            근거자료
          </Text>
          <AttachmentPanel
            ownerType="OPINION_REPLY"
            ownerId={reply.replyId}
            emptyMessage="근거자료가 없습니다."
          />
        </div>
      )}
    </Stack>
  );
}

interface Values {
  type: OpinionReplyType | '';
  content: string;
}

/** 쓰는 중인 회신. 임시저장은 제출한 내용을 건드리지 않는다. */
function ReplyEditor({ request, reply }: { request: OpinionRequestDetail; reply: OpinionMyReply }) {
  const save = useSaveOpinionReplyDraft(request.id);
  const submit = useSubmitOpinionReply(request.id);
  const [notice, setNotice] = useState<string>();
  const form = useForm<Values>({
    initialValues: { type: reply.draftType ?? '', content: reply.draftContent ?? '' },
  });
  const failed = save.error ?? submit.error;
  useEffect(() => {
    if (failed) form.setErrors(fieldErrorsOf(failed));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [failed]);
  const generalError =
    failed && Object.keys(fieldErrorsOf(failed)).length === 0 ? failed.message : undefined;

  const body = () => ({
    type: form.values.type === '' ? null : form.values.type,
    content: form.values.content,
  });

  return (
    <Stack gap="md">
      <Radio.Group label="의견 유형" withAsterisk {...form.getInputProps('type')}>
        <Group gap="lg" mt={6}>
          {REPLY_TYPE_OPTIONS.map((o) => (
            <Radio key={o.value} value={o.value} label={o.label} />
          ))}
        </Group>
      </Radio.Group>
      <Textarea
        label="세부의견"
        description="조건부 동의와 이견은 조건이나 이유를 꼭 적어 주세요."
        autosize
        minRows={5}
        maxLength={4000}
        {...form.getInputProps('content')}
      />
      <div>
        <Text size="sm" fw={500} mb={4}>
          근거자료
        </Text>
        {reply.replyId != null ? (
          <AttachmentPanel
            ownerType="OPINION_REPLY"
            ownerId={reply.replyId}
            editable
            uploadLabel="근거자료 올리기"
            emptyMessage="올린 근거자료가 없습니다."
          />
        ) : (
          <Text size="sm" c="dimmed">
            임시저장하면 근거자료를 올릴 수 있습니다.
          </Text>
        )}
      </div>
      {generalError && (
        <Alert variant="light" color="red" data-testid="reply-error">
          {generalError}
        </Alert>
      )}
      {notice && (
        <Alert variant="light" color="sky" data-testid="reply-notice">
          {notice}
        </Alert>
      )}
      <Group justify="flex-end" gap="xs">
        <Button
          variant="light"
          loading={save.isPending}
          onClick={() => {
            submit.reset();
            save.mutate(body(), {
              onSuccess: () =>
                setNotice('임시저장했습니다. 최종제출해야 결제원 담당자에게 회신으로 보입니다.'),
            });
          }}
        >
          임시저장
        </Button>
        <Button
          loading={submit.isPending}
          onClick={() => {
            save.reset();
            setNotice(undefined);
            submit.mutate(body());
          }}
        >
          최종제출
        </Button>
      </Group>
    </Stack>
  );
}
