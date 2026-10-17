import { Badge, Group, Skeleton, Stack, Table, Text } from '@mantine/core';

import { formatDate } from '../../../shared/format';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { OrgMonogram } from '../../../shared/OrgMonogram';
import type { BusinessCode, OrganizationDetail } from '../hooks';
import classes from './OrganizationTable.module.css';

interface Props {
  rows: OrganizationDetail[] | undefined;
  loading: boolean;
  selectedId: number | null;
  /** 현재 레일의 업무. 참여 업무 배지 중 이것만 채워서 보여 준다. */
  currentBusiness?: BusinessCode;
  onSelect: (organization: OrganizationDetail) => void;
}

/** 기관 기준정보 표. 행을 누르면 오른쪽 패널이 열리고 선택된 행은 남색 틴트로 남는다. */
export function OrganizationTable({ rows, loading, selectedId, currentBusiness, onSelect }: Props) {
  if (loading && !rows) {
    return (
      <Stack gap="xs">
        {[0, 1, 2, 3, 4].map((i) => (
          <Skeleton key={i} h={44} />
        ))}
      </Stack>
    );
  }
  return (
    <Table verticalSpacing="sm" highlightOnHover data-testid="organization-table">
      <Table.Thead>
        <Table.Tr>
          <Table.Th>기관</Table.Th>
          <Table.Th w={96}>구분</Table.Th>
          <Table.Th w={220}>참여 업무</Table.Th>
          <Table.Th w={190}>이메일 도메인</Table.Th>
          <Table.Th w={88}>상태</Table.Th>
          <Table.Th w={110}>최근 수정</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {rows?.map((o) => (
          <Table.Tr
            key={o.id}
            className={classes.row}
            data-selected={o.id === selectedId || undefined}
            data-inactive={!o.active || undefined}
            onClick={() => onSelect(o)}
            tabIndex={0}
            role="button"
            aria-label={o.name}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onSelect(o);
              }
            }}
          >
            <Table.Td>
              <Group gap="sm" wrap="nowrap">
                <OrgMonogram name={o.name} type={o.type} />
                <div>
                  <Text size="md" fw={600}>
                    {o.name}
                  </Text>
                  <Text size="xs" c="dimmed" ff="monospace">
                    {o.code}
                  </Text>
                </div>
              </Group>
            </Table.Td>
            <Table.Td>
              <Text size="sm">
                {o.typeLabel}
                {o.sectorLabel ? ` · ${o.sectorLabel}` : ''}
              </Text>
            </Table.Td>
            <Table.Td>
              <Group gap={4}>
                {o.businesses.map((b) => (
                  <Badge
                    key={b.code}
                    size="sm"
                    variant={b.code === currentBusiness ? 'filled' : 'light'}
                    color="clay"
                    title={b.label}
                  >
                    {b.shortLabel}
                  </Badge>
                ))}
                {o.businesses.length === 0 && (
                  <Text size="xs" c="dimmed">
                    없음
                  </Text>
                )}
              </Group>
            </Table.Td>
            <Table.Td>
              <Text size="sm" ff="monospace" c={o.emailDomain ? undefined : 'dimmed'}>
                {o.emailDomain ?? '-'}
              </Text>
            </Table.Td>
            <Table.Td>
              {o.active ? (
                <Badge size="sm" color="sky">
                  사용 중
                </Badge>
              ) : (
                <Badge size="sm" variant="outline" color="gray">
                  중지
                </Badge>
              )}
            </Table.Td>
            <Table.Td>
              <Text size="sm" c="dimmed">
                {formatDate(o.updatedAt)}
              </Text>
            </Table.Td>
          </Table.Tr>
        ))}
        {rows && rows.length === 0 && (
          <TableEmpty colSpan={6} message="조건에 맞는 기관이 없습니다." />
        )}
      </Table.Tbody>
    </Table>
  );
}
