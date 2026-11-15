import { Button, Card, Chip, Group, Pagination, Skeleton, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { DEFAULT_BUSINESS_SLUG } from '../../../app/navigation';
import { PageHeader } from '../../../shared/PageHeader';
import { AttachmentApprovalModal } from '../../attachment/components/AttachmentApprovalModal';
import type { AttachmentOwner, AttachmentOwnerType } from '../../attachment/hooks';
import { NotificationItem } from '../components/NotificationItem';
import {
  useMarkAllRead,
  useMarkRead,
  useNotifications,
  useUnreadCount,
  type NotificationResponse,
} from '../hooks';

const PAGE_SIZE = 20;

/** 주소의 결재 대상(approvalOwnerType, approvalOwnerId). 첨부파일 반출 결재 요청 알림이 이 주소로 연다. */
function approvalOwnerOf(params: URLSearchParams): AttachmentOwner | null {
  const ownerType = params.get('approvalOwnerType');
  const ownerId = Number(params.get('approvalOwnerId'));
  if (!ownerType || !Number.isInteger(ownerId) || ownerId <= 0) return null;
  return { ownerType: ownerType as AttachmentOwnerType, ownerId };
}

/**
 * 알림 센터. 가입 승인, 의견 요청, 연락처 변경, 일시중단 알림을 한 목록으로 본다. 메일·매터모스트로 나간 내용과 같다. 팀장은 첨부파일 반출 결재
 * 요청 알림을 눌러 여기서 바로 결재 창을 연다.
 */
export function NotificationCenterPage() {
  const navigate = useNavigate();
  const { business } = useParams<{ business: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const approvalOwner = approvalOwnerOf(searchParams);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');
  const [page, setPage] = useState(0);
  const unreadOnly = filter === 'unread';
  const { data, isPending, isError } = useNotifications({ page, size: PAGE_SIZE, unreadOnly });
  const { data: unread } = useUnreadCount();
  const markRead = useMarkRead();
  const markAllRead = useMarkAllRead();

  const open = (n: NotificationResponse) => {
    if (!n.read) markRead.mutate(n.id);
    if (n.link) navigate(n.link);
  };

  return (
    <Stack gap="md">
      <PageHeader
        title="알림 센터"
        // 세는 동안 0건으로 보이지 않게 한다.
        description={unread ? `읽지 않은 알림 ${unread.count}건` : '읽지 않은 알림을 세는 중…'}
        actions={
          <Button
            variant="light"
            disabled={(unread?.count ?? 0) === 0}
            loading={markAllRead.isPending}
            onClick={() => markAllRead.mutate()}
          >
            모두 읽음
          </Button>
        }
      />

      <Card data-testid="notification-list">
        <Group justify="space-between" mb="sm">
          <Chip.Group
            value={filter}
            onChange={(v) => {
              setFilter(v as 'all' | 'unread');
              setPage(0);
            }}
          >
            <Group gap="xs">
              <Chip value="all" size="xs">
                전체
              </Chip>
              <Chip value="unread" size="xs">
                읽지 않음
              </Chip>
            </Group>
          </Chip.Group>
          {data && (
            <Text size="sm" c="dimmed">
              {data.totalElements}건
            </Text>
          )}
        </Group>

        {isError && (
          <Text c="red" size="sm">
            알림을 불러오지 못했습니다.
          </Text>
        )}
        {isPending ? (
          <Stack gap="xs">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} h={56} />
            ))}
          </Stack>
        ) : data?.content.length ? (
          <Stack gap={2}>
            {data.content.map((n) => (
              <NotificationItem key={n.id} notification={n} onOpen={open} />
            ))}
          </Stack>
        ) : (
          <Text c="dimmed" ta="center" py="xl">
            {unreadOnly ? '읽지 않은 알림이 없습니다.' : '알림이 없습니다.'}
          </Text>
        )}

        {data && data.totalPages > 1 && (
          <Group justify="center" mt="md">
            <Pagination
              total={data.totalPages}
              value={page + 1}
              onChange={(p) => setPage(p - 1)}
              size="sm"
            />
          </Group>
        )}
      </Card>

      <AttachmentApprovalModal
        owner={approvalOwner}
        base={business ?? DEFAULT_BUSINESS_SLUG}
        onClose={() => setSearchParams({}, { replace: true })}
      />
    </Stack>
  );
}
