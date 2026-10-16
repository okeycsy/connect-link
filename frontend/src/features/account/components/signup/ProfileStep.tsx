import {
  Badge,
  Box,
  Group,
  PasswordInput,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';

import { useBusinesses, useJobRoles, useOrganizations } from '../../../organization/hooks';
import classes from './SignUpSteps.module.css';
import type { SignUpForm } from './signUpForm';

/**
 * 가입 ④: 담당자 정보와 비밀번호. 부서·팀·직급·내선번호는 필수, 휴대폰번호는 선택이다(명세). 위에 앞 단계에서 고른 내용을 요약해
 * 신청 전에 한 번 더 보게 한다.
 */
export function ProfileStep({ form, email }: { form: SignUpForm; email: string }) {
  return (
    <Stack gap="lg">
      <Summary form={form} email={email} />
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" verticalSpacing="sm">
        <TextInput label="이름" withAsterisk autoComplete="name" {...form.getInputProps('name')} />
        <TextInput label="직급" withAsterisk {...form.getInputProps('jobTitle')} />
        <TextInput label="부서" withAsterisk {...form.getInputProps('department')} />
        <TextInput label="팀" withAsterisk {...form.getInputProps('team')} />
        <TextInput
          label="내선번호"
          withAsterisk
          placeholder="예: 1234"
          inputMode="tel"
          {...form.getInputProps('extensionPhone')}
        />
        <TextInput
          label="휴대폰번호 (선택)"
          placeholder="010-0000-0000"
          inputMode="tel"
          autoComplete="tel"
          {...form.getInputProps('mobilePhone')}
        />
        <PasswordInput
          label="비밀번호"
          withAsterisk
          description="영문, 숫자, 특수문자를 모두 넣어 8~64자"
          autoComplete="new-password"
          {...form.getInputProps('password')}
        />
        <PasswordInput
          label="비밀번호 확인"
          withAsterisk
          description="같은 비밀번호를 한 번 더"
          autoComplete="new-password"
          {...form.getInputProps('passwordConfirm')}
        />
      </SimpleGrid>
    </Stack>
  );
}

function Summary({ form, email }: { form: SignUpForm; email: string }) {
  const { data: organizations } = useOrganizations();
  const { data: businesses } = useBusinesses();
  const { data: jobRoles } = useJobRoles();
  const values = form.values;
  const organization = organizations?.find((o) => String(o.id) === values.organizationId);
  const jobRole = jobRoles?.find((j) => j.code === values.jobRole);
  const itRoleLabels = (jobRole?.itRoles ?? [])
    .filter((r) => values.itRoles.includes(r.code))
    .map((r) => r.label);

  return (
    <Box className={classes.summary} data-testid="sign-up-summary">
      <Stack gap={6}>
        <Row label="회사 이메일">
          <Text size="sm" ff="monospace">
            {email}
          </Text>
        </Row>
        <Row label="소속 기관">
          <Text size="sm">{organization?.name ?? '-'}</Text>
        </Row>
        <Row label="담당 업무">
          <Group gap={6}>
            {values.businesses.map((b) => (
              <Badge key={b.business} color="sky">
                {businesses?.find((x) => x.code === b.business)?.label ?? b.business}
                {b.subBusiness.trim() ? ` · ${b.subBusiness.trim()}` : ''}
              </Badge>
            ))}
          </Group>
        </Row>
        <Row label="직무">
          <Text size="sm">
            {jobRole?.label ?? '-'}
            {itRoleLabels.length > 0 ? `(${itRoleLabels.join(', ')})` : ''}
          </Text>
        </Row>
      </Stack>
    </Box>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <Group gap="sm" wrap="nowrap" align="center">
      <Text size="sm" c="dimmed" className={classes.summaryLabel}>
        {label}
      </Text>
      {children}
    </Group>
  );
}
