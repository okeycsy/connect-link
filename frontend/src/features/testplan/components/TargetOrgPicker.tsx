import {
  Badge,
  Button,
  Checkbox,
  Chip,
  Group,
  Paper,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
} from '@mantine/core';
import { useMemo } from 'react';

import { OrgMonogram } from '../../../shared/OrgMonogram';
import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import type { TestPlanOrganization } from '../hooks';

interface Props {
  organizations: TestPlanOrganization[] | undefined;
  loading: boolean;
  error: unknown;
  value: number[];
  onChange: (value: number[]) => void;
  /** 결과를 등록해 대상에서 뺄 수 없는 기관. */
  lockedIds: Set<number>;
}

type Sector = NonNullable<TestPlanOrganization['sector']>;

/** 업권 묶음 순서. 서버 FinanceSector 선언 순서와 같다. 업권이 없는 기관은 맨 뒤 "업권 미지정"에 둔다. */
const SECTOR_ORDER: Sector[] = ['BANK', 'SECURITIES', 'MICROFINANCE', 'INSURANCE'];

interface SectorGroup {
  key: string;
  label: string;
  orgs: TestPlanOrganization[];
}

function groupBySector(orgs: TestPlanOrganization[]): SectorGroup[] {
  const groups = new Map<string, SectorGroup>();
  for (const org of orgs) {
    const key = org.sector ?? 'NONE';
    const group = groups.get(key) ?? { key, label: org.sectorLabel ?? '업권 미지정', orgs: [] };
    group.orgs.push(org);
    groups.set(key, group);
  }
  const rank = (key: string) => {
    const i = SECTOR_ORDER.indexOf(key as Sector);
    return i < 0 ? SECTOR_ORDER.length : i;
  };
  return [...groups.values()].sort((a, b) => rank(a.key) - rank(b.key));
}

/**
 * 기관 테스트 대상 기관 고르기. 후보는 이 업무에 참여하는 참가기관이다. 업무 전체나 업권으로 한 번에 고르고 한 곳씩 더하거나 뺀다. 결과를
 * 등록한 기관은 뺄 수 없다(서버도 막는다).
 */
export function TargetOrgPicker({
  organizations,
  loading,
  error,
  value,
  onChange,
  lockedIds,
}: Props) {
  const picked = useMemo(() => new Set(value), [value]);
  const groups = useMemo(() => groupBySector(organizations ?? []), [organizations]);

  if (error) return <StateView kind={stateKindOf(error)} compact />;
  if (loading || !organizations) {
    return (
      <Stack gap="xs">
        {[0, 1].map((i) => (
          <Skeleton key={i} h={56} />
        ))}
      </Stack>
    );
  }
  if (organizations.length === 0) {
    return (
      <StateView kind="empty" compact description="이 업무에 참여하는 참가기관이 아직 없습니다." />
    );
  }

  const allPicked = (orgs: TestPlanOrganization[]) => orgs.every((o) => picked.has(o.id));
  /** 기관들을 넣거나 뺀다(결과가 있는 기관은 남긴다). 고른 순서를 지킨다. */
  const setOrgs = (orgs: TestPlanOrganization[], checked: boolean) => {
    const ids = orgs.map((o) => o.id);
    if (checked) {
      onChange([...value, ...ids.filter((id) => !picked.has(id))]);
    } else {
      onChange(value.filter((id) => !ids.includes(id) || lockedIds.has(id)));
    }
  };
  const everything = allPicked(organizations);

  return (
    <Stack gap="sm">
      <Group gap="xs" justify="space-between">
        <Group gap="xs">
          <Button variant="light" onClick={() => setOrgs(organizations, !everything)}>
            {everything ? '모두 빼기' : '참여 기관 모두 고르기'}
          </Button>
          <Group gap="xs" role="group" aria-label="업권으로 고르기">
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
        </Group>
        <Text size="sm" c="dimmed" data-testid="target-summary">
          대상 {value.length}곳
        </Text>
      </Group>

      {groups.map((group) => (
        <Stack key={group.key} gap={6} data-testid={`sector-${group.key}`}>
          <Text size="sm" fw={700} mt={4}>
            {group.label}
          </Text>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="xs">
            {group.orgs.map((org) => {
              const checked = picked.has(org.id);
              const locked = lockedIds.has(org.id);
              return (
                <Paper
                  key={org.id}
                  p="sm"
                  radius="lg"
                  bg={checked ? 'clay.0' : 'gray.1'}
                  data-testid={`target-${org.code}`}
                >
                  <Checkbox
                    checked={checked}
                    disabled={locked}
                    onChange={(e) => setOrgs([org], e.currentTarget.checked)}
                    aria-label={`${org.name} 대상`}
                    label={
                      <Group gap="xs" wrap="nowrap">
                        <OrgMonogram
                          name={org.name}
                          type="FINANCE_ORGANIZATION"
                          size={28}
                          code={org.code}
                        />
                        <Text fw={600}>{org.name}</Text>
                        {locked && (
                          <Badge size="xs" color="sky">
                            결과 있음
                          </Badge>
                        )}
                      </Group>
                    }
                  />
                </Paper>
              );
            })}
          </SimpleGrid>
        </Stack>
      ))}
    </Stack>
  );
}
