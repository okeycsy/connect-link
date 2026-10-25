import {
  Badge,
  Box,
  Button,
  Card,
  Group,
  Pagination,
  Skeleton,
  Stack,
  Text,
  UnstyledButton,
} from '@mantine/core';
import { IconPin, IconPlus } from '@tabler/icons-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { useMe } from '../../account/hooks';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { useCurrentBusiness } from '../../organization/hooks';
import { ConfirmModal } from '../../../shared/ConfirmModal';
import { formatDate } from '../../../shared/format';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import { DetailDrawer } from '../../../shared/list/DetailDrawer';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { NoticeFormModal } from '../components/NoticeFormModal';
import {
  NOTICE_CATEGORY_LABEL,
  type Notice,
  type NoticeCategory,
  useDeleteNotice,
  useNotices,
} from '../hooks';

type CategoryFilter = 'ALL' | NoticeCategory;

const CATEGORY_OPTIONS: { value: CategoryFilter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'NOTICE', label: '공지' },
  { value: 'RULE', label: '규약' },
  { value: 'ENFORCEMENT_RULE', label: '시행세칙' },
];

/** 분류 배지 색. 공지는 스카이, 자료(규약·시행세칙)는 클레이. */
const CATEGORY_COLOR: Record<NoticeCategory, string> = {
  NOTICE: 'sky',
  RULE: 'clay',
  ENFORCEMENT_RULE: 'clay',
};

/**
 * 공지·자료실. 이 업무의 공지와 규약·시행세칙, 전 업무 공통 공지를 한 목록에서 본다(고정 글 먼저). 그 업무 결제원 담당자가 업무 글을, 관리자가 공통 공지를
 * 올린다. 글을 누르면 오른쪽에 내용과 첨부가 열린다.
 */
export function NoticeListPage() {
  const business = useCurrentBusiness();
  const { data: me } = useMe();
  const [searchParams, setSearchParams] = useSearchParams();
  const [category, setCategory] = useState<CategoryFilter>('ALL');
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Notice | null>(null);
  const [formOpened, setFormOpened] = useState(false);
  const [deleting, setDeleting] = useState<Notice | null>(null);
  const remove = useDeleteNotice();

  const slug = business?.slug ?? '';
  const { data, isPending, isError, error } = useNotices(
    slug,
    category === 'ALL' ? null : category,
    page,
  );
  const canWrite =
    !!me &&
    (me.admin || (me.organizationType === 'KFTC' && me.businesses.some((b) => b.slug === slug)));
  const selectedId = Number(searchParams.get('id')) || null;
  const selected = data?.content.find((n) => n.id === selectedId) ?? null;

  const open = (id: number | null) => {
    const next = new URLSearchParams(searchParams);
    if (id == null) next.delete('id');
    else next.set('id', String(id));
    setSearchParams(next, { replace: true });
  };

  return (
    <Stack gap="lg">
      <PageHeader
        title="공지·자료실"
        description={`${business?.label ?? ''} 공지와 규약·시행세칙, 전 업무 공통 공지를 봅니다.`}
        actions={
          canWrite && (
            <Button
              leftSection={<IconPlus size={16} />}
              onClick={() => {
                setEditing(null);
                setFormOpened(true);
              }}
            >
              글 쓰기
            </Button>
          )
        }
      />

      <Card>
        <Group mb="md">
          <ChipFilter
            label="분류"
            value={category}
            options={CATEGORY_OPTIONS}
            onChange={(v) => {
              setCategory(v);
              setPage(0);
            }}
          />
        </Group>
        {isError ? (
          <ErrorState error={error} />
        ) : isPending ? (
          <Stack gap="sm">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} h={56} />
            ))}
          </Stack>
        ) : data.content.length === 0 ? (
          <StateView kind="empty" compact title="올라온 글이 없습니다" />
        ) : (
          <Stack gap={0} data-testid="notice-list">
            {data.content.map((n) => (
              <UnstyledButton
                key={n.id}
                onClick={() => open(n.id)}
                py="sm"
                px="xs"
                style={{ borderBottom: '1px solid var(--mantine-color-default-border)' }}
              >
                <Group gap="sm" wrap="nowrap" align="flex-start">
                  <Box w={18} pt={2} style={{ flexShrink: 0 }}>
                    {n.pinned && (
                      <IconPin size={16} aria-label="고정" color="var(--mantine-color-clay-6)" />
                    )}
                  </Box>
                  <Stack gap={2} style={{ minWidth: 0, flex: 1 }}>
                    <Group gap={6} wrap="wrap">
                      <Badge size="sm" variant="light" color={CATEGORY_COLOR[n.category]}>
                        {n.categoryLabel}
                      </Badge>
                      {n.business == null && (
                        <Badge size="sm" variant="outline" color="gray">
                          전 업무 공통
                        </Badge>
                      )}
                      <Text fw={600} size="md" lineClamp={1}>
                        {n.title}
                      </Text>
                    </Group>
                    <Text size="xs" c="dimmed">
                      {n.authorName} · {formatDate(n.createdAt)}
                      {n.effectiveDate && ` · 시행 ${formatDate(n.effectiveDate)}`}
                    </Text>
                  </Stack>
                </Group>
              </UnstyledButton>
            ))}
          </Stack>
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

      <DetailDrawer
        opened={selected != null}
        onClose={() => open(null)}
        title={selected?.title ?? ''}
        subtitle={
          selected && (
            <Text size="sm" c="dimmed">
              {selected.business == null ? '전 업무 공통' : selected.businessLabel} ·{' '}
              {NOTICE_CATEGORY_LABEL[selected.category]} · {selected.authorName} ·{' '}
              {formatDate(selected.createdAt)}
            </Text>
          )
        }
        footer={
          selected?.canEdit && (
            <Group justify="flex-end" gap="xs">
              <Button variant="default" color="red" onClick={() => setDeleting(selected)}>
                지우기
              </Button>
              <Button
                onClick={() => {
                  setEditing(selected);
                  setFormOpened(true);
                }}
              >
                고치기
              </Button>
            </Group>
          )
        }
      >
        {selected && (
          <Stack gap="lg">
            {selected.effectiveDate && (
              <Text size="sm">
                <b>시행일</b> {formatDate(selected.effectiveDate)}
              </Text>
            )}
            <Text size="md" style={{ whiteSpace: 'pre-wrap' }}>
              {selected.content}
            </Text>
            <Stack gap="xs">
              <Text fw={700}>첨부</Text>
              <AttachmentPanel
                ownerType="NOTICE"
                ownerId={selected.id}
                editable={selected.canEdit}
                uploadLabel="파일 올리기"
                emptyMessage="첨부 파일이 없습니다."
              />
            </Stack>
          </Stack>
        )}
      </DetailDrawer>

      {business && (
        <NoticeFormModal
          opened={formOpened}
          onClose={() => setFormOpened(false)}
          business={business}
          notice={editing}
          canPostCommon={!!me?.admin}
        />
      )}

      <ConfirmModal
        opened={deleting != null}
        onClose={() => setDeleting(null)}
        title="글 지우기"
        message={`"${deleting?.title ?? ''}" 글과 첨부 파일을 지웁니다. 되돌릴 수 없습니다.`}
        confirmLabel="지우기"
        danger
        pending={remove.isPending}
        error={remove.error}
        onConfirm={() =>
          deleting &&
          remove.mutate(deleting.id, {
            onSuccess: () => {
              setDeleting(null);
              open(null);
            },
          })
        }
      />
    </Stack>
  );
}
