import {
  Alert,
  Button,
  Checkbox,
  Group,
  Modal,
  SegmentedControl,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect } from 'react';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { useJobRoles } from '../../organization/hooks';
import {
  type BusinessCode,
  type Contact,
  type ContactOrganization,
  type ContactRank,
  type ItRole,
  type JobRole,
  useCreateContact,
  useUpdateContact,
} from '../hooks';

interface FormValues {
  organizationId: string | null;
  subBusiness: string;
  name: string;
  department: string;
  team: string;
  jobTitle: string;
  jobRole: JobRole;
  itRoles: ItRole[];
  rank: ContactRank;
  extensionPhone: string;
  mobilePhone: string;
  email: string;
}

interface Props {
  opened: boolean;
  onClose: () => void;
  business: BusinessCode;
  /** 고칠 담당자. 없으면 등록이다. */
  contact?: Contact | null;
  /** 등록 때 고를 수 있는 기관. 하나면(기관 담당자) 고정이다. */
  organizations?: ContactOrganization[];
  onSaved?: (saved: Contact) => void;
}

const RANK_OPTIONS = [
  { value: 'PRIMARY', label: '주담당' },
  { value: 'SECONDARY', label: '부담당' },
];

function valuesOf(contact: Contact | null | undefined, organizationId: string | null): FormValues {
  return {
    organizationId: contact ? String(contact.organizationId) : organizationId,
    subBusiness: contact?.subBusiness ?? '',
    name: contact?.name ?? '',
    department: contact?.department ?? '',
    team: contact?.team ?? '',
    jobTitle: contact?.jobTitle ?? '',
    jobRole: contact?.jobRole ?? 'BUSINESS',
    itRoles: contact?.itRoles ?? [],
    rank: contact?.rank ?? 'PRIMARY',
    extensionPhone: contact?.extensionPhone ?? '',
    mobilePhone: contact?.mobilePhone ?? '',
    email: contact?.email ?? '',
  };
}

/**
 * 담당자 등록·수정 모달. 저장하면 최근 확인일이 오늘로 바뀐다. 기관 담당자는 자기 기관에만 등록한다(서버가 기관을 정한다).
 * 검증 메시지는 서버의 필드 오류를 그대로 보여 준다.
 */
export function ContactFormModal({
  opened,
  onClose,
  business,
  contact,
  organizations = [],
  onSaved,
}: Props) {
  const create = useCreateContact();
  const update = useUpdateContact();
  const { data: jobRoles } = useJobRoles();
  const editing = contact != null;
  const fixedOrganization = organizations.length === 1 ? String(organizations[0].id) : null;

  const form = useForm<FormValues>({ initialValues: valuesOf(contact, fixedOrganization) });

  // 열 때마다 고칠 담당자 값으로 다시 채운다.
  useEffect(() => {
    if (opened) {
      form.setValues(valuesOf(contact, fixedOrganization));
      form.resetDirty();
      form.clearErrors();
      create.reset();
      update.reset();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened, contact?.id]);

  const itRoleOptions = jobRoles?.find((r) => r.code === 'IT')?.itRoles ?? [];

  const submit = form.onSubmit(async (values) => {
    const body = {
      business,
      organizationId: values.organizationId ? Number(values.organizationId) : null,
      subBusiness: values.subBusiness.trim() || null,
      name: values.name.trim(),
      department: values.department.trim(),
      team: values.team.trim() || null,
      jobTitle: values.jobTitle.trim() || null,
      jobRole: values.jobRole,
      itRoles: values.jobRole === 'IT' ? values.itRoles : [],
      rank: values.rank,
      extensionPhone: values.extensionPhone.trim(),
      mobilePhone: values.mobilePhone.trim() || null,
      email: values.email.trim(),
    };
    try {
      const saved = contact
        ? await update.mutateAsync({ id: contact.id, body })
        : await create.mutateAsync(body);
      notifications.show({
        message: contact ? '담당자 정보를 저장했습니다.' : '담당자를 등록했습니다.',
        color: 'clay',
      });
      onSaved?.(saved);
      onClose();
    } catch (error) {
      const fieldErrors = fieldErrorsOf(error);
      if (Object.keys(fieldErrors).length > 0) {
        form.setErrors(fieldErrors);
      }
    }
  });

  const mutation = editing ? update : create;
  const generalError =
    mutation.error && Object.keys(fieldErrorsOf(mutation.error)).length === 0
      ? mutation.error
      : null;

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={editing ? `${contact.name} 담당자 수정` : '담당자 등록'}
      centered
      size="lg"
    >
      <form onSubmit={submit} noValidate>
        <Stack gap="md">
          {!editing && organizations.length > 1 && (
            <Select
              label="기관"
              placeholder="기관 고르기"
              required
              searchable
              data={organizations.map((o) => ({ value: String(o.id), label: o.name }))}
              {...form.getInputProps('organizationId')}
            />
          )}
          {!editing && fixedOrganization && (
            <Text size="sm" c="dimmed">
              {organizations[0].name}의 담당자로 등록합니다. 후임자나 새 담당자를 미리 올려 둘 수
              있습니다.
            </Text>
          )}
          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
            <TextInput label="이름" required {...form.getInputProps('name')} />
            <TextInput
              label="회사 이메일"
              required
              placeholder="name@example.com"
              {...form.getInputProps('email')}
            />
            <TextInput label="부서" required {...form.getInputProps('department')} />
            <TextInput label="팀" {...form.getInputProps('team')} />
            <TextInput label="직급" {...form.getInputProps('jobTitle')} />
            <TextInput
              label="세부업무"
              placeholder="예: 계좌정보 통합조회"
              {...form.getInputProps('subBusiness')}
            />
            <TextInput
              label="내선번호"
              required
              placeholder="예: 02-0000-0000 또는 1234"
              {...form.getInputProps('extensionPhone')}
            />
            <TextInput
              label="휴대폰번호"
              placeholder="010-0000-0000"
              {...form.getInputProps('mobilePhone')}
            />
          </SimpleGrid>
          <Group gap="xl" align="flex-start">
            <Stack gap={4}>
              <Text size="sm" fw={500}>
                직무
              </Text>
              <SegmentedControl
                aria-label="직무"
                data={[
                  { value: 'BUSINESS', label: '현업' },
                  { value: 'IT', label: '전산' },
                ]}
                {...form.getInputProps('jobRole')}
              />
            </Stack>
            <Stack gap={4}>
              <Text size="sm" fw={500}>
                주/부담당
              </Text>
              <SegmentedControl
                aria-label="주/부담당"
                data={RANK_OPTIONS}
                {...form.getInputProps('rank')}
              />
            </Stack>
          </Group>
          {form.values.jobRole === 'IT' && (
            <Checkbox.Group
              label="전산 세부 직무"
              description="하나 이상 고릅니다."
              {...form.getInputProps('itRoles')}
            >
              <Group gap="md" mt={6}>
                {itRoleOptions.map((r) => (
                  <Checkbox key={r.code} value={r.code} label={r.label} />
                ))}
              </Group>
            </Checkbox.Group>
          )}
          {generalError && (
            <Alert variant="light" color="red" data-testid="contact-form-error">
              {generalError instanceof ApiError ? generalError.message : '저장하지 못했습니다.'}
            </Alert>
          )}
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={onClose} disabled={mutation.isPending}>
              닫기
            </Button>
            <Button type="submit" loading={mutation.isPending}>
              {editing ? '저장' : '등록'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
