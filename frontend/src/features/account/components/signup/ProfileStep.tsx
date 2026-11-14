import {
  Anchor,
  Badge,
  Box,
  Checkbox,
  Group,
  PasswordInput,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
} from '@mantine/core';
import { IconExternalLink } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

import tableClasses from '../../../../shared/list/ResponsiveTable.module.css';
import { useBusinesses, useJobRoles, useOrganizations } from '../../../organization/hooks';
import {
  OPTIONAL_ITEMS,
  REQUIRED_ITEMS,
  SIGN_UP_PURPOSE,
  SIGN_UP_RETENTION,
} from '../privacyPolicy';
import classes from './SignUpSteps.module.css';
import type { SignUpForm } from './signUpForm';

/**
 * 가입 ④: 담당자 정보와 비밀번호. 부서·팀·직급·내선번호는 필수, 휴대폰번호는 선택이다(명세). 위에 앞 단계에서 고른 내용을 요약해
 * 신청 전에 한 번 더 보게 하고, 아래에 개인정보 수집·이용 안내와 확인·선택 동의를 둔다.
 */
export function ProfileStep({ form, email }: { form: SignUpForm; email: string }) {
  const { data: organizations } = useOrganizations();
  const organizationId = form.values.organizationId;
  // 결제원은 4자리 내선, 다른 기관은 결제원에서 바로 걸 수 있게 전체 번호를 받는다(서버가 검사한다).
  const kftc = organizations?.find((o) => String(o.id) === organizationId)?.type === 'KFTC';
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
          placeholder={kftc ? '예: 1234' : '예: 02-0000-0000'}
          description={kftc ? undefined : '지역번호부터 전체 번호를 적어 주세요.'}
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
      <PrivacyNotice form={form} />
    </Stack>
  );
}

/**
 * 개인정보 수집·이용 안내(개인정보 보호법 제15조 제2항의 네 가지: 목적, 항목, 보유 기간, 거부 권리와 불이익). 필수 항목은 계약
 * 이행(제15조 제1항 제4호)으로 처리하므로 "확인" 을 받고, 휴대폰번호는 선택 동의(제22조)로 따로 받는다. 선택 동의는 하지 않아도
 * 가입할 수 있다. 처리방침 전문은 새 탭으로 연다(이 화면을 떠나면 적은 내용이 사라진다).
 */
function PrivacyNotice({ form }: { form: SignUpForm }) {
  return (
    <Box className={classes.privacy} data-testid="sign-up-privacy">
      <Group justify="space-between" gap="xs" mb="xs">
        <Text fw={700}>개인정보 수집·이용 안내</Text>
        <Anchor component={Link} to="/privacy" target="_blank" rel="noopener" size="sm" fw={700}>
          <Group gap={4} wrap="nowrap">
            개인정보처리방침 전문
            <IconExternalLink size={14} stroke={1.8} />
          </Group>
        </Anchor>
      </Group>
      <Table withTableBorder className={tableClasses.stack} fz="sm" bg="var(--cl-card)">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>구분</Table.Th>
            <Table.Th>목적</Table.Th>
            <Table.Th>항목</Table.Th>
            <Table.Th>보유 기간</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          <Table.Tr>
            <Table.Td>필수 (계약 이행)</Table.Td>
            <Table.Td data-label="목적">{SIGN_UP_PURPOSE}</Table.Td>
            <Table.Td data-label="항목">{REQUIRED_ITEMS}</Table.Td>
            <Table.Td data-label="보유 기간">{SIGN_UP_RETENTION}</Table.Td>
          </Table.Tr>
          <Table.Tr>
            <Table.Td>선택 (동의)</Table.Td>
            <Table.Td data-label="목적">긴급한 업무 연락</Table.Td>
            <Table.Td data-label="항목">{OPTIONAL_ITEMS}</Table.Td>
            <Table.Td data-label="보유 기간">{SIGN_UP_RETENTION} 또는 동의 철회 시까지</Table.Td>
          </Table.Tr>
        </Table.Tbody>
      </Table>
      <Text size="xs" c="dimmed" mt="xs">
        필수 항목은 회원 계약 이행을 위해 처리합니다(개인정보 보호법 제15조 제1항 제4호). 휴대폰번호
        수집·이용에는 동의하지 않을 권리가 있으며, 동의하지 않아도 가입과 서비스 이용에 제한이
        없습니다. 다만 이 경우 휴대폰번호는 적을 수 없습니다.
      </Text>
      <Stack gap="xs" mt="sm">
        <Checkbox
          label="[필수] 개인정보 수집·이용 안내를 확인했습니다."
          {...form.getInputProps('privacyNoticeConfirmed', { type: 'checkbox' })}
        />
        <Checkbox
          label="[선택] 휴대폰번호 수집·이용에 동의합니다."
          {...form.getInputProps('mobilePhoneConsent', { type: 'checkbox' })}
        />
      </Stack>
    </Box>
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
