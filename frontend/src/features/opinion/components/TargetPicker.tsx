import {
  Badge,
  Button,
  Checkbox,
  Group,
  Paper,
  Select,
  Skeleton,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useMemo, useState } from 'react';

import { OrgMonogram } from '../../../shared/OrgMonogram';
import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import type { OpinionRecipientCandidate, OpinionRecipientCandidateMember } from '../hooks';

/** 화면에서 고른 대상 기관 한 곳. 양식은 화면 안 키로 잡고 저장할 때 이름으로 바꾼다. */
export interface TargetValue {
  organizationId: number;
  formKey: string | null;
  recipientAccountIds: number[];
}

interface Props {
  candidates: OpinionRecipientCandidate[] | undefined;
  loading: boolean;
  error: unknown;
  value: TargetValue[];
  onChange: (value: TargetValue[]) => void;
  /** 고를 수 있는 회신 양식(키, 이름). 비어 있으면 양식 칸을 숨긴다. */
  forms: { key: string; name: string }[];
  /** 이미 회신을 저장해 뺄 수 없는 기관. */
  lockedOrganizationIds: Set<number>;
}

function matches(member: OpinionRecipientCandidateMember, keyword: string): boolean {
  return (member.subBusiness ?? '').toLowerCase().includes(keyword.toLowerCase());
}

/**
 * 대상 기관과 회신 담당자 고르기. 후보는 이 업무에 참여하는 참가기관과 그 기관의 같은 업무 담당자(사용 중 계정)다. 세부업무로 담당자를
 * 찾고, 찾은 사람을 한 번에 고를 수 있다. 담당자를 고르면 그 기관도 대상이 된다.
 */
export function TargetPicker({
  candidates,
  loading,
  error,
  value,
  onChange,
  forms,
  lockedOrganizationIds,
}: Props) {
  const [keyword, setKeyword] = useState('');
  const byOrg = useMemo(() => new Map(value.map((t) => [t.organizationId, t])), [value]);
  const trimmed = keyword.trim();

  if (error) return <StateView kind={stateKindOf(error)} compact />;
  if (loading || !candidates) {
    return (
      <Stack gap="xs">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} h={56} />
        ))}
      </Stack>
    );
  }
  if (candidates.length === 0) {
    return (
      <StateView kind="empty" compact description="이 업무에 참여하는 참가기관이 아직 없습니다." />
    );
  }

  const setTarget = (organizationId: number, next: TargetValue | null) => {
    const rest = value.filter((t) => t.organizationId !== organizationId);
    onChange(next ? [...rest, next] : rest);
  };
  const toggleOrg = (org: OpinionRecipientCandidate, checked: boolean) =>
    setTarget(
      org.organizationId,
      checked
        ? { organizationId: org.organizationId, formKey: null, recipientAccountIds: [] }
        : null,
    );
  const toggleMember = (org: OpinionRecipientCandidate, accountId: number, checked: boolean) => {
    const current = byOrg.get(org.organizationId) ?? {
      organizationId: org.organizationId,
      formKey: null,
      recipientAccountIds: [],
    };
    const ids = checked
      ? [...current.recipientAccountIds, accountId]
      : current.recipientAccountIds.filter((id) => id !== accountId);
    setTarget(org.organizationId, { ...current, recipientAccountIds: ids });
  };
  /** 세부업무가 맞는 담당자를 모두 고른다. 이미 고른 사람은 그대로 둔다. */
  const pickMatching = () => {
    const next = new Map(byOrg);
    for (const org of candidates) {
      const hits = org.members.filter((m) => matches(m, trimmed)).map((m) => m.accountId);
      if (hits.length === 0) continue;
      const current = next.get(org.organizationId) ?? {
        organizationId: org.organizationId,
        formKey: null,
        recipientAccountIds: [],
      };
      next.set(org.organizationId, {
        ...current,
        recipientAccountIds: [...new Set([...current.recipientAccountIds, ...hits])],
      });
    }
    onChange([...next.values()]);
  };
  const matchCount = trimmed
    ? candidates.reduce((n, org) => n + org.members.filter((m) => matches(m, trimmed)).length, 0)
    : 0;

  return (
    <Stack gap="sm">
      <Group gap="xs" align="flex-end">
        <TextInput
          label="세부업무로 담당자 찾기"
          placeholder="예: 수수료, 전문, 해지"
          leftSection={<IconSearch size={16} stroke={1.8} />}
          value={keyword}
          onChange={(e) => setKeyword(e.currentTarget.value)}
          w={280}
        />
        <Button variant="light" disabled={matchCount === 0} onClick={pickMatching}>
          찾은 담당자 {matchCount}명 고르기
        </Button>
        <Text size="sm" c="dimmed" ml="auto" data-testid="target-summary">
          대상 {value.length}곳 · 회신 담당자{' '}
          {value.reduce((n, t) => n + t.recipientAccountIds.length, 0)}명
        </Text>
      </Group>

      {candidates.map((org) => {
        const target = byOrg.get(org.organizationId);
        const locked = lockedOrganizationIds.has(org.organizationId);
        return (
          <Paper
            key={org.organizationId}
            p="sm"
            radius="lg"
            bg={target ? 'clay.0' : 'gray.1'}
            data-testid={`target-${org.organizationCode}`}
          >
            <Stack gap={6}>
              <Group justify="space-between" wrap="nowrap">
                <Checkbox
                  checked={Boolean(target)}
                  disabled={locked}
                  onChange={(e) => toggleOrg(org, e.currentTarget.checked)}
                  label={
                    <Group gap="xs" wrap="nowrap">
                      <OrgMonogram name={org.organizationName} type="FINANCE_ORGANIZATION" />
                      <Text fw={600}>{org.organizationName}</Text>
                      {locked && (
                        <Badge size="xs" color="sky">
                          회신 있음
                        </Badge>
                      )}
                    </Group>
                  }
                  aria-label={`${org.organizationName} 대상`}
                />
                {target && forms.length > 0 && (
                  <Select
                    size="xs"
                    w={180}
                    placeholder="회신 양식 없음"
                    aria-label={`${org.organizationName} 회신 양식`}
                    data={forms.map((f) => ({ value: f.key, label: f.name || '(이름 없음)' }))}
                    value={target.formKey}
                    clearable
                    onChange={(v) => setTarget(org.organizationId, { ...target, formKey: v })}
                  />
                )}
              </Group>
              {org.members.length === 0 ? (
                <Text size="sm" c="dimmed" pl={30}>
                  이 업무를 맡은 담당자 계정이 없습니다. 대상으로 넣을 수는 있지만 메일은 가지
                  않습니다.
                </Text>
              ) : (
                <Stack gap={4} pl={30}>
                  {org.members.map((m) => {
                    const hit = trimmed !== '' && matches(m, trimmed);
                    return (
                      <Checkbox
                        key={m.accountId}
                        size="xs"
                        checked={target?.recipientAccountIds.includes(m.accountId) ?? false}
                        onChange={(e) => toggleMember(org, m.accountId, e.currentTarget.checked)}
                        aria-label={`${org.organizationName} ${m.name}`}
                        label={
                          <Group gap={6} wrap="wrap">
                            <Text size="sm" fw={hit ? 700 : 500}>
                              {m.name}
                            </Text>
                            <Text size="xs" c="dimmed">
                              {m.department} {m.team} · {m.jobTitle} · {m.jobRoleLabel}
                            </Text>
                            {m.subBusiness && (
                              <Badge
                                size="xs"
                                color={hit ? 'clay' : 'gray'}
                                variant={hit ? 'light' : 'outline'}
                              >
                                {m.subBusiness}
                              </Badge>
                            )}
                          </Group>
                        }
                      />
                    );
                  })}
                </Stack>
              )}
            </Stack>
          </Paper>
        );
      })}
    </Stack>
  );
}
