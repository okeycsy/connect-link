import { Checkbox, Group, Radio, Skeleton, Stack, Text } from '@mantine/core';

import { useJobRoles } from '../../../organization/hooks';
import type { ItRoleCode, JobRoleCode, SignUpForm } from './signUpForm';

/** 가입 ③: 직무. 현업이거나 전산이고, 전산이면 세부 직무를 하나 이상(복수) 고른다. 선택지와 이름은 서버가 준다. */
export function JobRoleStep({ form }: { form: SignUpForm }) {
  const { data: jobRoles } = useJobRoles();
  const itRoles = jobRoles?.find((j) => j.code === 'IT')?.itRoles ?? [];

  if (!jobRoles) {
    return <Skeleton h={120} />;
  }

  return (
    <Stack gap="lg">
      <Radio.Group
        label="직무"
        withAsterisk
        value={form.values.jobRole}
        error={form.errors.jobRole}
        onChange={(value) => {
          form.setFieldValue('jobRole', value as JobRoleCode);
          form.clearFieldError('jobRole');
          if (value !== 'IT') form.setFieldValue('itRoles', []);
        }}
      >
        <Group mt="xs" gap="lg">
          {jobRoles.map((j) => (
            <Radio key={j.code} value={j.code} label={j.label} />
          ))}
        </Group>
      </Radio.Group>

      {form.values.jobRole === 'IT' ? (
        <Checkbox.Group
          label="전산 세부 직무"
          description="여러 개 고를 수 있습니다."
          withAsterisk
          value={form.values.itRoles}
          error={form.errors.itRoles}
          onChange={(value) => {
            form.setFieldValue('itRoles', value as ItRoleCode[]);
            form.clearFieldError('itRoles');
          }}
        >
          <Group mt="xs" gap="lg">
            {itRoles.map((r) => (
              <Checkbox key={r.code} value={r.code} label={r.label} />
            ))}
          </Group>
        </Checkbox.Group>
      ) : (
        <Text size="sm" c="dimmed">
          전산을 고르면 맡은 세부 직무를 고릅니다.
        </Text>
      )}
    </Stack>
  );
}
