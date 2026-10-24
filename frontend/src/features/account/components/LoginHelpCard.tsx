import { Box, Button, Group, Stack, Text } from '@mantine/core';
import { IconArrowRight, IconBook2, IconHeadset, IconMail } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

import classes from './LoginHelpCard.module.css';

/**
 * 로그인 카드 아래의 도움말 카드: 왼쪽 이용 가이드(로그인 없이 보는 /guide), 오른쪽 문의하기(문의 메일).
 * 문의 메일 주소가 없으면 문의하기 칸을 빼고 가이드만 둔다.
 */
export function LoginHelpCard({ inquiryEmail }: { inquiryEmail?: string }) {
  return (
    <Box className={classes.card} data-testid="login-help">
      <HelpItem
        icon={<IconBook2 size={22} stroke={1.7} />}
        title="이용 가이드"
        description="가입부터 기능별 사용 방법까지 차례로 안내해요."
        action={
          <Button
            component={Link}
            to="/guide"
            variant="outline"
            size="sm"
            rightSection={<IconArrowRight size={16} />}
          >
            가이드 보기
          </Button>
        }
      />
      {inquiryEmail && (
        <>
          <Box className={classes.divider} aria-hidden />
          <HelpItem
            icon={<IconHeadset size={22} stroke={1.7} />}
            title="문의하기"
            description="이용 중 궁금한 점은 운영 담당자에게 메일로 문의해 주세요."
            action={
              <Button
                component="a"
                href={`mailto:${inquiryEmail}`}
                variant="outline"
                size="sm"
                rightSection={<IconMail size={16} />}
              >
                문의 메일 보내기
              </Button>
            }
          />
        </>
      )}
    </Box>
  );
}

function HelpItem(props: {
  icon: ReactNode;
  title: string;
  description: string;
  action: ReactNode;
}) {
  return (
    <Stack gap={8} className={classes.item}>
      <Group gap={8} c="var(--mantine-color-clay-7)" wrap="nowrap">
        {props.icon}
        <Text fw={700} c="var(--cl-ink)">
          {props.title}
        </Text>
      </Group>
      <Text size="sm" c="var(--cl-ink-muted)" lh={1.6} style={{ flex: 1 }}>
        {props.description}
      </Text>
      <div>{props.action}</div>
    </Stack>
  );
}
