import {
  Badge,
  Button,
  Group,
  Modal,
  MultiSelect,
  ScrollArea,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';
import { useForm } from '@mantine/form';

import { fieldErrorsOf } from '../../../api/client';
import { OrgMonogram } from '../../../shared/OrgMonogram';
import type { BusinessCode } from '../../organization/hooks';
import {
  type ChatMember,
  type ChatRoomDetail,
  useCreateChatRoom,
  useInviteCandidates,
  useInviteChatMembers,
} from '../hooks';

/** 초대방 만들기. 이름만 정하고, 만든 뒤 바로 초대 창을 연다(초대 후보는 방이 생겨야 정해진다). */
export function CreateChatRoomModal({
  business,
  opened,
  onClose,
  onCreated,
}: {
  business: BusinessCode;
  opened: boolean;
  onClose: () => void;
  onCreated: (room: ChatRoomDetail) => void;
}) {
  const create = useCreateChatRoom();
  const form = useForm({
    initialValues: { name: '' },
    validate: { name: (v) => (v.trim() ? null : '방 이름을 입력해 주세요.') },
  });

  const submit = form.onSubmit((values) =>
    create.mutate(
      { business, name: values.name.trim(), memberEmails: [] },
      {
        onSuccess: (room) => {
          form.reset();
          onCreated(room);
        },
        onError: (error) => form.setErrors(fieldErrorsOf(error)),
      },
    ),
  );

  return (
    <Modal opened={opened} onClose={onClose} title="초대방 만들기">
      <form onSubmit={submit}>
        <Stack gap="md">
          <Text size="sm" c="dimmed">
            이 업무의 일부 기관이나 업무 밖 결제원 사람(대외계 등)과 따로 이야기할 방입니다. 만든 뒤
            사람을 초대합니다.
          </Text>
          <TextInput
            label="방 이름"
            placeholder="예: 오픈뱅킹 안심차단 전문 테스트"
            maxLength={100}
            data-autofocus
            {...form.getInputProps('name')}
          />
          {create.error && !Object.keys(fieldErrorsOf(create.error)).length && (
            <Text size="sm" c="red">
              {create.error.message}
            </Text>
          )}
          <Group justify="flex-end">
            <Button variant="outline" onClick={onClose}>
              취소
            </Button>
            <Button type="submit" loading={create.isPending}>
              만들기
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}

/** 사람 초대. 그 업무 참가기관 담당자와 결제원 사람 중 아직 구성원이 아닌 사람을 고른다. */
export function InviteChatMembersModal({
  roomId,
  opened,
  onClose,
}: {
  roomId: number;
  opened: boolean;
  onClose: () => void;
}) {
  const candidates = useInviteCandidates(roomId, opened);
  const invite = useInviteChatMembers(roomId);
  const form = useForm<{ emails: string[] }>({
    initialValues: { emails: [] },
    validate: { emails: (v) => (v.length ? null : '초대할 사람을 골라 주세요.') },
  });

  const groups = new Map<string, { value: string; label: string }[]>();
  for (const c of candidates.data ?? []) {
    const key = c.kftc ? '결제원' : c.organizationName;
    const items = groups.get(key) ?? [];
    items.push({ value: c.email, label: `${c.name} · ${c.roleLabel}` });
    groups.set(key, items);
  }
  const data = [...groups.entries()].map(([group, items]) => ({ group, items }));

  const close = () => {
    form.reset();
    invite.reset();
    onClose();
  };

  const submit = form.onSubmit((values) =>
    invite.mutate(values.emails, {
      onSuccess: close,
      onError: (error) => form.setErrors(fieldErrorsOf(error)),
    }),
  );

  return (
    <Modal opened={opened} onClose={close} title="사람 초대">
      <form onSubmit={submit}>
        <Stack gap="md">
          <MultiSelect
            label="초대할 사람"
            placeholder={candidates.isLoading ? '불러오는 중…' : '이름으로 찾기'}
            data={data}
            searchable
            nothingFoundMessage="초대할 수 있는 사람이 없습니다"
            comboboxProps={{ withinPortal: true }}
            {...form.getInputProps('emails')}
          />
          {invite.error && !Object.keys(fieldErrorsOf(invite.error)).length && (
            <Text size="sm" c="red">
              {invite.error.message}
            </Text>
          )}
          <Group justify="flex-end">
            <Button variant="outline" onClick={close}>
              취소
            </Button>
            <Button type="submit" loading={invite.isPending}>
              초대
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}

/** 구성원 보기. 결제원이 먼저, 그다음 기관 이름 순으로 서버가 준다. */
export function ChatMembersModal({
  members,
  opened,
  onClose,
}: {
  members: ChatMember[];
  opened: boolean;
  onClose: () => void;
}) {
  return (
    <Modal opened={opened} onClose={onClose} title={`구성원 ${members.length}명`}>
      <ScrollArea.Autosize mah={480}>
        <Stack gap="xs">
          {members.map((m) => (
            <Group key={m.email} gap="sm" wrap="nowrap">
              <OrgMonogram
                name={m.organizationName}
                type={m.kftc ? 'KFTC' : 'FINANCE_ORGANIZATION'}
                size={32}
              />
              <Stack gap={0} style={{ flex: 1, minWidth: 0 }}>
                <Text size="md" fw={600}>
                  {m.name}
                </Text>
                <Text size="sm" c="dimmed" truncate>
                  {m.organizationName} · {m.roleLabel}
                </Text>
              </Stack>
              {m.invited && (
                <Badge size="sm" color="gray" variant="outline">
                  초대
                </Badge>
              )}
            </Group>
          ))}
        </Stack>
      </ScrollArea.Autosize>
    </Modal>
  );
}
