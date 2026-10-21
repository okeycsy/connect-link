import { Anchor, Avatar, Badge, Button, Group, Stack, Text, Tooltip } from '@mantine/core';
import { IconCheck, IconPaperclip } from '@tabler/icons-react';

import { formatBytes } from '../../../shared/format';
import { attachmentDownloadUrl } from '../../attachment/hooks';
import { groupAcksByOrganization, type ChatMessage } from '../hooks';
import classes from './Chat.module.css';
import { formatClock, splitMentions } from './chatFormat';

interface Props {
  message: ChatMessage;
  /** 바로 앞 메시지와 같은 사람이 이어 보낸 것이면 이름·아바타를 줄인다. */
  continued: boolean;
  onToggleAck: (message: ChatMessage, acked: boolean) => void;
  ackPending: boolean;
}

/** 말풍선 하나. 본문(멘션 강조), 첨부, 읽지 않은 사람 수, 기관별 "확인" 을 보인다. */
export function ChatMessageItem({ message, continued, onToggleAck, ackPending }: Props) {
  const ackGroups = groupAcksByOrganization(message.acks);
  const acked = message.acks.some((a) => a.mine);
  const parts = splitMentions(
    message.content,
    message.mentions.map((m) => m.label),
  );

  return (
    <div
      className={classes.message}
      data-mine={message.mine || undefined}
      data-continued={continued || undefined}
      data-testid="chat-message"
    >
      {!message.mine &&
        (continued ? (
          <div className={classes.avatarSpacer} />
        ) : (
          <Avatar radius={8} size={36} color={message.senderKftc ? 'sky' : 'clay'} aria-hidden>
            {message.senderName.slice(0, 1)}
          </Avatar>
        ))}
      <div className={classes.body}>
        {!message.mine && !continued && (
          <Group gap={6}>
            <Text size="sm" fw={700}>
              {message.senderName}
            </Text>
            <Text size="xs" c="dimmed">
              {message.senderOrganizationName}
            </Text>
          </Group>
        )}
        <div className={classes.bubbleRow}>
          <Stack
            gap={6}
            className={classes.bubble}
            data-mentions-me={message.mentionsMe || undefined}
          >
            {message.content && (
              <span>
                {parts.map((part, i) =>
                  part.mention ? (
                    <span key={i} className={classes.mention}>
                      {part.text}
                    </span>
                  ) : (
                    <span key={i}>{part.text}</span>
                  ),
                )}
              </span>
            )}
            {message.attachmentsExpired ? (
              <Text size="sm" c="dimmed">
                보관 기간이 지나 첨부 파일을 지웠습니다.
              </Text>
            ) : (
              message.attachments.map((file) => (
                <Group key={file.id} gap={6} wrap="nowrap">
                  <IconPaperclip size={16} stroke={1.8} />
                  <Anchor href={attachmentDownloadUrl(file.id)} size="sm" fw={600} truncate>
                    {file.name}
                  </Anchor>
                  <Text size="xs" c="dimmed" className={classes.meta}>
                    {formatBytes(file.size)}
                  </Text>
                </Group>
              ))
            )}
          </Stack>
          <Stack gap={0} align={message.mine ? 'flex-end' : 'flex-start'}>
            {message.unreadCount > 0 && (
              <Tooltip label="아직 읽지 않은 사람 수">
                <Text size="xs" fw={700} c="clay.6" className={classes.meta}>
                  {message.unreadCount}
                </Text>
              </Tooltip>
            )}
            <Text size="xs" c="dimmed" className={classes.meta}>
              {formatClock(message.sentAt)}
            </Text>
          </Stack>
        </div>
        {(ackGroups.length > 0 || message.canAck) && (
          <Group gap={4}>
            {ackGroups.map((group) => (
              <Tooltip key={group.organizationCode} label={`${group.names.join(', ')} 확인`}>
                <Badge
                  size="sm"
                  color="sky"
                  leftSection={<IconCheck size={12} stroke={2} />}
                  data-testid="chat-ack"
                >
                  {group.organizationName} {group.names.length}
                </Badge>
              </Tooltip>
            ))}
            {message.canAck && (
              <Button
                size="compact-xs"
                variant={acked ? 'outline' : 'light'}
                onClick={() => onToggleAck(message, acked)}
                loading={ackPending}
              >
                {acked ? '확인 취소' : '확인'}
              </Button>
            )}
          </Group>
        )}
      </div>
    </div>
  );
}
