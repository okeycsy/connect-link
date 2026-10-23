import { Button, Group, Stack, Text, Textarea } from '@mantine/core';
import { IconCornerDownRight } from '@tabler/icons-react';
import { useState } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import { formatAuthor, formatDateTime } from '../../../shared/format';
import { type TestItemResultRow, useAddTestComment, useDeleteTestComment } from '../hooks';
import classes from './TestPlanTable.module.css';

/**
 * 결제원 의견 아래에 결제원 담당자와 그 기관 담당자가 시간순으로 이어 다는 답글. 쓴 사람은 "결제원(김민찬 과장)", "KB국민은행(김민준 대리)"처럼
 * 보이고, 입력창에는 지금 쓸 사람이 보인다. 자기가 쓴 답글만 지운다.
 */
export function TestItemComments({
  row,
  organizationName,
  canReply,
  writer,
  planId,
  organizationId,
}: {
  row: TestItemResultRow;
  organizationName: string;
  canReply: boolean;
  writer: string;
  planId: number;
  organizationId: number;
}) {
  const add = useAddTestComment(planId, organizationId);
  const remove = useDeleteTestComment(planId, organizationId);
  const [open, setOpen] = useState(false);
  const [body, setBody] = useState('');

  if (row.comments.length === 0 && !canReply) return null;

  const submit = () =>
    add.mutate(
      { itemId: row.itemId, body: body.trim() },
      {
        onSuccess: () => {
          setBody('');
          setOpen(false);
        },
      },
    );
  const error = add.isError ? (fieldErrorsOf(add.error).body ?? add.error.message) : undefined;

  return (
    <Stack gap={8} mt="xs" data-testid="item-comments">
      {row.comments.length > 0 && (
        <Stack gap={10} className={classes.thread}>
          {row.comments.map((c) => (
            <div key={c.id} data-testid="item-comment">
              <Group gap="xs" wrap="nowrap">
                <Text size="sm" fw={600}>
                  {formatAuthor(
                    c.authorKftc ? '결제원' : organizationName,
                    c.authorName,
                    c.authorJobTitle,
                  )}
                </Text>
                <Text size="xs" c="dimmed" className={classes.numeric}>
                  {formatDateTime(c.writtenAt)}
                </Text>
                {c.deletable && (
                  <Button
                    size="compact-xs"
                    variant="subtle"
                    color="gray"
                    loading={remove.isPending && remove.variables?.commentId === c.id}
                    onClick={() => remove.mutate({ itemId: row.itemId, commentId: c.id })}
                  >
                    지우기
                  </Button>
                )}
              </Group>
              <Text size="sm" className={classes.content}>
                {c.body}
              </Text>
            </div>
          ))}
        </Stack>
      )}
      {remove.isError && (
        <Text size="xs" c="red.6">
          {remove.error.message}
        </Text>
      )}
      {canReply &&
        (open ? (
          <Stack gap={6} className={classes.thread}>
            <Text size="sm" fw={600}>
              {writer}
            </Text>
            <Textarea
              aria-label={`${row.number} 답글 입력`}
              placeholder="답글을 입력하세요. 상대 쪽 담당자에게 알림이 갑니다."
              autosize
              minRows={2}
              maxLength={2000}
              data-autofocus
              value={body}
              error={error}
              onChange={(e) => setBody(e.currentTarget.value)}
            />
            <Group justify="flex-end" gap="xs">
              <Button
                size="xs"
                variant="default"
                onClick={() => {
                  setOpen(false);
                  add.reset();
                }}
              >
                취소
              </Button>
              <Button
                size="xs"
                disabled={body.trim() === ''}
                loading={add.isPending}
                onClick={submit}
              >
                답글 달기
              </Button>
            </Group>
          </Stack>
        ) : (
          <Button
            size="xs"
            variant="subtle"
            w="fit-content"
            leftSection={<IconCornerDownRight size={14} stroke={1.8} />}
            aria-label={`${row.number} 답글`}
            onClick={() => setOpen(true)}
          >
            답글
          </Button>
        ))}
    </Stack>
  );
}
