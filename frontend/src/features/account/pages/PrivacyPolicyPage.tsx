import { Box, Card, Divider, List, NavLink, SimpleGrid, Stack, Table, Text, ThemeIcon } from '@mantine/core';
import {
  IconClockHour4,
  IconHeadset,
  IconListDetails,
  IconShareOff,
} from '@tabler/icons-react';
import type { ReactNode } from 'react';

import tableClasses from '../../../shared/list/ResponsiveTable.module.css';
import { PageHeader } from '../../../shared/PageHeader';
import { EntryLayout } from '../components/EntryLayout';
import {
  OPTIONAL_ITEMS,
  PRIVACY_ARTICLES,
  PRIVACY_POLICY_EFFECTIVE,
  type PrivacyBlock,
  SIGN_UP_RETENTION,
} from '../components/privacyPolicy';
import { useSupportInfo } from '../hooks';
import classes from './PrivacyPolicyPage.module.css';

/**
 * 개인정보 처리방침(로그인 없이, `/privacy`). 개인정보 보호법 제30조에 따라 누구나 볼 수 있게 공개한다. 위에 요약 네 칸(수집 항목,
 * 제3자 제공, 보유 기간, 고충처리부서), 아래 조문이다. 왼쪽 목차는 휴대폰 폭에서 숨긴다. 본문은 `privacyPolicy.ts`, 관리부서
 * 연락처는 서버 설정(`/api/v1/support-info`)이다.
 */
export function PrivacyPolicyPage() {
  const { data: support } = useSupportInfo();
  const department = support?.privacyDepartment ?? '연결이 운영 담당 부서';

  return (
    <EntryLayout>
      <Stack gap="md">
        <PageHeader
          title="개인정보 처리방침"
          description={`시행일 ${PRIVACY_POLICY_EFFECTIVE}`}
        />
        <Box className={classes.body} data-testid="privacy-policy">
          <Box className={classes.layout}>
            <Box component="nav" aria-label="개인정보 처리방침 목차" className={classes.nav}>
              {PRIVACY_ARTICLES.map((article) => (
                <NavLink
                  key={article.id}
                  href={`#${article.id}`}
                  label={article.title}
                  className={classes.navLink}
                  onClick={(event) => {
                    // 라우터 주소는 그대로 두고 그 조문으로만 옮긴다.
                    event.preventDefault();
                    document.getElementById(article.id)?.scrollIntoView({ behavior: 'smooth' });
                  }}
                />
              ))}
            </Box>

            <Stack gap="md" className={classes.content}>
              <Card>
                <Text size="lg" fw={700} mb="xs">
                  금융결제원 연결이는 「개인정보 보호법」을 준수하며, 개인정보 처리방침은 아래와 같습니다.
                </Text>
                <Text c="var(--cl-ink-soft)">
                  금융결제원은 정보주체의 자유와 권리 보호를 위해 「개인정보 보호법」 및 관계 법령이 정한 바를 준수하여
                  적법하게 개인정보를 처리하고 안전하게 관리하고 있습니다. 이에 「개인정보 보호법」 제30조에 따라
                  정보주체에게 개인정보의 처리와 보호에 관한 절차 및 기준을 안내하고, 이와 관련한 고충을 신속하고
                  원활하게 처리할 수 있도록 하기 위하여 다음과 같이 개인정보 처리방침을 수립·공개합니다.
                </Text>
              </Card>

              <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="sm">
                <SummaryTile icon={<IconListDetails size={22} stroke={1.7} />} title="개인정보 수집">
                  성명, 회사 이메일, 사무실 전화번호, 소속·부서·직급, 담당 업무, {OPTIONAL_ITEMS}(선택)
                </SummaryTile>
                <SummaryTile icon={<IconShareOff size={22} stroke={1.7} />} title="제3자 제공">
                  제공하지 않음
                </SummaryTile>
                <SummaryTile icon={<IconClockHour4 size={22} stroke={1.7} />} title="보유 기간">
                  회원 정보: {SIGN_UP_RETENTION}
                </SummaryTile>
                <SummaryTile icon={<IconHeadset size={22} stroke={1.7} />} title="고충처리부서">
                  {department}
                </SummaryTile>
              </SimpleGrid>

              <Card>
                <Stack gap="lg">
                  {PRIVACY_ARTICLES.map((article, index) => (
                    <Box key={article.id} id={article.id} className={classes.article}>
                      {index > 0 && <Divider mb="lg" color="var(--cl-line-soft)" />}
                      <Text component="h2" fw={700} size="xl" m={0} mb="sm">
                        {article.title}
                      </Text>
                      <Stack gap="sm">
                        {article.blocks.map((block, i) => (
                          <Block key={i} block={block} />
                        ))}
                        {article.id === 'officer' && (
                          <OfficerBox
                            department={department}
                            phone={support?.privacyPhone ?? undefined}
                            email={support?.inquiryEmail ?? undefined}
                          />
                        )}
                      </Stack>
                    </Box>
                  ))}
                </Stack>
              </Card>
            </Stack>
          </Box>
        </Box>
      </Stack>
    </EntryLayout>
  );
}

function SummaryTile({
  icon,
  title,
  children,
}: {
  icon: ReactNode;
  title: string;
  children: ReactNode;
}) {
  return (
    <Card className={classes.tile} padding="md">
      <ThemeIcon variant="light" color="clay" size={40} radius="md" mb="xs">
        {icon}
      </ThemeIcon>
      <Text fw={700} size="md">
        {title}
      </Text>
      <Text size="sm" c="dimmed" mt={4}>
        {children}
      </Text>
    </Card>
  );
}

function Block({ block }: { block: PrivacyBlock }) {
  switch (block.kind) {
    case 'p':
      return <Text c="var(--cl-ink-soft)">{block.text}</Text>;
    case 'note':
      return (
        <Text size="sm" c="dimmed">
          {block.text}
        </Text>
      );
    case 'list':
      return (
        <List
          type={block.ordered ? 'ordered' : 'unordered'}
          spacing={6}
          className={classes.list}
          c="var(--cl-ink-soft)"
        >
          {block.items.map((item) => (
            <List.Item key={item}>{item}</List.Item>
          ))}
        </List>
      );
    case 'table':
      return (
        <Table withTableBorder className={tableClasses.stack}>
          <Table.Thead>
            <Table.Tr>
              {block.head.map((h) => (
                <Table.Th key={h}>{h}</Table.Th>
              ))}
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {block.rows.map((row, r) => (
              <Table.Tr key={r}>
                {row.map((cell, c) => (
                  <Table.Td key={c} data-label={c === 0 ? undefined : block.head[c]}>
                    {cell}
                  </Table.Td>
                ))}
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      );
  }
}

/** 제15조의 보호책임자와 관리부서. 부서·전화·메일은 서버 설정 값이고 비어 있으면 그 줄을 뺀다. */
function OfficerBox({
  department,
  phone,
  email,
}: {
  department: string;
  phone?: string;
  email?: string;
}) {
  return (
    <Box className={classes.officer} data-testid="privacy-officer">
      <Row label="개인정보 보호책임자">정보보호최고책임자(CISO)</Row>
      <Row label="개인정보 관리부서">{department}</Row>
      {phone && <Row label="전화">{phone}</Row>}
      {email && <Row label="전자우편">{email}</Row>}
    </Box>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Box className={classes.officerRow}>
      <Text size="sm" c="dimmed" className={classes.officerLabel}>
        {label}
      </Text>
      <Text size="sm">{children}</Text>
    </Box>
  );
}
