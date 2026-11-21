import { ActionIcon, Group, Popover, Stack, Text, Textarea, UnstyledButton } from '@mantine/core';
import { IconSend } from '@tabler/icons-react';
import { useRef, useState, type KeyboardEvent } from 'react';

import { mentionQueryAt, type ChatMention, type ChatSendInput } from '../hooks';
import classes from './Chat.module.css';

interface Props {
  mentionables: ChatMention[];
  onSend: (input: ChatSendInput) => Promise<unknown>;
  sending: boolean;
  error?: string;
}

/**
 * 메시지 입력. "@" 를 치면 이 방에서 부를 수 있는 기관(결제원 포함)이 뜨고, 고르면 "@기관 " 이 들어간다.
 * 보낼 때 본문에 남아 있는 "@기관" 만 멘션으로 보낸다. Enter 는 보내기, Shift+Enter 는 줄바꿈.
 * 파일 첨부는 없다(결제원 파일 반출은 팀장 결재가 필요해 대화방에서는 막았다).
 */
export function ChatComposer({ mentionables, onSend, sending, error }: Props) {
  const [content, setContent] = useState('');
  const [caret, setCaret] = useState(0);
  const [highlight, setHighlight] = useState(0);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const query = mentionQueryAt(content, caret);
  const options =
    query == null ? [] : mentionables.filter((m) => m.label.includes(query)).slice(0, 8);
  const menuOpen = options.length > 0;

  const pickMention = (mention: ChatMention) => {
    const before = content.slice(0, caret).replace(/@([^\s@]*)$/, `@${mention.label} `);
    const next = before + content.slice(caret);
    setContent(next);
    setCaret(before.length);
    setHighlight(0);
    requestAnimationFrame(() => {
      inputRef.current?.focus();
      inputRef.current?.setSelectionRange(before.length, before.length);
    });
  };

  const canSend = content.trim() !== '' && !sending;

  const send = async () => {
    if (!canSend) return;
    const codes = mentionables
      .filter((m) => content.includes(`@${m.label}`))
      .map((m) => m.organizationCode);
    try {
      await onSend({ content: content.trim(), mentionOrganizationCodes: codes });
      setContent('');
      setCaret(0);
    } catch {
      // 오류 문구는 부모가 error 로 보인다. 쓰던 글은 그대로 둔다.
    }
  };

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    // 한글 조합 중 Enter 는 글자 확정이다. 보내지 않는다.
    if (event.nativeEvent.isComposing) return;
    if (menuOpen) {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        const step = event.key === 'ArrowDown' ? 1 : -1;
        setHighlight((h) => (h + step + options.length) % options.length);
        return;
      }
      if (event.key === 'Enter' || event.key === 'Tab') {
        event.preventDefault();
        pickMention(options[Math.min(highlight, options.length - 1)]);
        return;
      }
    }
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      void send();
    }
  };

  return (
    <Stack gap={6} className={classes.composer}>
      <Group gap="xs" align="flex-end" wrap="nowrap">
        <Popover opened={menuOpen} position="top-start" width={260} shadow="card" radius="lg">
          <Popover.Target>
            <Textarea
              ref={inputRef}
              style={{ flex: 1 }}
              aria-label="메시지 입력"
              placeholder="메시지를 입력하세요. @ 로 기관을 부를 수 있습니다."
              autosize
              minRows={1}
              maxRows={6}
              value={content}
              onChange={(e) => {
                setContent(e.currentTarget.value);
                setCaret(e.currentTarget.selectionStart ?? e.currentTarget.value.length);
                setHighlight(0);
              }}
              onSelect={(e) => setCaret(e.currentTarget.selectionStart ?? 0)}
              onKeyDown={onKeyDown}
              maxLength={4000}
            />
          </Popover.Target>
          <Popover.Dropdown p={4}>
            <Stack gap={0} role="listbox" aria-label="부를 기관">
              {options.map((option, i) => (
                <UnstyledButton
                  key={option.organizationCode}
                  role="option"
                  aria-selected={i === highlight}
                  className={classes.room}
                  data-active={i === highlight || undefined}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => pickMention(option)}
                >
                  <Text size="md" fw={600}>
                    @{option.label}
                  </Text>
                </UnstyledButton>
              ))}
            </Stack>
          </Popover.Dropdown>
        </Popover>
        <ActionIcon
          size={40}
          radius="xl"
          aria-label="보내기"
          onClick={() => void send()}
          disabled={!canSend}
          loading={sending}
        >
          <IconSend size={20} stroke={1.8} />
        </ActionIcon>
      </Group>
      {error && (
        <Text size="sm" c="red">
          {error}
        </Text>
      )}
    </Stack>
  );
}
