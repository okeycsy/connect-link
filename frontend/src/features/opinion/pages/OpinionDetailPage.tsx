import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Skeleton,
  Stack,
  Table,
  Tabs,
  Text,
} from '@mantine/core';
import { IconPencil } from '@tabler/icons-react';
import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { formatDate, formatDateTime } from '../../../shared/format';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState } from '../../../shared/state/StateView';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { ChangeHistoryList } from '../../history/components/ChangeHistoryList';
import { CollationPanel, ReminderHistory } from '../components/CollationPanel';
import { MyReplyPanel } from '../components/MyReplyPanel';
import { DueBadge, OpinionStatusBadge, ReplyStatusBadge } from '../components/OpinionBadges';
import { ConfirmModal, ReasonModal } from '../components/OpinionModals';
import classes from '../components/OpinionTable.module.css';
import { ReplyStatusPanel } from '../components/ReplyStatusPanel';
import {
  type OpinionRequestDetail,
  useCancelOpinionRequest,
  useCloseOpinionRequest,
  useDeleteOpinionRequest,
  useOpenOpinionRequest,
  useOpinionRequest,
} from '../hooks';

type Tab = 'content' | 'replies' | 'collation' | 'reminders' | 'history';
const TABS: Tab[] = ['content', 'replies', 'collation', 'reminders', 'history'];

/**
 * 의견 요청 상세. 결제원 담당자는 요청 내용·회신 현황·취합표·리마인드 이력·변경 이력을 탭으로 보고 상태를 바꾼다. 기관 담당자는 요청
 * 내용과 질문을 보고 그 아래에서 답을 쓴다.
 */
export function OpinionDetailPage() {
  const { business: slug = '', id } = useParams<{ business: string; id: string }>();
  const requestId = Number(id);
  const { data, isError, error } = useOpinionRequest(
    Number.isInteger(requestId) ? requestId : null,
  );

  if (isError) {
    return (
      <Stack gap="md">
        <PageHeader title="의견 요청" />
        <ErrorState error={error} homeLink={`/${slug}/home`} />
      </Stack>
    );
  }
  if (!data) {
    return (
      <Stack gap="md">
        <PageHeader title="의견 요청" />
        <Skeleton h={360} />
      </Stack>
    );
  }
  return <OpinionDetail slug={slug} request={data} />;
}

type Dialog = 'open' | 'close' | 'cancel' | 'delete' | null;

function OpinionDetail({ slug, request }: { slug: string; request: OpinionRequestDetail }) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab') as Tab | null;
  const draft = request.status === 'DRAFT';
  const tab: Tab =
    tabParam &&
    TABS.includes(tabParam) &&
    !(draft && tabParam !== 'content' && tabParam !== 'history')
      ? tabParam
      : 'content';
  const [dialog, setDialog] = useState<Dialog>(null);
  const p = request.permissions;
  const open = useOpenOpinionRequest(request.id);
  const close = useCloseOpinionRequest(request.id);
  const cancel = useCancelOpinionRequest(request.id);
  const remove = useDeleteOpinionRequest(request.id);
  const closeDialog = () => {
    open.reset();
    close.reset();
    remove.reset();
    setDialog(null);
  };

  const description = (
    <>
      {request.businessLabel} · 담당 {request.ownerName} · 회신 기한 {formatDate(request.dueDate)}
      {request.openedAt && ` · ${formatDate(request.openedAt)} 요청`}
      {request.closedAt &&
        ` · ${formatDate(request.closedAt)} ${request.cancelled ? '취소' : '마감'}`}
    </>
  );

  return (
    <Stack gap="md">
      <PageHeader
        title={request.title}
        description={description}
        status={
          <Group gap={6} wrap="nowrap">
            <OpinionStatusBadge status={request.status} cancelled={request.cancelled} />
            <DueBadge status={request.status} daysLeft={request.daysLeft} />
          </Group>
        }
        actions={
          <>
            {p.edit && (
              <Button
                variant="default"
                component={Link}
                to={`/${slug}/opinions/${request.id}/edit`}
                leftSection={<IconPencil size={16} stroke={1.8} />}
              >
                고치기
              </Button>
            )}
            {p.delete && (
              <Button color="red" variant="light" onClick={() => setDialog('delete')}>
                삭제
              </Button>
            )}
            {p.cancel && (
              <Button color="red" variant="light" onClick={() => setDialog('cancel')}>
                요청 취소
              </Button>
            )}
            {p.open && <Button onClick={() => setDialog('open')}>회신 요청 시작</Button>}
            {p.close && <Button onClick={() => setDialog('close')}>마감</Button>}
          </>
        }
      />

      {request.cancelled && (
        <Alert variant="light" color="gray" title="취소된 요청입니다">
          {request.cancelReason}
        </Alert>
      )}

      {p.manage ? (
        <Tabs
          value={tab}
          onChange={(v) =>
            setSearchParams(v === 'content' ? {} : { tab: v ?? 'content' }, { replace: true })
          }
          keepMounted={false}
        >
          <Tabs.List mb="md">
            <Tabs.Tab value="content">요청 내용</Tabs.Tab>
            {!draft && <Tabs.Tab value="replies">회신 현황</Tabs.Tab>}
            {!draft && <Tabs.Tab value="collation">취합표</Tabs.Tab>}
            {!draft && <Tabs.Tab value="reminders">리마인드 이력</Tabs.Tab>}
            <Tabs.Tab value="history">변경 이력</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="content">
            <RequestContent request={request} />
          </Tabs.Panel>
          <Tabs.Panel value="replies">
            <ReplyStatusPanel request={request} />
          </Tabs.Panel>
          <Tabs.Panel value="collation">
            <CollationPanel request={request} />
          </Tabs.Panel>
          <Tabs.Panel value="reminders">
            <ReminderHistory request={request} />
          </Tabs.Panel>
          <Tabs.Panel value="history">
            <Card>
              <ChangeHistoryList targetType="OPINION_REQUEST" targetId={request.id} />
            </Card>
          </Tabs.Panel>
        </Tabs>
      ) : (
        <>
          <RequestContent request={request} />
          {p.reply && <MyReplyPanel request={request} />}
        </>
      )}

      <ConfirmModal
        opened={dialog === 'open'}
        onClose={closeDialog}
        title="회신 요청 시작"
        message={`대상 기관 ${request.targets.length}곳의 회신 담당자에게 요청 메일이 갑니다. 시작한 뒤에도 내용과 대상은 고칠 수 있습니다.`}
        confirmLabel="시작하기"
        pending={open.isPending}
        error={open.error}
        onConfirm={() => open.mutate(undefined, { onSuccess: closeDialog })}
      />
      <ConfirmModal
        opened={dialog === 'close'}
        onClose={closeDialog}
        title="회신 마감"
        message="마감하면 더는 회신을 받지 않습니다. 수정 중인 회신은 제출한 내용으로 남습니다."
        confirmLabel="마감하기"
        pending={close.isPending}
        error={close.error}
        onConfirm={() => close.mutate(undefined, { onSuccess: closeDialog })}
      />
      <ConfirmModal
        opened={dialog === 'delete'}
        onClose={closeDialog}
        title="의견 요청 삭제"
        message="작성 중인 요청과 첨부자료를 지웁니다. 되돌릴 수 없습니다."
        confirmLabel="삭제하기"
        danger
        pending={remove.isPending}
        error={remove.error}
        onConfirm={() =>
          remove.mutate(undefined, { onSuccess: () => navigate(`/${slug}/opinions?status=DRAFT`) })
        }
      />
      <ReasonModal
        opened={dialog === 'cancel'}
        onClose={closeDialog}
        title="의견 요청 취소"
        message="회신을 더 받지 않고 요청을 취소합니다. 사유는 대상 기관 회신 담당자에게 가는 취소 메일에 들어갑니다."
        label="취소 사유"
        confirmLabel="요청 취소"
        danger
        pending={cancel.isPending}
        onConfirm={(reason, done) => cancel.mutate(reason, done)}
      />
    </Stack>
  );
}

/** 요청 내용: 목적·내용, 첨부자료, 질문, 대상 기관. 기관 담당자에게는 서버가 자기 기관 대상만 내려 준다. */
function RequestContent({ request }: { request: OpinionRequestDetail }) {
  const p = request.permissions;
  return (
    <Stack gap="md">
      <Card>
        <Stack gap="sm">
          <Text fw={700} size="xl">
            요청 목적·내용
          </Text>
          <Text size="lg" className={classes.content} data-testid="opinion-content">
            {request.content}
          </Text>
          <div>
            <Text size="sm" c="dimmed" fw={600} mb={4}>
              첨부자료
            </Text>
            <AttachmentPanel
              ownerType="OPINION_REQUEST"
              ownerId={request.id}
              editable={p.edit}
              uploadLabel="첨부자료 올리기"
              emptyMessage="첨부자료가 없습니다."
            />
          </div>
        </Stack>
      </Card>

      {p.manage && request.questions.length > 0 && (
        <Card data-testid="question-list">
          <Stack gap="sm">
            <Text fw={700} size="xl">
              질문 {request.questions.length}개
            </Text>
            {request.questions.map((q, index) => (
              <Stack key={q.id} gap={2}>
                <Group gap="xs">
                  <Text fw={600}>
                    {index + 1}. {q.title}
                  </Text>
                  <Badge size="sm" color="gray" variant="outline">
                    {q.typeLabel}
                  </Badge>
                  {q.required && (
                    <Badge size="sm" color="amber">
                      필수
                    </Badge>
                  )}
                </Group>
                {q.options.length > 0 && (
                  <Text size="sm" c="dimmed">
                    {q.options.map((o) => o.label).join(' / ')}
                  </Text>
                )}
              </Stack>
            ))}
          </Stack>
        </Card>
      )}

      {p.manage && (
        <Card>
          <Text fw={700} size="xl" mb="sm">
            대상 기관 {request.targets.length}곳
          </Text>
          <Table verticalSpacing="sm" data-testid="target-table">
            <Table.Thead>
              <Table.Tr>
                <Table.Th w={180}>기관</Table.Th>
                <Table.Th>회신 담당자</Table.Th>
                <Table.Th w={110}>회신</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {request.targets.map((t) => (
                <Table.Tr key={t.id}>
                  <Table.Td>
                    <Text fw={600}>{t.organizationName}</Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm" c={t.recipients.length ? undefined : 'dimmed'}>
                      {t.recipients.length
                        ? t.recipients
                            .map((r) => (r.subBusiness ? `${r.name}(${r.subBusiness})` : r.name))
                            .join(', ')
                        : '지정 안 함'}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <ReplyStatusBadge status={t.replyStatus} size="xs" />
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
          <Text size="xs" c="dimmed" mt="xs">
            마지막 저장 {formatDateTime(request.updatedAt)}
          </Text>
        </Card>
      )}
    </Stack>
  );
}
