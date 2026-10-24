import {
  Box,
  Button,
  Card,
  Group,
  List,
  NavLink,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
} from '@mantine/core';
import { IconArrowLeft, IconArrowRight, IconBulb } from '@tabler/icons-react';
import { Link, Navigate, useParams } from 'react-router-dom';

import { PageHeader } from '../../../shared/PageHeader';
import { EntryLayout } from '../components/EntryLayout';
import { GUIDE_TOPICS, type GuideTopic, guideTopicOf } from '../components/guideTopics';
import classes from './UserGuidePage.module.css';

/**
 * 이용 가이드(참가기관 담당자용, 로그인 없이). `/guide` 는 기능 여섯 개의 목차 카드이고, `/guide/{기능}` 은 왼쪽 목차와
 * 기능 하나의 단계별 설명(소제목마다 순서 목록 + 알아 두기), 아래 이전·다음 이동이다. 휴대폰 폭에서는 왼쪽 목차를 숨긴다.
 */
export function UserGuidePage() {
  const { topic: topicId } = useParams();
  const topic = guideTopicOf(topicId);
  if (topicId && !topic) {
    return <Navigate to="/guide" replace />;
  }

  return (
    <EntryLayout>
      <Stack gap="md">
        <PageHeader
          title="이용 가이드"
          description="참가기관 담당자가 기능별로 알아 두면 좋은 사용 방법이에요."
          actions={
            <Button component={Link} to="/sign-up" variant="light">
              회원가입
            </Button>
          }
        />
        <Box className={classes.body} data-testid="user-guide">
          {topic ? <TopicView topic={topic} /> : <TopicIndex />}
        </Box>
      </Stack>
    </EntryLayout>
  );
}

function TopicIndex() {
  return (
    <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
      {GUIDE_TOPICS.map((topic, index) => {
        const Icon = topic.icon;
        return (
          <Card
            key={topic.id}
            component={Link}
            to={`/guide/${topic.id}`}
            className={classes.indexCard}
          >
            <Group justify="space-between" align="flex-start" wrap="nowrap" mb="sm">
              <ThemeIcon variant="light" color="clay" size={44} radius="md">
                <Icon size={24} stroke={1.7} />
              </ThemeIcon>
              <Text size="sm" fw={700} c="var(--mantine-color-clay-6)">
                {String(index + 1).padStart(2, '0')}
              </Text>
            </Group>
            <Text fw={700} size="lg">
              {topic.title}
            </Text>
            <Text size="sm" c="dimmed" mt={4}>
              {topic.summary}
            </Text>
          </Card>
        );
      })}
    </SimpleGrid>
  );
}

function TopicView({ topic }: { topic: GuideTopic }) {
  const index = GUIDE_TOPICS.indexOf(topic);
  const prev = GUIDE_TOPICS[index - 1];
  const next = GUIDE_TOPICS[index + 1];
  const Icon = topic.icon;

  return (
    <Box className={classes.layout}>
      <Box component="nav" aria-label="이용 가이드 목차" className={classes.nav}>
        <NavLink component={Link} to="/guide" label="전체 목차" className={classes.navLink} />
        {GUIDE_TOPICS.map((t) => (
          <NavLink
            key={t.id}
            component={Link}
            to={`/guide/${t.id}`}
            label={t.title}
            active={t.id === topic.id}
            className={classes.navLink}
          />
        ))}
      </Box>

      <Stack gap="md" className={classes.content}>
        <Card>
          <Group gap="md" wrap="nowrap" align="flex-start">
            <ThemeIcon variant="light" color="clay" size={48} radius="md">
              <Icon size={26} stroke={1.7} />
            </ThemeIcon>
            <div>
              <Text size="sm" fw={700} c="var(--mantine-color-clay-6)">
                {String(index + 1).padStart(2, '0')}
              </Text>
              <Text component="h2" fw={700} fz={22} m={0}>
                {topic.title}
              </Text>
              <Text c="dimmed" mt={4}>
                {topic.summary}
              </Text>
            </div>
          </Group>
        </Card>

        {topic.groups.map((group) => (
          <Card key={group.heading}>
            <Text component="h3" fw={700} size="lg" m={0} mb="sm">
              {group.heading}
            </Text>
            <List type="ordered" spacing={8} className={classes.steps}>
              {group.steps.map((step) => (
                <List.Item key={step}>{step}</List.Item>
              ))}
            </List>
          </Card>
        ))}

        {topic.tips.length > 0 && (
          <Card className={classes.tips}>
            <Group gap={8} mb="xs" c="var(--mantine-color-sky-7)">
              <IconBulb size={20} stroke={1.7} />
              <Text fw={700} c="var(--cl-ink)">
                알아 두기
              </Text>
            </Group>
            <List spacing={6} size="sm" className={classes.steps}>
              {topic.tips.map((tip) => (
                <List.Item key={tip}>{tip}</List.Item>
              ))}
            </List>
          </Card>
        )}

        <Group justify="space-between" gap="sm">
          {prev ? (
            <Button
              component={Link}
              to={`/guide/${prev.id}`}
              variant="outline"
              leftSection={<IconArrowLeft size={16} />}
            >
              {prev.title}
            </Button>
          ) : (
            <Button
              component={Link}
              to="/guide"
              variant="outline"
              leftSection={<IconArrowLeft size={16} />}
            >
              전체 목차
            </Button>
          )}
          {next && (
            <Button
              component={Link}
              to={`/guide/${next.id}`}
              variant="outline"
              rightSection={<IconArrowRight size={16} />}
            >
              {next.title}
            </Button>
          )}
        </Group>
      </Stack>
    </Box>
  );
}
