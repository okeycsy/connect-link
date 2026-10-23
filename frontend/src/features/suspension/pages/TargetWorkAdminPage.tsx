import { Badge, Button, Card, Group, Stack, Table, Text, TextInput } from '@mantine/core';
import { IconPlus, IconSearch } from '@tabler/icons-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';

import { DetailDrawer } from '../../../shared/list/DetailDrawer';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState } from '../../../shared/state/StateView';
import { TargetWorkForm } from '../components/TargetWorkForm';
import { type TargetWork, useTargetWorks } from '../hooks';

/**
 * 관리 > 대상 업무 기준정보. 일시중단 등록에서 고르는 대상 업무(레일과 별개)와 그 관련 업무(레일)를 관리자가 더하고 고친다. 지우지 않고
 * 사용을 끈다.
 */
export function TargetWorkAdminPage() {
  const { business: slug = '' } = useParams<{ business: string }>();
  const { data, isPending, isError, error } = useTargetWorks(true);
  const [keyword, setKeyword] = useState('');
  // null 은 닫힘, 'new' 는 더하기, 그 밖은 고칠 대상 업무다.
  const [editing, setEditing] = useState<TargetWork | 'new' | null>(null);

  const query = keyword.trim().toLowerCase();
  const rows = (data ?? []).filter(
    (w) =>
      !query ||
      w.name.toLowerCase().includes(query) ||
      (w.category ?? '').toLowerCase().includes(query),
  );
  const categories = [...new Set((data ?? []).map((w) => w.category).filter(Boolean))] as string[];
  const nextSortOrder = Math.max(0, ...(data ?? []).map((w) => w.sortOrder)) + 10;
  const work = editing === 'new' ? undefined : (editing ?? undefined);

  return (
    <Stack gap="md">
      <PageHeader
        title="대상 업무 기준정보"
        description="일시중단 등록에서 고르는 업무입니다. 관련 업무를 정하면 그 업무 영역에 보이고 그 업무 결제원 담당자가 확인합니다."
        actions={
          <Button leftSection={<IconPlus size={16} />} onClick={() => setEditing('new')}>
            대상 업무 추가
          </Button>
        }
      />
      <Card>
        <Group justify="space-between" mb="sm" gap="sm">
          <TextInput
            size="xs"
            w={260}
            placeholder="업무 이름, 묶음 검색"
            aria-label="대상 업무 검색"
            leftSection={<IconSearch size={14} />}
            value={keyword}
            onChange={(e) => setKeyword(e.currentTarget.value)}
          />
          {data && (
            <Text size="sm" c="dimmed" data-testid="target-work-count">
              {rows.length}개
            </Text>
          )}
        </Group>
        {isError && <ErrorState error={error} homeLink={`/${slug}/home`} />}
        <Table.ScrollContainer minWidth={560}>
          <Table highlightOnHover verticalSpacing="sm" data-testid="target-work-table">
            <Table.Thead>
              <Table.Tr>
                <Table.Th w={70}>순서</Table.Th>
                <Table.Th>업무 이름</Table.Th>
                <Table.Th>묶음</Table.Th>
                <Table.Th>관련 업무</Table.Th>
                <Table.Th w={90}>상태</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rows.map((w) => (
                <Table.Tr
                  key={w.id}
                  onClick={() => setEditing(w)}
                  style={{ cursor: 'pointer' }}
                  tabIndex={0}
                  role="button"
                  aria-label={w.name}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      setEditing(w);
                    }
                  }}
                >
                  <Table.Td>
                    <Text size="sm" c="dimmed">
                      {w.sortOrder}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="md" fw={600}>
                      {w.name}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm">{w.category}</Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm" c={w.businesses.length ? undefined : 'dimmed'}>
                      {w.businesses.length
                        ? w.businesses.map((b) => b.label).join(', ')
                        : '없음(운영·상황실·대외계)'}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    {w.active ? (
                      <Badge variant="light" size="sm">
                        사용 중
                      </Badge>
                    ) : (
                      <Badge variant="outline" color="gray" size="sm">
                        중지
                      </Badge>
                    )}
                  </Table.Td>
                </Table.Tr>
              ))}
              {!isPending && rows.length === 0 && (
                <TableEmpty
                  colSpan={5}
                  message={query ? '찾는 대상 업무가 없습니다.' : '대상 업무가 없습니다.'}
                />
              )}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Card>

      <DetailDrawer
        opened={editing != null}
        onClose={() => setEditing(null)}
        title={work ? work.name : '대상 업무 추가'}
        subtitle={work ? '대상 업무 고치기' : '일시중단 등록에서 고를 업무를 더합니다.'}
      >
        {editing != null && (
          <TargetWorkForm
            key={work?.id ?? 'new'}
            work={work}
            categories={categories}
            nextSortOrder={nextSortOrder}
            onSaved={() => setEditing(null)}
            onCancel={() => setEditing(null)}
          />
        )}
      </DetailDrawer>
    </Stack>
  );
}
