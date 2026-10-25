import { Badge, Button, Checkbox, Chip, Group, Paper, Skeleton, Stack, Text } from '@mantine/core';
import { useMemo } from 'react';

import { OrgMonogram } from './OrgMonogram';
import { StateView } from './state/StateView';
import { stateKindOf } from './state/stateKind';

/** 담당자로 고를 수 있는 사람 한 명(그 기관·업무의 담당자 연락처). */
export interface ContactTargetMember {
  contactId: number;
  name: string;
  email: string;
  subBusiness?: string | null;
  jobRoleLabel: string;
  department: string;
  team: string;
  jobTitle: string;
}

/** 대상으로 고를 수 있는 기관 한 곳과 그 기관의 같은 업무 담당자들. */
export interface ContactTargetCandidate {
  organizationId: number;
  organizationCode: string;
  organizationName: string;
  sector?: string | null;
  sectorLabel?: string | null;
  members: ContactTargetMember[];
}

/** 화면에서 고른 대상 기관 한 곳. */
export interface TargetValue {
  organizationId: number;
  /** 고른 담당자(담당자 연락처의 회사 이메일). */
  recipientEmails: string[];
}

/** 기능마다 다른 문구. */
export interface ContactTargetLabels {
  /** 고른 사람을 부르는 말(예: "회신 담당자", "테스트 담당자"). */
  person: string;
  /** 뺄 수 없는 기관에 붙는 표시(예: "회신 있음"). */
  locked: string;
  /** 담당자 연락처가 없는 기관 안내. */
  noMembers: string;
  /** 대상으로 넣었지만 아무도 고르지 않은 기관 안내. 없으면 보이지 않는다. */
  nobodyPicked?: string;
}

interface Props {
  candidates: ContactTargetCandidate[] | undefined;
  loading: boolean;
  error: unknown;
  value: TargetValue[];
  onChange: (value: TargetValue[]) => void;
  /** 기관이 이미 답(회신·결과)을 저장해 뺄 수 없는 기관. */
  lockedOrganizationIds: Set<number>;
  labels: ContactTargetLabels;
}

/** 업권 묶음 순서. 서버 FinanceSector 선언 순서와 같다. 참가기관은 서버가 업권을 반드시 두게 하므로 업권 없는 묶음은 생기지 않는다(혹시 있으면 맨 뒤에 둔다). */
const SECTOR_ORDER = ['BANK', 'SECURITIES', 'MICROFINANCE', 'INSURANCE'];

interface SectorGroup {
  key: string;
  label: string;
  orgs: ContactTargetCandidate[];
}

function groupBySector(candidates: ContactTargetCandidate[]): SectorGroup[] {
  const groups = new Map<string, SectorGroup>();
  for (const org of candidates) {
    const key = org.sector ?? 'NONE';
    const group = groups.get(key) ?? { key, label: org.sectorLabel ?? '업권 미지정', orgs: [] };
    group.orgs.push(org);
    groups.set(key, group);
  }
  const rank = (key: string) => {
    const i = SECTOR_ORDER.indexOf(key);
    return i < 0 ? SECTOR_ORDER.length : i;
  };
  return [...groups.values()].sort((a, b) => rank(a.key) - rank(b.key));
}

const subBusinessOf = (subBusiness: string | null | undefined) => (subBusiness ?? '').trim();

/**
 * 대상 기관과 그 기관 담당자 고르기(의견 요청의 회신 담당자, 기관 테스트의 테스트 담당자). 후보는 이 업무에 참여하는 참가기관과 그 기관의 같은 업무 담당자
 * 연락처다. 업무 전체, 세부업무, 업권으로 한 번에 고르고 한 명씩 더하거나 뺄 수 있다. 담당자를 고르면 그 기관도 대상이 된다.
 */
export function ContactTargetPicker({
  candidates,
  loading,
  error,
  value,
  onChange,
  lockedOrganizationIds,
  labels,
}: Props) {
  const byOrg = useMemo(() => new Map(value.map((t) => [t.organizationId, t])), [value]);
  const groups = useMemo(() => groupBySector(candidates ?? []), [candidates]);
  const subBusinesses = useMemo(() => {
    const names = new Set<string>();
    for (const org of candidates ?? []) {
      for (const m of org.members)
        if (subBusinessOf(m.subBusiness)) names.add(subBusinessOf(m.subBusiness));
    }
    return [...names].sort((a, b) => a.localeCompare(b, 'ko'));
  }, [candidates]);

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

  const empty = (organizationId: number): TargetValue => ({
    organizationId,
    recipientEmails: [],
  });
  const isPicked = (orgId: number, email: string) =>
    byOrg.get(orgId)?.recipientEmails.includes(email) ?? false;
  /** 기관 전부와 그 담당자 전부가 골라져 있는가. */
  const allPicked = (orgs: ContactTargetCandidate[]) =>
    orgs.every(
      (o) =>
        byOrg.has(o.organizationId) && o.members.every((m) => isPicked(o.organizationId, m.email)),
    );

  const setTarget = (organizationId: number, next: TargetValue | null) => {
    const rest = value.filter((t) => t.organizationId !== organizationId);
    onChange(next ? [...rest, next] : rest);
  };
  /** 기관들을 담당자 전부와 함께 넣거나, 뺀다(회신이 있는 기관은 남긴다). */
  const setOrgs = (orgs: ContactTargetCandidate[], checked: boolean) => {
    const next = new Map(byOrg);
    for (const org of orgs) {
      if (checked) {
        const current = next.get(org.organizationId) ?? empty(org.organizationId);
        next.set(org.organizationId, {
          ...current,
          recipientEmails: org.members.map((m) => m.email),
        });
      } else if (!lockedOrganizationIds.has(org.organizationId)) {
        next.delete(org.organizationId);
      }
    }
    onChange([...next.values()]);
  };
  /** 세부업무가 같은 담당자를 넣거나 뺀다. 빼서 담당자가 남지 않은 기관은 대상에서도 뺀다(회신이 있는 기관은 남긴다). */
  const setSubBusiness = (name: string, checked: boolean) => {
    const next = new Map(byOrg);
    for (const org of candidates) {
      const hits = org.members
        .filter((m) => subBusinessOf(m.subBusiness) === name)
        .map((m) => m.email);
      if (hits.length === 0) continue;
      const current = next.get(org.organizationId) ?? empty(org.organizationId);
      const ids = checked
        ? [...new Set([...current.recipientEmails, ...hits])]
        : current.recipientEmails.filter((id) => !hits.includes(id));
      if (ids.length === 0 && !lockedOrganizationIds.has(org.organizationId)) {
        next.delete(org.organizationId);
      } else {
        next.set(org.organizationId, { ...current, recipientEmails: ids });
      }
    }
    onChange([...next.values()]);
  };
  const subBusinessPicked = (name: string) =>
    candidates.every((org) =>
      org.members
        .filter((m) => subBusinessOf(m.subBusiness) === name)
        .every((m) => isPicked(org.organizationId, m.email)),
    );
  const toggleMember = (org: ContactTargetCandidate, email: string, checked: boolean) => {
    const current = byOrg.get(org.organizationId) ?? empty(org.organizationId);
    const ids = checked
      ? [...current.recipientEmails, email]
      : current.recipientEmails.filter((e) => e !== email);
    setTarget(org.organizationId, { ...current, recipientEmails: ids });
  };
  const everything = allPicked(candidates);

  return (
    <Stack gap="sm">
      <Group gap="xs" justify="space-between">
        <Button variant="light" onClick={() => setOrgs(candidates, !everything)}>
          {everything ? '모두 빼기' : '이 업무 담당자 모두 고르기'}
        </Button>
        <Text size="sm" c="dimmed" data-testid="target-summary">
          대상 {value.length}곳 · {labels.person}{' '}
          {value.reduce((n, t) => n + t.recipientEmails.length, 0)}명
        </Text>
      </Group>

      {subBusinesses.length > 0 && (
        <Group gap="xs" role="group" aria-label="세부업무로 고르기">
          <Text size="sm" fw={500} w={64}>
            세부업무
          </Text>
          {subBusinesses.map((name) => (
            <Chip
              key={name}
              size="sm"
              checked={subBusinessPicked(name)}
              onChange={(checked) => setSubBusiness(name, checked)}
            >
              {name}
            </Chip>
          ))}
        </Group>
      )}
      <Group gap="xs" role="group" aria-label="업권으로 고르기">
        <Text size="sm" fw={500} w={64}>
          업권
        </Text>
        {groups.map((g) => (
          <Chip
            key={g.key}
            size="sm"
            checked={allPicked(g.orgs)}
            onChange={(checked) => setOrgs(g.orgs, checked)}
          >
            {g.label} {g.orgs.length}곳
          </Chip>
        ))}
      </Group>

      {groups.map((group) => {
        const pickedInGroup = group.orgs.filter((o) => byOrg.has(o.organizationId)).length;
        return (
          <Stack key={group.key} gap={6} data-testid={`sector-${group.key}`}>
            <Group justify="space-between" wrap="nowrap" mt={4}>
              <Group gap="xs">
                <Checkbox
                  checked={pickedInGroup === group.orgs.length}
                  indeterminate={pickedInGroup > 0 && pickedInGroup < group.orgs.length}
                  onChange={(e) => setOrgs(group.orgs, e.currentTarget.checked)}
                  aria-label={`${group.label} 기관 모두`}
                  label={<Text fw={700}>{group.label}</Text>}
                />
                <Text size="sm" c="dimmed">
                  {pickedInGroup}/{group.orgs.length}곳
                </Text>
              </Group>
            </Group>
            {group.orgs.map((org) => {
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
                        onChange={(e) =>
                          setTarget(
                            org.organizationId,
                            e.currentTarget.checked ? empty(org.organizationId) : null,
                          )
                        }
                        label={
                          <Group gap="xs" wrap="nowrap">
                            <OrgMonogram
                              name={org.organizationName}
                              type="FINANCE_ORGANIZATION"
                              code={org.organizationCode}
                            />
                            <Text fw={600}>{org.organizationName}</Text>
                            {locked && (
                              <Badge size="xs" color="sky">
                                {labels.locked}
                              </Badge>
                            )}
                          </Group>
                        }
                        aria-label={`${org.organizationName} 대상`}
                      />
                    </Group>
                    {org.members.length === 0 ? (
                      <Text size="sm" c="dimmed" pl={30}>
                        {labels.noMembers}
                      </Text>
                    ) : (
                      <Stack gap={4} pl={30}>
                        {target && target.recipientEmails.length === 0 && labels.nobodyPicked && (
                          <Text size="xs" c="dimmed">
                            {labels.nobodyPicked}
                          </Text>
                        )}
                        {org.members.map((m) => (
                          <Checkbox
                            key={m.contactId}
                            size="xs"
                            checked={target?.recipientEmails.includes(m.email) ?? false}
                            onChange={(e) => toggleMember(org, m.email, e.currentTarget.checked)}
                            aria-label={`${org.organizationName} ${m.name}`}
                            label={
                              <Group gap={6} wrap="wrap">
                                <Text size="sm" fw={500}>
                                  {m.name}
                                </Text>
                                <Text size="xs" c="dimmed">
                                  {m.department} {m.team} · {m.jobTitle} · {m.jobRoleLabel}
                                </Text>
                                {m.subBusiness && (
                                  <Badge size="xs" color="gray" variant="outline">
                                    {m.subBusiness}
                                  </Badge>
                                )}
                              </Group>
                            }
                          />
                        ))}
                      </Stack>
                    )}
                  </Stack>
                </Paper>
              );
            })}
          </Stack>
        );
      })}
    </Stack>
  );
}
