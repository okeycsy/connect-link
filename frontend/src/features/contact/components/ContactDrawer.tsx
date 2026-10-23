import { Alert, Badge, Button, Divider, Group, Skeleton, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconPhoneOff, IconTrash, IconUserCheck } from '@tabler/icons-react';
import { useState } from 'react';

import { ConfirmModal } from '../../../shared/ConfirmModal';
import { formatDate, formatPhone } from '../../../shared/format';
import { DetailDrawer } from '../../../shared/list/DetailDrawer';
import { OrgMonogram } from '../../../shared/OrgMonogram';
import { ErrorState } from '../../../shared/state/StateView';
import { ChangeHistoryList } from '../../history/components/ChangeHistoryList';
import { useJobRoles } from '../../organization/hooks';
import {
  type Contact,
  useClearContactUnreachable,
  useConfirmContact,
  useContact,
  useDeleteContact,
} from '../hooks';
import { unreachableSummary } from '../unreachable';
import { ContactNeedsConfirmBadge, ContactRankBadge } from './ContactBadges';
import classes from './ContactTable.module.css';

interface Props {
  contactId: number | null;
  onClose: () => void;
  onEdit: (contact: Contact) => void;
  /** 연락 불가 표시 창을 연다. */
  onMarkUnreachable: (contact: Contact) => void;
  /** 결제원 담당자가 인터넷망에서 볼 때. 고치기·지우기가 막힌 까닭을 알린다. */
  kftcExternal: boolean;
}

/**
 * 담당자 상세 패널. 연락처, 최근 확인일, 이전 담당자 한 명까지의 변경 이력(휴대폰은 가려서)을 보이고, 권한에 따라 수정·"변동 없음"
 * 확인·삭제를 한다. 연락 불가 표시가 있으면 맨 위에 빨갛게 보이고, 결제원 쪽은 "연락 불가" 로 남기며 고칠 수 있는 사람은 해제한다. 버튼
 * 표시는 서버가 내려 준 권한(permissions)을 따른다.
 */
export function ContactDrawer({
  contactId,
  onClose,
  onEdit,
  onMarkUnreachable,
  kftcExternal,
}: Props) {
  const { data: contact, isPending, isError, error } = useContact(contactId);
  const confirm = useConfirmContact();
  const clearUnreachable = useClearContactUnreachable();
  const remove = useDeleteContact();
  const [deleting, setDeleting] = useState(false);
  const { data: jobRoles } = useJobRoles();
  const itRoleLabels = new Map(
    jobRoles?.flatMap((j) => j.itRoles).map((r) => [r.code, r.label] as const) ?? [],
  );

  const footer = contact && (
    <>
      {contact.permissions.delete && (
        <Button
          color="red"
          variant="light"
          leftSection={<IconTrash size={16} stroke={1.8} />}
          onClick={() => {
            remove.reset();
            setDeleting(true);
          }}
          mr="auto"
        >
          삭제
        </Button>
      )}
      {contact.permissions.markUnreachable && (
        <Button
          color="red"
          variant="light"
          leftSection={<IconPhoneOff size={16} stroke={1.8} />}
          onClick={() => onMarkUnreachable(contact)}
          mr={contact.permissions.delete ? undefined : 'auto'}
        >
          연락 불가
        </Button>
      )}
      {contact.permissions.edit && (
        <>
          <Button
            variant="light"
            leftSection={<IconUserCheck size={16} stroke={1.8} />}
            loading={confirm.isPending}
            onClick={() =>
              confirm.mutate(contact.id, {
                onSuccess: () =>
                  notifications.show({
                    message: '변동 없음으로 확인했습니다. 최근 확인일이 오늘로 바뀌었습니다.',
                    color: 'clay',
                  }),
              })
            }
          >
            변동 없음
          </Button>
          <Button onClick={() => onEdit(contact)}>수정</Button>
        </>
      )}
    </>
  );

  return (
    <DetailDrawer
      opened={contactId != null}
      onClose={onClose}
      title={contact ? contact.name : '담당자'}
      subtitle={contact ? `${contact.organizationName} · ${contact.businessLabel}` : undefined}
      footer={footer}
    >
      {isError && <ErrorState error={error} />}
      {isPending && contactId != null && (
        <Stack gap="sm">
          <Skeleton h={44} />
          <Skeleton h={120} />
        </Stack>
      )}
      {contact && (
        <Stack gap="lg" data-testid="contact-detail">
          {contact.unreachable && (
            <Alert
              variant="light"
              color="red"
              icon={<IconPhoneOff size={18} stroke={1.8} />}
              title="연락 불가"
              data-testid="contact-unreachable-alert"
            >
              <Stack gap={8}>
                <Text size="sm">{unreachableSummary(contact.unreachable)}</Text>
                <Text size="xs" c="dimmed">
                  담당자 정보를 고치면 표시가 지워집니다.
                </Text>
                {contact.permissions.clearUnreachable && (
                  <Group>
                    <Button
                      size="xs"
                      variant="default"
                      loading={clearUnreachable.isPending}
                      onClick={() =>
                        clearUnreachable.mutate(contact.id, {
                          onSuccess: () =>
                            notifications.show({
                              message: '연락 불가 표시를 해제했습니다.',
                              color: 'clay',
                            }),
                          onError: (e) => notifications.show({ message: e.message, color: 'red' }),
                        })
                      }
                    >
                      연락됨 · 해제
                    </Button>
                  </Group>
                )}
              </Stack>
            </Alert>
          )}
          <Group gap="sm" wrap="nowrap">
            <OrgMonogram
              name={contact.organizationName}
              type={contact.organizationType}
              size={40}
            />
            <div>
              <Group gap={6}>
                <ContactRankBadge rank={contact.rank} label={contact.rankLabel} />
                <Badge color="gray" variant="outline">
                  {contact.jobRoleLabel}
                </Badge>
                {contact.self && <Badge color="clay">나</Badge>}
              </Group>
              <Text size="sm" c="dimmed" mt={4}>
                {[contact.department, contact.team, contact.jobTitle].filter(Boolean).join(' · ')}
              </Text>
            </div>
          </Group>

          <Stack gap={8}>
            <Field label="세부업무" value={contact.subBusiness || '-'} />
            <Field label="내선번호" value={formatPhone(contact.extensionPhone)} mono />
            <Field label="휴대폰번호" value={formatPhone(contact.mobilePhone) || '-'} mono />
            <Field label="회사 이메일" value={contact.email} mono />
            {contact.itRoles.length > 0 && (
              <Field
                label="전산 세부 직무"
                value={contact.itRoles.map((r) => itRoleLabels.get(r) ?? r).join(', ')}
              />
            )}
            <Field
              label="최근 확인일"
              value={
                <Group gap={6}>
                  <Text size="md" className={classes.numeric}>
                    {formatDate(contact.lastConfirmedAt)}
                  </Text>
                  <Text size="sm" c="dimmed">
                    {contact.lastConfirmedByName}
                  </Text>
                  {contact.needsConfirm && <ContactNeedsConfirmBadge />}
                </Group>
              }
            />
          </Stack>

          {kftcExternal && (
            <Alert variant="light" color="amber" data-testid="contact-internal-only">
              결제원 담당자의 수정·삭제는 내부망에서만 합니다.
            </Alert>
          )}

          <Divider />
          <div>
            <Text fw={700} size="lg" mb="xs">
              변경 이력
            </Text>
            <Text size="xs" c="dimmed" mb="sm">
              이전 담당자 한 명까지 보입니다. 내선번호는 그대로, 휴대폰번호와 이메일은 가려서
              보입니다.
            </Text>
            <ChangeHistoryList
              targetType="CONTACT"
              targetId={contact.id}
              size={1}
              emptyMessage="고친 적이 없습니다."
            />
          </div>
        </Stack>
      )}

      <ConfirmModal
        opened={deleting}
        onClose={() => setDeleting(false)}
        title="담당자 삭제"
        message={
          contact
            ? `${contact.organizationName} ${contact.name} 담당자를 지웁니다. 계정은 그대로 남습니다.`
            : ''
        }
        confirmLabel="삭제"
        danger
        pending={remove.isPending}
        error={remove.error}
        onConfirm={() =>
          contact &&
          remove.mutate(contact.id, {
            onSuccess: () => {
              setDeleting(false);
              notifications.show({ message: '담당자를 지웠습니다.', color: 'clay' });
              onClose();
            },
          })
        }
      />
    </DetailDrawer>
  );
}

function Field({ label, value, mono }: { label: string; value: React.ReactNode; mono?: boolean }) {
  return (
    <Group gap="sm" wrap="nowrap" align="baseline">
      <Text size="sm" c="dimmed" w={110} style={{ flex: '0 0 auto' }}>
        {label}
      </Text>
      {typeof value === 'string' ? (
        <Text
          size="md"
          ff={mono ? 'monospace' : undefined}
          style={{ minWidth: 0, overflowWrap: 'anywhere' }}
        >
          {value}
        </Text>
      ) : (
        value
      )}
    </Group>
  );
}
