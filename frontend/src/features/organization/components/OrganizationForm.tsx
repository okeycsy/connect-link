import { Button, Checkbox, Group, Select, Stack, Switch, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import {
  useBusinesses,
  useCreateOrganization,
  useFinanceSectors,
  useUpdateOrganization,
  type BusinessCode,
  type FinanceSector,
  type OrganizationDetail,
  type OrganizationType,
} from '../hooks';

const TYPE_OPTIONS: { value: OrganizationType; label: string }[] = [
  { value: 'KFTC', label: '결제원' },
  { value: 'FINANCE_ORGANIZATION', label: '참가기관' },
  { value: 'USE_ORGANIZATION', label: '이용기관' },
];

interface FormValues {
  code: string;
  name: string;
  type: OrganizationType;
  emailDomain: string;
  active: boolean;
  businesses: BusinessCode[];
  /** 업권. 참가기관만 쓴다. */
  sector: FinanceSector | null;
}

interface Props {
  /** 수정할 기관. 없으면 등록 폼이다. */
  organization?: OrganizationDetail;
  /** 등록 폼에서 미리 체크해 둘 업무(현재 레일). */
  defaultBusiness?: BusinessCode;
  onSaved: (saved: OrganizationDetail) => void;
  onCancel: () => void;
}

/** 기관 등록·수정 폼. 검증 메시지는 서버의 필드 오류를 그대로 보여 준다. 코드와 구분은 등록 때만 입력한다. */
export function OrganizationForm({ organization, defaultBusiness, onSaved, onCancel }: Props) {
  const { data: businesses } = useBusinesses();
  const { data: sectors } = useFinanceSectors();
  const create = useCreateOrganization();
  const update = useUpdateOrganization();
  const editing = organization != null;

  const form = useForm<FormValues>({
    initialValues: organization
      ? {
          code: organization.code,
          name: organization.name,
          type: organization.type,
          emailDomain: organization.emailDomain ?? '',
          active: organization.active,
          businesses: organization.businesses.map((b) => b.code),
          sector: organization.sector ?? null,
        }
      : {
          code: '',
          name: '',
          type: 'FINANCE_ORGANIZATION',
          emailDomain: '',
          active: true,
          businesses: defaultBusiness ? [defaultBusiness] : [],
          sector: null,
        },
  });

  const submit = form.onSubmit(async (values) => {
    try {
      const saved = organization
        ? await update.mutateAsync({
            id: organization.id,
            body: {
              name: values.name,
              emailDomain: values.emailDomain || null,
              active: values.active,
              businesses: values.businesses,
              sector: values.sector,
            },
          })
        : await create.mutateAsync({
            code: values.code.trim().toUpperCase(),
            name: values.name,
            type: values.type,
            emailDomain: values.emailDomain || null,
            businesses: values.businesses,
            sector: values.type === 'FINANCE_ORGANIZATION' ? values.sector : null,
          });
      notifications.show({
        message: organization ? '기관 정보를 저장했습니다.' : '기관을 등록했습니다.',
        color: 'clay',
      });
      onSaved(saved);
    } catch (error) {
      const fieldErrors = fieldErrorsOf(error);
      if (Object.keys(fieldErrors).length > 0) {
        form.setErrors(fieldErrors);
        return;
      }
      notifications.show({
        title: '저장하지 못했습니다',
        message: error instanceof ApiError ? error.message : '잠시 후 다시 시도해 주세요.',
        color: 'red',
      });
    }
  });

  const saving = create.isPending || update.isPending;
  const finance = (organization?.type ?? form.values.type) === 'FINANCE_ORGANIZATION';

  return (
    <form onSubmit={submit} noValidate>
      <Stack gap="md">
        {!editing && (
          <Group grow align="flex-start">
            <TextInput
              label="기관 코드"
              description="영문 대문자·숫자 2~20자. 등록 뒤 바꾸지 않습니다."
              placeholder="예: 004"
              required
              {...form.getInputProps('code')}
              onChange={(e) => form.setFieldValue('code', e.currentTarget.value.toUpperCase())}
            />
            <Select
              label="기관 구분"
              data={TYPE_OPTIONS}
              allowDeselect={false}
              required
              {...form.getInputProps('type')}
            />
          </Group>
        )}
        <TextInput
          label="기관명"
          placeholder="예: KB국민은행"
          required
          {...form.getInputProps('name')}
        />
        {finance && (
          <Select
            label="업권"
            description="의견 요청 대상을 업권별로 고를 때 씁니다."
            placeholder="업권 고르기"
            data={sectors?.map((s) => ({ value: s.code, label: s.label })) ?? []}
            allowDeselect={false}
            required
            {...form.getInputProps('sector')}
          />
        )}
        <TextInput
          label="회사 이메일 도메인"
          description="가입 승인 때 신청자 이메일이 이 기관과 맞는지 볼 때 씁니다."
          placeholder="예: kb.example.com"
          {...form.getInputProps('emailDomain')}
        />
        <Checkbox.Group
          label="참여 업무"
          description="레일의 업무 영역에서 이 기관이 보이는 범위입니다."
          {...form.getInputProps('businesses')}
        >
          <Group gap="md" mt={6}>
            {businesses?.map((b) => (
              <Checkbox key={b.code} value={b.code} label={b.label} />
            ))}
          </Group>
        </Checkbox.Group>
        {editing && (
          <Switch
            label="사용 중"
            description="끄면 공개 목록과 가입 선택지에서 빠집니다. 이력은 남습니다."
            {...form.getInputProps('active', { type: 'checkbox' })}
          />
        )}
        {organization && (
          <Text size="xs" c="dimmed">
            등록 {organization.createdAt.slice(0, 10)} ({organization.createdBy}) · 수정{' '}
            {organization.updatedAt.slice(0, 16).replace('T', ' ')} ({organization.updatedBy})
          </Text>
        )}
        <Group justify="flex-end" gap="xs">
          <Button variant="subtle" color="gray" onClick={onCancel} disabled={saving}>
            닫기
          </Button>
          <Button type="submit" loading={saving}>
            {editing ? '저장' : '등록'}
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
