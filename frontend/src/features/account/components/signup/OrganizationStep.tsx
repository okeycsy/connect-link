import { Box, Checkbox, Group, Select, Stack, Text, TextInput } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';

import {
  type BusinessCode,
  type OrganizationResponse,
  useBusinesses,
  useOrganizations,
} from '../../../organization/hooks';
import classes from './SignUpSteps.module.css';
import type { SignUpForm } from './signUpForm';

interface Props {
  form: SignUpForm;
  /** 회사 이메일 도메인이 같은 기관. 고른 기관이 여기 없으면 승인할 때 확인한다는 안내를 보인다. */
  suggestedOrganizationIds: number[];
}

/** 이용기관 담당자의 가입은 명세가 정하기 전까지 받지 않는다(서버도 막는다). */
function selectable(o: OrganizationResponse) {
  return o.type !== 'USE_ORGANIZATION';
}

/** 가입 ②: 소속 기관과 담당 업무(복수). 업무는 기관이 참여하는 것만 고를 수 있고, 업무마다 세부업무를 적을 수 있다. */
export function OrganizationStep({ form, suggestedOrganizationIds }: Props) {
  const { data: organizations } = useOrganizations();
  const { data: businesses } = useBusinesses();
  const candidates = (organizations ?? []).filter(selectable);
  const organization = candidates.find((o) => String(o.id) === form.values.organizationId);
  const domainMatches =
    organization !== undefined && suggestedOrganizationIds.includes(organization.id);

  // 구분(결제원, 참가기관)으로 묶는다. 목록은 서버가 구분·이름순으로 준다.
  const groups = [...new Set(candidates.map((o) => o.typeLabel))].map((typeLabel) => ({
    group: typeLabel,
    items: candidates
      .filter((o) => o.typeLabel === typeLabel)
      .map((o) => ({ value: String(o.id), label: o.name })),
  }));

  const chooseOrganization = (id: string | null) => {
    form.setFieldValue('organizationId', id);
    // 새 기관이 참여하지 않는 업무는 뺀다.
    const next = candidates.find((o) => String(o.id) === id);
    form.setFieldValue(
      'businesses',
      form.values.businesses.filter((b) => next?.businesses.includes(b.business)),
    );
  };

  const toggle = (code: BusinessCode, checked: boolean) => {
    const current = new Map(form.values.businesses.map((b) => [b.business, b.subBusiness]));
    if (checked) current.set(code, '');
    else current.delete(code);
    // 업무 순서를 지켜 서버 오류 경로(businesses.0…)와 화면 줄이 어긋나지 않게 한다.
    form.setFieldValue(
      'businesses',
      (businesses ?? [])
        .filter((b) => current.has(b.code))
        .map((b) => ({ business: b.code, subBusiness: current.get(b.code) ?? '' })),
    );
    form.clearFieldError('businesses');
  };

  return (
    <Stack gap="lg">
      <Box>
        <Select
          label="소속 기관"
          placeholder="기관 이름으로 찾기"
          withAsterisk
          searchable
          nothingFoundMessage="맞는 기관이 없습니다."
          data={groups}
          {...form.getInputProps('organizationId')}
          onChange={chooseOrganization}
          description={domainMatches ? '회사 이메일 도메인과 맞는 기관입니다.' : undefined}
        />
        {organization && !domainMatches && (
          <Group gap={6} mt={6} wrap="nowrap" data-testid="domain-mismatch">
            <IconAlertTriangle size={16} color="var(--mantine-color-amber-5)" />
            <Text size="sm" c="amber.7">
              회사 이메일 도메인과 기관이 다릅니다. 승인할 때 결제원 담당자가 확인합니다.
            </Text>
          </Group>
        )}
      </Box>

      <Stack gap={8}>
        <div>
          <Text size="sm" fw={500}>
            담당 업무{' '}
            <Text span c="red">
              *
            </Text>
          </Text>
          <Text size="xs" c="dimmed">
            여러 개 고를 수 있습니다. 세부업무는 자유롭게 적고, 비워 둬도 됩니다.
          </Text>
        </div>
        {(businesses ?? []).map((b) => {
          const participates = organization?.businesses.includes(b.code) ?? false;
          const index = form.values.businesses.findIndex((x) => x.business === b.code);
          const checked = index >= 0;
          return (
            <Box
              key={b.code}
              className={classes.option}
              data-checked={checked || undefined}
              data-disabled={!participates || undefined}
            >
              <Checkbox
                label={b.label}
                checked={checked}
                disabled={!participates}
                onChange={(e) => toggle(b.code, e.currentTarget.checked)}
                description={
                  organization && !participates
                    ? `${organization.name} 은(는) 참여하지 않는 업무입니다.`
                    : undefined
                }
              />
              {checked && (
                <TextInput
                  mt="xs"
                  ml={30}
                  size="sm"
                  aria-label={`${b.label} 세부업무`}
                  placeholder="세부업무 (선택)"
                  {...form.getInputProps(`businesses.${index}.subBusiness`)}
                />
              )}
            </Box>
          );
        })}
        {!organization && (
          <Text size="xs" c="dimmed">
            기관을 고르면 그 기관이 참여하는 업무를 고를 수 있습니다.
          </Text>
        )}
        {form.errors.businesses && (
          <Text size="sm" c="red" role="alert">
            {form.errors.businesses}
          </Text>
        )}
      </Stack>
    </Stack>
  );
}
