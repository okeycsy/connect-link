import {
  Anchor,
  Badge,
  Box,
  Card,
  Group,
  Select,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
} from '@mantine/core';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { useContactOrganizations } from '../../contact/hooks';
import { formatPhone } from '../../../shared/format';
import { OrgMonogram } from '../../../shared/OrgMonogram';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import {
  type OrganizationOverviewItem,
  type OrganizationOverviewSection,
  useOrganizationOverview,
} from '../hooks';

/** 칸이 비었을 때 문구. */
const EMPTY_TEXT: Record<OrganizationOverviewSection['key'], string> = {
  CONTACT: '등록된 담당자가 없습니다.',
  OPINION_REQUEST: '회신을 기다리는 의견 요청이 없습니다.',
  TEST_PLAN: '진행 중인 기관 테스트가 없습니다.',
  SUSPENSION: '진행 중이거나 예정된 일시중단이 없습니다.',
};

/**
 * 기관 한눈에 보기(결제원 담당자). 참가기관 하나를 고르면 그 업무의 담당자, 회신을 기다리는 의견 요청, 진행 중 기관 테스트, 일시중단을 한 화면에서 본다.
 * 무엇을 채울지는 서버가 칸마다 정한다.
 */
export function OrganizationOverviewPage() {
  const { business = '', id } = useParams();
  const navigate = useNavigate();
  const organizationId = id ? Number(id) : null;
  const organizations = useContactOrganizations(business);
  const overview = useOrganizationOverview(business, organizationId);

  const options = (organizations.data ?? [])
    .filter((o) => o.type === 'FINANCE_ORGANIZATION')
    .map((o) => ({ value: String(o.id), label: `${o.name} (${o.code})` }));

  return (
    <Stack gap="lg">
      <PageHeader
        title="기관 한눈에 보기"
        description="참가기관 하나의 담당자, 의견 회신, 기관 테스트, 일시중단을 한 화면에서 봅니다."
        actions={
          <Select
            aria-label="기관 고르기"
            placeholder="기관 고르기"
            searchable
            data={options}
            value={organizationId ? String(organizationId) : null}
            onChange={(v) => v && navigate(`/${business}/organizations/${v}`)}
            w={240}
          />
        }
      />

      {organizationId == null ? (
        <StateView
          kind="empty"
          title="기관을 골라 주세요"
          description="오른쪽 위에서 기관을 고르거나 전역 검색에서 기관 이름을 누르면 여기에 모입니다."
        />
      ) : overview.isError ? (
        <ErrorState error={overview.error} homeLink={`/${business}/home`} />
      ) : overview.isPending ? (
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} h={200} radius="md" />
          ))}
        </SimpleGrid>
      ) : (
        <Stack gap="md">
          <Group gap="sm">
            <OrgMonogram
              name={overview.data.organizationName}
              type="FINANCE_ORGANIZATION"
              code={overview.data.organizationCode}
              size={40}
            />
            <div>
              <Text fw={700} size="xl">
                {overview.data.organizationName}
              </Text>
              <Text size="sm" c="dimmed">
                {overview.data.organizationCode}
                {overview.data.sectorLabel && ` · ${overview.data.sectorLabel}`}
              </Text>
            </div>
          </Group>
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md" data-testid="org-overview">
            {overview.data.sections.map((s) => (
              <SectionCard key={s.key} section={s} />
            ))}
          </SimpleGrid>
        </Stack>
      )}
    </Stack>
  );
}

function SectionCard({ section }: { section: OrganizationOverviewSection }) {
  return (
    <Card data-testid={`org-overview-${section.key}`}>
      <Group justify="space-between" mb="sm" wrap="nowrap">
        <Group gap="xs">
          <Text fw={700} size="lg">
            {section.title}
          </Text>
          <Badge variant="light" color="gray">
            {section.items.length}
          </Badge>
        </Group>
        <Anchor component={Link} to={section.link} size="sm">
          모두 보기
        </Anchor>
      </Group>
      {section.items.length === 0 ? (
        <Text size="sm" c="dimmed">
          {EMPTY_TEXT[section.key]}
        </Text>
      ) : (
        <Stack gap={10}>
          {section.items.map((item, i) => (
            <ItemRow key={i} item={item} />
          ))}
        </Stack>
      )}
    </Card>
  );
}

function ItemRow({ item }: { item: OrganizationOverviewItem }) {
  const phones = [
    item.extensionPhone && `내선 ${item.extensionPhone}`,
    item.mobilePhone && formatPhone(item.mobilePhone),
    item.email,
  ].filter(Boolean);
  return (
    <Box>
      <Group justify="space-between" gap="xs" wrap="nowrap">
        <Anchor component={Link} to={item.link} fw={600} size="sm" c="inherit" lineClamp={1}>
          {item.title}
        </Anchor>
        {item.badge && (
          <Badge
            size="sm"
            variant={item.attention ? 'filled' : 'light'}
            color={item.attention ? 'red' : 'gray'}
            style={{ flexShrink: 0 }}
          >
            {item.badge}
          </Badge>
        )}
      </Group>
      {item.subtitle && (
        <Text size="xs" c="dimmed" lineClamp={1}>
          {item.subtitle}
        </Text>
      )}
      {phones.length > 0 && (
        <Text size="xs" c="dimmed">
          {phones.join(' · ')}
        </Text>
      )}
    </Box>
  );
}
