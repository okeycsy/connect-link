import { Badge, Group, Skeleton, Stack, Table, Text } from '@mantine/core';

import { formatDate, formatDateTime } from '../../../../shared/format';
import tableClasses from '../../../../shared/list/ResponsiveTable.module.css';
import { TableEmpty } from '../../../../shared/list/TableEmpty';
import { OrgMonogram } from '../../../../shared/OrgMonogram';
import type { SignUpApplicationResponse, SignUpReviewStatus } from '../../hooks';
import classes from './SignUpApplicationTable.module.css';
import { SignUpStatusBadge } from './SignUpStatusBadge';

interface Props {
  rows: SignUpApplicationResponse[] | undefined;
  loading: boolean;
  status: SignUpReviewStatus;
  selectedId: number | null;
  /** 현재 레일의 업무 slug. 담당 업무 배지 중 이것만 채워서 보여 준다. */
  currentBusiness?: string;
  onSelect: (application: SignUpApplicationResponse) => void;
}

const EMPTY_MESSAGE: Record<SignUpReviewStatus, string> = {
  PENDING_APPROVAL: '기다리는 가입 신청이 없습니다.',
  ACTIVE: '승인한 가입 신청이 없습니다.',
  REJECTED: '반려한 가입 신청이 없습니다.',
};

/** 가입 신청 표. 행을 누르면 오른쪽 패널이 열린다. 처리한 신청(승인·반려)은 처리한 사람과 날짜를 함께 보인다. */
export function SignUpApplicationTable({
  rows,
  loading,
  status,
  selectedId,
  currentBusiness,
  onSelect,
}: Props) {
  if (loading && !rows) {
    return (
      <Stack gap="xs">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} h={44} />
        ))}
      </Stack>
    );
  }
  const reviewed = status !== 'PENDING_APPROVAL';
  return (
    <Table
      verticalSpacing="sm"
      highlightOnHover
      data-testid="sign-up-table"
      className={tableClasses.stack}
    >
      <Table.Thead>
        <Table.Tr>
          <Table.Th>신청자</Table.Th>
          <Table.Th w={220}>기관</Table.Th>
          <Table.Th w={150}>담당 업무</Table.Th>
          <Table.Th w={140}>직무</Table.Th>
          <Table.Th w={140}>신청 일시</Table.Th>
          {reviewed && <Table.Th w={150}>처리</Table.Th>}
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {rows?.map((a) => (
          <Table.Tr
            key={a.id}
            className={classes.row}
            data-selected={a.id === selectedId || undefined}
            onClick={() => onSelect(a)}
            tabIndex={0}
            role="button"
            aria-label={`${a.name} 가입 신청`}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onSelect(a);
              }
            }}
          >
            <Table.Td>
              <Text size="md" fw={600}>
                {a.name}
              </Text>
              <Text size="xs" c="dimmed" ff="monospace">
                {a.email}
              </Text>
            </Table.Td>
            <Table.Td data-label="기관">
              <Group gap="sm" wrap="nowrap">
                <OrgMonogram name={a.organizationName} type={a.organizationType} />
                <div>
                  <Text size="sm">{a.organizationName}</Text>
                  {!a.emailDomainMatches && (
                    <Badge size="xs" color="amber" mt={2}>
                      도메인 다름
                    </Badge>
                  )}
                </div>
              </Group>
            </Table.Td>
            <Table.Td data-label="담당 업무">
              <Group gap={4}>
                {a.businesses.map((b) => (
                  <Badge
                    key={b.code}
                    size="sm"
                    variant={b.slug === currentBusiness ? 'filled' : 'light'}
                    color="clay"
                    title={b.label}
                  >
                    {b.shortLabel}
                  </Badge>
                ))}
              </Group>
            </Table.Td>
            <Table.Td data-label="직무">
              <Text size="sm">{a.jobRoleLabel}</Text>
            </Table.Td>
            <Table.Td data-label="신청 일시">
              <Text size="sm" className={classes.numeric}>
                {formatDateTime(a.appliedAt)}
              </Text>
            </Table.Td>
            {reviewed && (
              <Table.Td data-label="처리">
                <Group gap={6} wrap="nowrap">
                  <SignUpStatusBadge application={a} size="xs" />
                  <Text size="sm" c="dimmed" className={classes.numeric}>
                    {a.reviewerName} · {formatDate(a.reviewedAt)}
                  </Text>
                </Group>
              </Table.Td>
            )}
          </Table.Tr>
        ))}
        {rows && rows.length === 0 && (
          <TableEmpty colSpan={reviewed ? 6 : 5} message={EMPTY_MESSAGE[status]} />
        )}
      </Table.Tbody>
    </Table>
  );
}
