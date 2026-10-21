import {
  Badge,
  Button,
  Checkbox,
  Chip,
  Group,
  Modal,
  ScrollArea,
  Select,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import { OrgMonogram } from '../../../shared/OrgMonogram';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import { type BusinessCode, useBusinesses, useJobRoles } from '../../organization/hooks';
import {
  type ChatMember,
  type ChatRoomDetail,
  filterInviteCandidates,
  groupInviteCandidates,
  type InviteFilter,
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

/**
 * 사람 초대. 이름으로 찾지 않고 담당 업무 → 소속(결제원/참가기관) → 직무(현업/전산, 전산 세부 직무)로 거른 뒤, 기관 묶음이나 보이는 사람
 * 전부를 한 번에 고른다. 고른 사람은 거르기를 바꿔도 남는다. 후보는 아직 구성원이 아닌 사람만 서버가 준다.
 */
export function InviteChatMembersModal({
  roomId,
  business,
  opened,
  onClose,
}: {
  roomId: number;
  business: BusinessCode;
  opened: boolean;
  onClose: () => void;
}) {
  const candidates = useInviteCandidates(roomId, opened);
  const businesses = useBusinesses();
  const jobRoles = useJobRoles();
  const invite = useInviteChatMembers(roomId);
  const initialFilter: InviteFilter = { business, side: 'ALL', jobRole: 'ALL', itRoles: [] };
  const [filter, setFilter] = useState<InviteFilter>(initialFilter);
  const [selected, setSelected] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  const visible = filterInviteCandidates(candidates.data ?? [], filter);
  const groups = groupInviteCandidates(visible);
  const visibleEmails = visible.map((c) => c.email);
  const itRoleOptions = jobRoles.data?.find((r) => r.code === 'IT')?.itRoles ?? [];

  const toggle = (emails: string[], on: boolean) => {
    setError(null);
    setSelected((prev) =>
      on ? [...new Set([...prev, ...emails])] : prev.filter((e) => !emails.includes(e)),
    );
  };

  const close = () => {
    setFilter(initialFilter);
    setSelected([]);
    setError(null);
    invite.reset();
    onClose();
  };

  const submit = () => {
    if (!selected.length) {
      setError('초대할 사람을 골라 주세요.');
      return;
    }
    invite.mutate(selected, {
      onSuccess: close,
      onError: (e) => setError(fieldErrorsOf(e).emails ?? e.message),
    });
  };

  return (
    <Modal opened={opened} onClose={close} title="사람 초대" size="lg">
      <Stack gap="md">
        <Select
          label="담당 업무"
          data={[
            { value: 'ALL', label: '전체 업무' },
            ...(businesses.data ?? []).map((b) => ({ value: b.code, label: b.label })),
          ]}
          value={filter.business}
          onChange={(v) => v && setFilter({ ...filter, business: v as InviteFilter['business'] })}
          allowDeselect={false}
          comboboxProps={{ withinPortal: true }}
        />
        <Group gap="md" wrap="wrap">
          <ChipFilter
            label="소속"
            value={filter.side}
            options={[
              { value: 'ALL', label: '전체' },
              { value: 'KFTC', label: '결제원' },
              { value: 'ORGANIZATION', label: '참가기관' },
            ]}
            onChange={(side) => setFilter({ ...filter, side })}
          />
          <ChipFilter
            label="직무"
            value={filter.jobRole}
            options={[
              { value: 'ALL', label: '전체' },
              ...(jobRoles.data ?? []).map((r) => ({ value: r.code, label: r.label })),
            ]}
            onChange={(jobRole) => setFilter({ ...filter, jobRole, itRoles: [] })}
          />
        </Group>
        {filter.jobRole === 'IT' && itRoleOptions.length > 0 && (
          <Group gap={6} role="group" aria-label="전산 세부 직무">
            <Text size="xs" c="dimmed" fw={600}>
              전산 세부 직무
            </Text>
            <Chip.Group
              multiple
              value={filter.itRoles}
              onChange={(v) => setFilter({ ...filter, itRoles: v as InviteFilter['itRoles'] })}
            >
              <Group gap={4}>
                {itRoleOptions.map((r) => (
                  <Chip key={r.code} value={r.code} size="xs">
                    {r.label}
                  </Chip>
                ))}
              </Group>
            </Chip.Group>
          </Group>
        )}

        <Group justify="space-between" wrap="nowrap">
          <SelectAllCheckbox
            label={`보이는 ${visible.length}명 모두`}
            emails={visibleEmails}
            selected={selected}
            onToggle={toggle}
          />
          <Text size="sm" fw={600} c={selected.length ? undefined : 'dimmed'}>
            {selected.length}명 선택
          </Text>
        </Group>

        <ScrollArea.Autosize mah={360} type="auto">
          {candidates.isLoading ? (
            <Text size="sm" c="dimmed">
              불러오는 중…
            </Text>
          ) : groups.length === 0 ? (
            <Text size="sm" c="dimmed">
              조건에 맞는 초대할 사람이 없습니다.
            </Text>
          ) : (
            <Stack gap="sm">
              {groups.map((g) => (
                <Stack key={g.organizationCode} gap={4} data-testid="invite-group">
                  <Group gap="xs" wrap="nowrap">
                    <OrgMonogram
                      name={g.label}
                      type={g.kftc ? 'KFTC' : 'FINANCE_ORGANIZATION'}
                      size={24}
                    />
                    <SelectAllCheckbox
                      label={`${g.label} ${g.items.length}명`}
                      emails={g.items.map((c) => c.email)}
                      selected={selected}
                      onToggle={toggle}
                      bold
                    />
                  </Group>
                  <Stack gap={4} pl={32}>
                    {g.items.map((c) => (
                      <Checkbox
                        key={c.email}
                        checked={selected.includes(c.email)}
                        onChange={(e) => toggle([c.email], e.currentTarget.checked)}
                        label={
                          <Text size="sm" span>
                            {c.name}{' '}
                            <Text size="sm" c="dimmed" span>
                              · {c.roleLabel}
                            </Text>
                          </Text>
                        }
                      />
                    ))}
                  </Stack>
                </Stack>
              ))}
            </Stack>
          )}
        </ScrollArea.Autosize>

        {error && (
          <Text size="sm" c="red">
            {error}
          </Text>
        )}
        <Group justify="flex-end">
          <Button variant="outline" onClick={close}>
            취소
          </Button>
          <Button onClick={submit} loading={invite.isPending}>
            {selected.length ? `${selected.length}명 초대` : '초대'}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}

/** 여러 사람을 한 번에 고르는 체크박스. 일부만 골랐으면 반쯤 찬 표시다. */
function SelectAllCheckbox({
  label,
  emails,
  selected,
  onToggle,
  bold,
}: {
  label: string;
  emails: string[];
  selected: string[];
  onToggle: (emails: string[], on: boolean) => void;
  bold?: boolean;
}) {
  const count = emails.filter((e) => selected.includes(e)).length;
  return (
    <Checkbox
      label={
        <Text size="sm" fw={bold ? 600 : undefined} span>
          {label}
        </Text>
      }
      checked={emails.length > 0 && count === emails.length}
      indeterminate={count > 0 && count < emails.length}
      disabled={emails.length === 0}
      onChange={() => onToggle(emails, count < emails.length)}
    />
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
