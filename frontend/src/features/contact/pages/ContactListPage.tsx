import {
  Badge,
  Button,
  Card,
  Chip,
  Group,
  Pagination,
  Select,
  Skeleton,
  Stack,
  Table,
  Text,
  TextInput,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { notifications } from '@mantine/notifications';
import {
  IconCalendarEvent,
  IconDownload,
  IconFileImport,
  IconPlus,
  IconSearch,
} from '@tabler/icons-react';
import { Fragment, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';

import { formatDate, formatPhone } from '../../../shared/format';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import tableClasses from '../../../shared/list/ResponsiveTable.module.css';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { OrgMonogram } from '../../../shared/OrgMonogram';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState } from '../../../shared/state/StateView';
import { useMe } from '../../account/hooks';
import { useCurrentBusiness } from '../../organization/hooks';
import { CampaignBanner } from '../components/CampaignBanner';
import { ContactNeedsConfirmBadge, ContactRankBadge } from '../components/ContactBadges';
import { ContactDrawer } from '../components/ContactDrawer';
import { ContactFormModal } from '../components/ContactFormModal';
import { ContactImportModal } from '../components/ContactImportModal';
import classes from '../components/ContactTable.module.css';
import {
  type Contact,
  type ContactListParams,
  type JobRole,
  canManageContacts,
  isContactOrganizationMember,
  useContactCounts,
  useContactOrganizations,
  useContacts,
  useDownloadContactExcel,
} from '../hooks';

const PAGE_SIZE = 50;
type JobRoleFilter = 'ALL' | JobRole;

/**
 * 담당자 연락처. 결제원 담당자와 결제원 운영·상황실·대외계는 이 업무의 전 기관을, 기관 담당자는 결제원과 자기 기관 담당자를 본다.
 * 기관별로 묶어 보이고, 행을 누르면 오른쪽 상세 패널(?id=)이 열린다. 검색·필터·엑셀은 내부망에서만 보인다.
 */
export function ContactListPage() {
  const { business: slug = '' } = useParams<{ business: string }>();
  const current = useCurrentBusiness();
  const { data: me } = useMe();
  const manager = canManageContacts(me, slug);
  const member = isContactOrganizationMember(me, slug);
  const internal = me?.networkZone === 'INTERNAL';
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedId = Number(searchParams.get('id')) || null;

  const [keyword, setKeyword] = useState('');
  const [debouncedKeyword] = useDebouncedValue(keyword.trim(), 300);
  const [organizationId, setOrganizationId] = useState<number | undefined>();
  const [jobRole, setJobRole] = useState<JobRoleFilter>('ALL');
  const [needsConfirm, setNeedsConfirm] = useState(false);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Contact | null>(null);
  const [creating, setCreating] = useState(false);
  const [importing, setImporting] = useState(false);

  // 인터넷망에서는 거르기 조건을 보내지 않는다(서버가 403 INTERNAL_ONLY 로 막는다).
  const params: ContactListParams = internal
    ? {
        business: slug,
        q: debouncedKeyword || undefined,
        organizationId,
        jobRole: jobRole === 'ALL' ? undefined : jobRole,
        needsConfirm: needsConfirm || undefined,
        page,
        size: PAGE_SIZE,
      }
    : { business: slug, page, size: PAGE_SIZE };
  const { data, isPending, isError, error, isFetching } = useContacts(params);
  const { data: counts } = useContactCounts(slug);
  const { data: organizations } = useContactOrganizations(slug);
  const excel = useDownloadContactExcel();

  const select = (id: number | null) => {
    const next = new URLSearchParams(searchParams);
    if (id) next.set('id', String(id));
    else next.delete('id');
    setSearchParams(next, { replace: true });
  };
  const resetPage = () => setPage(0);

  // 기관 담당자는 자기 기관에만 등록한다.
  const createOrganizations = (organizations ?? []).filter(
    (o) => manager || o.code === me?.organizationCode,
  );
  const canCreate = (manager && internal) || member;

  return (
    <Stack gap="md">
      <PageHeader
        title="연락처"
        description={
          member
            ? `${current?.label ?? '이 업무'}의 결제원 담당자와 우리 기관 담당자입니다. 바뀐 것이 있으면 고치고, 그대로면 "변동 없음"으로 확인합니다.`
            : `${current?.label ?? '이 업무'} 참가기관과 결제원 담당자입니다. 고치거나 확인하면 최근 확인일이 갱신됩니다.`
        }
        actions={
          <>
            {manager && (
              <Button
                component={Link}
                to={`/${slug}/contacts/update-campaigns`}
                variant="light"
                leftSection={<IconCalendarEvent size={16} stroke={1.8} />}
              >
                정기 변경 주간
              </Button>
            )}
            {internal && (
              <Button
                variant="light"
                leftSection={<IconDownload size={16} stroke={1.8} />}
                loading={excel.isPending}
                onClick={() =>
                  excel.mutate(params, {
                    onError: (e) => notifications.show({ message: e.message, color: 'red' }),
                  })
                }
              >
                엑셀
              </Button>
            )}
            {manager && internal && (
              <Button
                variant="light"
                leftSection={<IconFileImport size={16} stroke={1.8} />}
                onClick={() => setImporting(true)}
              >
                일괄 등록
              </Button>
            )}
            {canCreate && (
              <Button
                leftSection={<IconPlus size={18} stroke={1.8} />}
                onClick={() => setCreating(true)}
              >
                담당자 등록
              </Button>
            )}
          </>
        }
      />

      {current && (
        <CampaignBanner slug={slug} business={current.code} manager={manager} member={member} />
      )}

      <Card>
        <Group justify="space-between" align="center" mb="sm" gap="sm">
          {internal ? (
            <Group gap="md" align="center">
              <TextInput
                aria-label="담당자 검색"
                placeholder="이름, 기관, 부서, 세부업무, 이메일"
                leftSection={<IconSearch size={16} stroke={1.8} />}
                size="xs"
                w={240}
                value={keyword}
                onChange={(e) => {
                  setKeyword(e.currentTarget.value);
                  resetPage();
                }}
              />
              {(organizations?.length ?? 0) > 2 && (
                <Select
                  aria-label="기관"
                  placeholder="전체 기관"
                  size="xs"
                  w={170}
                  clearable
                  searchable
                  data={(organizations ?? []).map((o) => ({ value: String(o.id), label: o.name }))}
                  value={organizationId ? String(organizationId) : null}
                  onChange={(v) => {
                    setOrganizationId(v ? Number(v) : undefined);
                    resetPage();
                  }}
                />
              )}
              <ChipFilter<JobRoleFilter>
                label="직무"
                value={jobRole}
                onChange={(v) => {
                  setJobRole(v);
                  resetPage();
                }}
                options={[
                  { value: 'ALL', label: '전체' },
                  { value: 'BUSINESS', label: '현업' },
                  { value: 'IT', label: '전산' },
                ]}
              />
              <Chip
                size="xs"
                color="amber"
                checked={needsConfirm}
                onChange={(v) => {
                  setNeedsConfirm(v);
                  resetPage();
                }}
              >
                확인 필요만{counts ? ` ${counts.needsConfirm}` : ''}
              </Chip>
            </Group>
          ) : (
            <Text size="sm" c="dimmed" data-testid="contact-filter-internal-only">
              검색·필터와 엑셀은 내부망에서 씁니다.
            </Text>
          )}
          <Group gap="sm">
            {counts && counts.needsConfirm > 0 && (
              <Text size="sm" c="amber.7" fw={600} data-testid="contact-needs-confirm">
                확인 필요 {counts.needsConfirm}명
              </Text>
            )}
            {data && (
              <Text size="sm" c="dimmed" data-testid="contact-count">
                {data.totalElements}명
              </Text>
            )}
          </Group>
        </Group>

        {isError && <ErrorState error={error} homeLink={`/${slug}/home`} />}
        {!isError && (
          <div style={{ opacity: isFetching && data ? 0.6 : 1, transition: 'opacity 120ms' }}>
            <ContactTable
              rows={data?.content}
              loading={isPending}
              selectedId={selectedId}
              onSelect={(c) => select(c.id)}
            />
          </div>
        )}
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

      <ContactDrawer
        contactId={selectedId}
        onClose={() => select(null)}
        onEdit={setEditing}
        kftcExternal={manager && !internal}
      />
      {current && (
        <>
          <ContactFormModal
            opened={editing != null}
            onClose={() => setEditing(null)}
            business={current.code}
            contact={editing}
          />
          <ContactFormModal
            opened={creating}
            onClose={() => setCreating(false)}
            business={current.code}
            organizations={createOrganizations}
            onSaved={(saved) => select(saved.id)}
          />
          <ContactImportModal
            business={slug}
            businessLabel={current.label}
            opened={importing}
            onClose={() => setImporting(false)}
          />
        </>
      )}
    </Stack>
  );
}

function ContactTable({
  rows,
  loading,
  selectedId,
  onSelect,
}: {
  rows: Contact[] | undefined;
  loading: boolean;
  selectedId: number | null;
  onSelect: (row: Contact) => void;
}) {
  if (loading && !rows) {
    return (
      <Stack gap="xs">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} h={44} />
        ))}
      </Stack>
    );
  }
  // 서버가 결제원 → 기관 이름순 → 주/부담당 순으로 준다. 기관이 바뀔 때마다 묶음 머리를 넣는다.
  const groups: { organizationId: number; rows: Contact[] }[] = [];
  for (const row of rows ?? []) {
    const last = groups[groups.length - 1];
    if (last && last.organizationId === row.organizationId) last.rows.push(row);
    else groups.push({ organizationId: row.organizationId, rows: [row] });
  }

  return (
    <Table
      verticalSpacing="sm"
      highlightOnHover
      className={tableClasses.stack}
      data-testid="contact-table"
    >
      <Table.Thead>
        <Table.Tr>
          <Table.Th w={150}>이름</Table.Th>
          <Table.Th w={96}>직무</Table.Th>
          <Table.Th>부서 · 팀</Table.Th>
          <Table.Th>세부업무</Table.Th>
          <Table.Th w={124}>내선</Table.Th>
          <Table.Th w={124}>휴대폰</Table.Th>
          <Table.Th w={168}>최근 확인일</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {groups.map((group) => {
          const first = group.rows[0];
          return (
            <Fragment key={group.organizationId}>
              <Table.Tr className={classes.group} data-group data-testid="contact-group">
                <Table.Td colSpan={7}>
                  <Group gap="xs">
                    <OrgMonogram
                      name={first.organizationName}
                      type={first.organizationType}
                      code={first.organizationCode}
                      size={24}
                    />
                    <Text size="sm" fw={700}>
                      {first.organizationName}
                    </Text>
                    {first.organizationType !== 'KFTC' && (
                      <Text size="xs" c="dimmed" ff="monospace">
                        {first.organizationCode}
                      </Text>
                    )}
                    <Text size="xs" c="dimmed">
                      {group.rows.length}명
                    </Text>
                  </Group>
                </Table.Td>
              </Table.Tr>
              {group.rows.map((r) => (
                <Table.Tr
                  key={r.id}
                  className={`${classes.row} ${selectedId === r.id ? classes.selected : ''}`}
                  onClick={() => onSelect(r)}
                  tabIndex={0}
                  role="button"
                  aria-label={`${r.organizationName} ${r.name}`}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      onSelect(r);
                    }
                  }}
                >
                  <Table.Td>
                    <Group gap={6} wrap="nowrap">
                      <Text size="md" fw={600}>
                        {r.name}
                      </Text>
                      <ContactRankBadge rank={r.rank} label={r.rankLabel} size="xs" />
                      {r.self && (
                        <Badge color="clay" size="xs">
                          나
                        </Badge>
                      )}
                    </Group>
                    {r.jobTitle && (
                      <Text size="xs" c="dimmed">
                        {r.jobTitle}
                      </Text>
                    )}
                  </Table.Td>
                  <Table.Td data-label="직무">
                    <Text size="sm">{r.jobRoleLabel}</Text>
                  </Table.Td>
                  <Table.Td data-label="부서 · 팀">
                    <Text size="sm" lineClamp={2}>
                      {[r.department, r.team].filter(Boolean).join(' · ')}
                    </Text>
                  </Table.Td>
                  <Table.Td data-label="세부업무">
                    <Text size="sm" lineClamp={2} c={r.subBusiness ? undefined : 'dimmed'}>
                      {r.subBusiness || '-'}
                    </Text>
                  </Table.Td>
                  <Table.Td data-label="내선">
                    <Text size="sm" className={classes.numeric}>
                      {formatPhone(r.extensionPhone)}
                    </Text>
                  </Table.Td>
                  <Table.Td data-label="휴대폰">
                    <Text size="sm" className={classes.numeric}>
                      {formatPhone(r.mobilePhone) || '-'}
                    </Text>
                  </Table.Td>
                  <Table.Td data-label="최근 확인일">
                    <Group gap={6} wrap="nowrap">
                      <Text size="sm" className={classes.numeric}>
                        {formatDate(r.lastConfirmedAt)}
                      </Text>
                      {r.needsConfirm && <ContactNeedsConfirmBadge size="xs" />}
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Fragment>
          );
        })}
        {rows && rows.length === 0 && (
          <TableEmpty colSpan={7} message="조건에 맞는 담당자가 없습니다." />
        )}
      </Table.Tbody>
    </Table>
  );
}
