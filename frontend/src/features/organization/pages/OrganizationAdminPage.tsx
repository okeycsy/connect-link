import { Button, Card, Group, Pagination, Stack, Text, TextInput } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconPlus, IconSearch } from '@tabler/icons-react';
import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';

import { ChipFilter } from '../../../shared/list/ChipFilter';
import { ListPageHeader } from '../../../shared/list/ListPageHeader';
import { ErrorState } from '../../../shared/state/StateView';
import { OrganizationDrawer } from '../components/OrganizationDrawer';
import { OrganizationTable } from '../components/OrganizationTable';
import { useCurrentBusiness, useOrganizationSearch, type OrganizationType } from '../hooks';

const PAGE_SIZE = 20;

type Scope = 'current' | 'all';
type TypeFilter = 'ALL' | OrganizationType;
type ActiveFilter = 'active' | 'inactive' | 'all';

/**
 * 관리 > 기관·업무 기준정보. 공통 목록 패턴(필터 칩 + 표 + 오른쪽 상세 패널)의 첫 화면이다.
 * 기본은 현재 레일(업무 영역)에 참여하는 사용 중 기관만 보이고, 칩으로 전 업무·중지 기관까지 넓힌다.
 */
export function OrganizationAdminPage() {
  const { business: slug } = useParams<{ business: string }>();
  const current = useCurrentBusiness();

  const [scope, setScope] = useState<Scope>('current');
  const [type, setType] = useState<TypeFilter>('ALL');
  const [active, setActive] = useState<ActiveFilter>('active');
  const [keyword, setKeyword] = useState('');
  const [debouncedKeyword] = useDebouncedValue(keyword.trim(), 300);
  const [page, setPage] = useState(0);
  // 전역 검색 등에서 ?org={id} 로 들어오면 그 기관의 패널을 바로 연다.
  const [searchParams, setSearchParams] = useSearchParams();
  const [selectedId, setSelectedId] = useState<number | null>(() => {
    const org = Number(searchParams.get('org'));
    return Number.isInteger(org) && org > 0 ? org : null;
  });
  const [creating, setCreating] = useState(false);

  const { data, isPending, isError, error, isFetching } = useOrganizationSearch({
    keyword: debouncedKeyword || undefined,
    type: type === 'ALL' ? undefined : type,
    business: scope === 'current' ? slug : undefined,
    active: active === 'all' ? undefined : active === 'active',
    page,
    size: PAGE_SIZE,
  });

  const change = <T,>(setter: (v: T) => void) => {
    return (v: T) => {
      setter(v);
      setPage(0);
    };
  };

  return (
    <Stack gap="md">
      <ListPageHeader
        title="기관·업무 기준정보"
        description={
          current
            ? `${current.label}에 참여하는 기관을 기본으로 보여 줍니다. 참여 업무, 이메일 도메인, 사용 여부를 여기서 고칩니다.`
            : '기관과 참여 업무를 관리합니다.'
        }
        actions={
          <Button
            leftSection={<IconPlus size={16} />}
            onClick={() => {
              setSelectedId(null);
              setCreating(true);
            }}
          >
            기관 추가
          </Button>
        }
      />

      <Card>
        <Group justify="space-between" align="center" mb="sm" gap="sm">
          <Group gap="lg">
            <ChipFilter<Scope>
              label="업무"
              value={scope}
              onChange={change(setScope)}
              options={[
                { value: 'current', label: current?.shortLabel ?? '이 업무' },
                { value: 'all', label: '전체 업무' },
              ]}
            />
            <ChipFilter<TypeFilter>
              label="구분"
              value={type}
              onChange={change(setType)}
              options={[
                { value: 'ALL', label: '전체' },
                { value: 'KFTC', label: '결제원' },
                { value: 'FINANCE_ORGANIZATION', label: '참가기관' },
                { value: 'USE_ORGANIZATION', label: '이용기관' },
              ]}
            />
            <ChipFilter<ActiveFilter>
              label="상태"
              value={active}
              onChange={change(setActive)}
              options={[
                { value: 'active', label: '사용 중' },
                { value: 'inactive', label: '중지' },
                { value: 'all', label: '전체' },
              ]}
            />
          </Group>
          <Group gap="sm" wrap="nowrap">
            <TextInput
              size="xs"
              w={220}
              placeholder="기관명, 코드 검색"
              aria-label="기관 검색"
              leftSection={<IconSearch size={14} />}
              value={keyword}
              onChange={(e) => change(setKeyword)(e.currentTarget.value)}
            />
            {data && (
              <Text
                size="sm"
                c="dimmed"
                style={{ whiteSpace: 'nowrap' }}
                data-testid="organization-count"
              >
                {data.totalElements}곳
              </Text>
            )}
          </Group>
        </Group>

        {isError && <ErrorState error={error} homeLink={`/${slug}/home`} />}
        <div style={{ opacity: isFetching && data ? 0.6 : 1, transition: 'opacity 120ms' }}>
          <OrganizationTable
            rows={data?.content}
            loading={isPending}
            selectedId={selectedId}
            currentBusiness={current?.code}
            onSelect={(o) => {
              setCreating(false);
              setSelectedId(o.id);
            }}
          />
        </div>
        {data && data.totalPages > 1 && (
          <Group justify="center" mt="md">
            <Pagination
              total={data.totalPages}
              value={page + 1}
              onChange={(p) => setPage(p - 1)}
              size="sm"
            />
          </Group>
        )}
      </Card>

      <OrganizationDrawer
        organizationId={selectedId}
        creating={creating}
        defaultBusiness={current?.code}
        onClose={() => {
          setCreating(false);
          setSelectedId(null);
          if (searchParams.has('org')) setSearchParams({}, { replace: true });
        }}
        onSaved={(saved) => {
          setCreating(false);
          setSelectedId(saved.id);
        }}
      />
    </Stack>
  );
}
