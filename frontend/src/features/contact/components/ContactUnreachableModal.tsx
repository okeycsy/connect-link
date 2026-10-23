import { Alert, Button, Checkbox, Group, Modal, Stack, Text, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconPhoneOff } from '@tabler/icons-react';
import { useEffect } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import { formatPhone } from '../../../shared/format';
import { type Contact, useMarkContactUnreachable } from '../hooks';

interface Props {
  contact: Contact | null;
  onClose: () => void;
}

/**
 * 연락 불가 표시. 야간 관제 등 결제원 담당자가 전화했는데 닿지 않을 때 메모를 남긴다. 그 업무 결제원 담당자에게는 늘 알리고, 고르면 그
 * 기관 담당자에게도 메일로 알린다. 담당자 정보를 고치면 표시가 지워진다.
 */
export function ContactUnreachableModal({ contact, onClose }: Props) {
  const mark = useMarkContactUnreachable();
  const form = useForm({ initialValues: { note: '', notifyOrganization: false } });
  const organization = contact?.organizationType === 'FINANCE_ORGANIZATION';

  useEffect(() => {
    if (contact) {
      form.setValues({ note: contact.unreachable?.note ?? '', notifyOrganization: false });
      mark.reset();
    }
    // 열 때마다 양식을 새로 채운다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [contact?.id]);

  const submit = form.onSubmit((values) => {
    if (!contact) return;
    mark.mutate(
      {
        id: contact.id,
        body: {
          note: values.note.trim() || null,
          notifyOrganization: organization && values.notifyOrganization,
        },
      },
      {
        onSuccess: () => {
          notifications.show({
            message: `${contact.name} 담당자를 연락 불가로 표시했습니다.`,
            color: 'clay',
          });
          onClose();
        },
        onError: (e) => form.setErrors(fieldErrorsOf(e)),
      },
    );
  });

  return (
    <Modal opened={contact != null} onClose={onClose} title="연락 불가 표시" centered>
      {contact && (
        <form onSubmit={submit}>
          <Stack gap="md">
            <div>
              <Text fw={700}>
                {contact.organizationName} {contact.name}
                {contact.jobTitle ? ` ${contact.jobTitle}` : ''}
              </Text>
              <Text size="sm" c="dimmed">
                내선 {formatPhone(contact.extensionPhone)}
                {contact.mobilePhone ? ` · 휴대폰 ${formatPhone(contact.mobilePhone)}` : ''}
              </Text>
            </div>
            <Textarea
              label="메모"
              placeholder="예: 02:10 내선·휴대폰 모두 받지 않음"
              autosize
              minRows={2}
              maxLength={500}
              data-autofocus
              {...form.getInputProps('note')}
            />
            {organization && (
              <Checkbox
                label={`${contact.organizationName} ${contact.businessLabel} 담당자에게도 메일로 알리기`}
                {...form.getInputProps('notifyOrganization', { type: 'checkbox' })}
              />
            )}
            <Text size="xs" c="dimmed">
              결제원 {contact.businessLabel} 담당자에게 매터모스트로 알립니다. 담당자 정보를
              고치거나 연락이 닿아 해제하면 표시가 지워집니다.
            </Text>
            {mark.error && Object.keys(fieldErrorsOf(mark.error)).length === 0 && (
              <Alert color="red" variant="light">
                {mark.error.message}
              </Alert>
            )}
            <Group justify="flex-end" gap="sm">
              <Button variant="default" onClick={onClose}>
                취소
              </Button>
              <Button
                type="submit"
                color="red"
                leftSection={<IconPhoneOff size={16} stroke={1.8} />}
                loading={mark.isPending}
              >
                연락 불가로 표시
              </Button>
            </Group>
          </Stack>
        </form>
      )}
    </Modal>
  );
}
