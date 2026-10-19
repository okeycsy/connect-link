import { Badge, Box, Button, Card, Group, Skeleton, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconUserCheck } from '@tabler/icons-react';
import { useState } from 'react';

import { formatDate, formatPhone } from '../../../shared/format';
import { ErrorState } from '../../../shared/state/StateView';
import { type Contact, useConfirmContact, useMyContacts } from '../hooks';
import { ContactNeedsConfirmBadge, ContactRankBadge } from './ContactBadges';
import { ContactFormModal } from './ContactFormModal';
import classes from './ContactTable.module.css';

/**
 * 내 정보 화면의 연락처 카드. 업무마다 하나씩 있는 내 담당자 정보(가입 때 적은 값으로 시작)를 보이고, 고치거나 "변동 없음"으로 확인해
 * 최근 확인일을 갱신한다. 버튼은 서버가 내려 준 권한을 따른다(결제원 담당자는 내부망에서만 고친다).
 */
export function MyContactsCard() {
  const { data: contacts, isPending, isError, error } = useMyContacts();
  const confirm = useConfirmContact();
  const [editing, setEditing] = useState<Contact | null>(null);

  return (
    <Card h="100%" data-testid="my-contact">
      <Text fw={700} size="lg" mb={4}>
        연락처
      </Text>
      <Text size="sm" c="dimmed" mb="md">
        담당 업무마다 연락처에 올라가는 내 정보입니다. 고치면 최근 확인일이 갱신되고, 바뀐 것이
        없으면 "변동 없음"으로 확인합니다.
      </Text>
      {isError && <ErrorState error={error} />}
      {isPending && (
        <Stack gap="xs">
          <Skeleton h={72} />
          <Skeleton h={72} />
        </Stack>
      )}
      {contacts && contacts.length === 0 && (
        <Text size="sm" c="dimmed">
          연락처에 등록된 내 담당자 정보가 없습니다.
        </Text>
      )}
      <Stack gap={8}>
        {contacts?.map((c) => (
          <Box key={c.id} className={classes.campaignRow} data-testid="my-contact-row">
            <Group justify="space-between" align="flex-start" wrap="nowrap" gap="sm">
              <Stack gap={2} style={{ minWidth: 0 }}>
                <Group gap={6}>
                  <Badge color="sky">{c.businessLabel}</Badge>
                  <ContactRankBadge rank={c.rank} label={c.rankLabel} size="sm" />
                  {c.subBusiness && (
                    <Text size="sm" c="dimmed" lineClamp={1}>
                      {c.subBusiness}
                    </Text>
                  )}
                </Group>
                <Text size="sm">
                  {[c.department, c.team, c.jobTitle].filter(Boolean).join(' · ')} ·{' '}
                  {c.jobRoleLabel}
                </Text>
                <Text size="sm" className={classes.numeric}>
                  내선 {formatPhone(c.extensionPhone)} · 휴대폰 {formatPhone(c.mobilePhone) || '-'}
                </Text>
                <Group gap={6}>
                  <Text size="xs" c="dimmed" className={classes.numeric}>
                    최근 확인일 {formatDate(c.lastConfirmedAt)}
                  </Text>
                  {c.needsConfirm && <ContactNeedsConfirmBadge size="xs" />}
                </Group>
              </Stack>
              {c.permissions.edit && (
                <Group gap="xs" wrap="nowrap">
                  <Button
                    size="xs"
                    variant="light"
                    leftSection={<IconUserCheck size={14} stroke={1.8} />}
                    loading={confirm.isPending && confirm.variables === c.id}
                    onClick={() =>
                      confirm.mutate(c.id, {
                        onSuccess: () =>
                          notifications.show({
                            message: `${c.businessLabel} 담당자 정보를 변동 없음으로 확인했습니다.`,
                            color: 'clay',
                          }),
                      })
                    }
                  >
                    변동 없음
                  </Button>
                  <Button size="xs" variant="outline" onClick={() => setEditing(c)}>
                    수정
                  </Button>
                </Group>
              )}
            </Group>
          </Box>
        ))}
      </Stack>
      {editing && (
        <ContactFormModal
          opened
          onClose={() => setEditing(null)}
          business={editing.business}
          contact={editing}
        />
      )}
    </Card>
  );
}
