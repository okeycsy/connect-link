import { Box, Card, List, SimpleGrid, Stack, Table, Text, ThemeIcon, Title } from '@mantine/core';
import {
  IconCheck,
  IconFingerprintOff,
  IconHeadset,
  IconShareOff,
  IconUserCircle,
} from '@tabler/icons-react';
import type { ReactNode } from 'react';

import tableClasses from '../../../shared/list/ResponsiveTable.module.css';
import { EntryLayout } from '../components/EntryLayout';
import {
  PRIVACY_ARTICLES,
  PRIVACY_POLICY_EFFECTIVE,
  type PrivacyBlock,
} from '../components/privacyPolicy';
import { useSupportInfo } from '../hooks';
import classes from './PrivacyPolicyPage.module.css';

/**
 * 개인정보 처리방침(로그인 없이, `/privacy`). 개인정보 보호법 제30조에 따라 누구나 볼 수 있게 공개한다. 모양은 금융결제원 처리방침
 * 화면(SHY 제공, 2026-11-14)을 따른다: 가운데 큰 제목, 머리말, 요약 네 칸(아이콘 + 체크 목록), 조문. 그래서 다른 화면과 달리
 * `PageHeader` 대신 가운데 제목을 쓴다. 본문은 `privacyPolicy.ts`, 관리부서 연락처는 서버 설정(`/api/v1/support-info`)이다.
 */
export function PrivacyPolicyPage() {
  const { data: support } = useSupportInfo();
  const department = support?.privacyDepartment ?? '연결이 운영 담당 부서';
  const phone = support?.privacyPhone ?? undefined;
  const email = support?.inquiryEmail ?? undefined;

  return (
    <EntryLayout>
      <Box className={classes.body} data-testid="privacy-policy">
        <Card className={classes.sheet}>
          <Stack gap={40}>
            <Stack gap={6} align="center">
              <Title order={1} className={classes.title}>
                개인정보처리방침
              </Title>
              <Text size="sm" c="dimmed">
                시행일 {PRIVACY_POLICY_EFFECTIVE}
              </Text>
            </Stack>

            <Stack gap="sm">
              <Text fw={700} fz={18}>
                금융결제원은 「개인정보 보호법」의 규정을 준수하며, 연결이의 개인정보처리방침은
                아래와 같습니다.
              </Text>
              <Text c="var(--cl-ink-soft)" lh={1.7}>
                금융결제원은 정보주체의 자유와 권리 보호를 위해 「개인정보 보호법」 및 관계 법령이
                정한 바를 준수하여 적법하게 개인정보를 처리하고 안전하게 관리하고 있습니다. 이에
                「개인정보 보호법」 제30조에 따라 정보주체에게 개인정보의 처리와 보호에 관한 절차 및
                기준을 안내하고, 이와 관련한 고충을 신속하고 원활하게 처리할 수 있도록 하기 위하여
                다음과 같이 개인정보처리방침을 수립·공개합니다.
              </Text>
            </Stack>

            <SimpleGrid cols={{ base: 1, xs: 2, md: 4 }} spacing="md">
              <SummaryBox
                title="개인정보 수집"
                icon={<IconUserCircle size={30} stroke={1.5} />}
                items={[
                  '성명',
                  '회사 이메일',
                  '사무실 전화번호',
                  '휴대전화번호(선택)',
                  '소속·부서·직급 등',
                ]}
              />
              <SummaryBox
                title="고유식별정보 수집"
                icon={<IconFingerprintOff size={30} stroke={1.5} />}
                items={['수집하지 않음']}
              />
              <SummaryBox
                title="제3자 제공"
                icon={<IconShareOff size={30} stroke={1.5} />}
                items={['제공하지 않음']}
              />
              <SummaryBox
                title="고충처리부서"
                icon={<IconHeadset size={30} stroke={1.5} />}
                items={[department]}
                lines={[phone && `Tel. ${phone}`, email && `Mail. ${email}`].filter(
                  (v): v is string => Boolean(v),
                )}
              />
            </SimpleGrid>

            <Stack gap={36}>
              {PRIVACY_ARTICLES.map((article) => (
                <Box key={article.id} id={article.id} component="section">
                  <Text component="h2" className={classes.articleTitle}>
                    {article.title}
                  </Text>
                  <Stack gap="sm" className={classes.articleBody}>
                    {article.blocks.map((block, i) => (
                      <Block key={i} block={block} />
                    ))}
                    {article.id === 'officer' && (
                      <OfficerBox department={department} phone={phone} email={email} />
                    )}
                  </Stack>
                </Box>
              ))}
            </Stack>
          </Stack>
        </Card>
      </Box>
    </EntryLayout>
  );
}

/** 요약 한 칸: 가운데 제목과 둥근 아이콘, 아래 체크 목록. */
function SummaryBox({
  title,
  icon,
  items,
  lines = [],
}: {
  title: string;
  icon: ReactNode;
  items: string[];
  /** 체크 없이 덧붙이는 줄(전화, 메일). */
  lines?: string[];
}) {
  return (
    <Box className={classes.summary}>
      <Text fw={700} size="lg" ta="center">
        {title}
      </Text>
      <ThemeIcon variant="light" color="clay" size={64} radius={32} className={classes.summaryIcon}>
        {icon}
      </ThemeIcon>
      <Stack gap={6} className={classes.summaryItems}>
        {items.map((item) => (
          <Box key={item} className={classes.check}>
            <IconCheck size={16} stroke={2.2} className={classes.checkIcon} />
            <Text size="md">{item}</Text>
          </Box>
        ))}
        {lines.map((line) => (
          <Text key={line} size="sm" c="var(--cl-ink-soft)" pl={24}>
            · {line}
          </Text>
        ))}
      </Stack>
    </Box>
  );
}

function Block({ block }: { block: PrivacyBlock }) {
  switch (block.kind) {
    case 'p':
      return (
        <Text c="var(--cl-ink-soft)" lh={1.7}>
          {block.text}
        </Text>
      );
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
          lh={1.7}
        >
          {block.items.map((item) => (
            <List.Item key={item}>{item}</List.Item>
          ))}
        </List>
      );
    case 'table':
      return (
        <Table withTableBorder withColumnBorders className={tableClasses.stack}>
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
