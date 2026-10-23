import { Anchor, Box, Group, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

import { BrandMark } from '../../../shared/BrandMark';
import classes from './EntryLayout.module.css';

/**
 * 로그인 전 화면(회원가입, 승인 대기)의 틀. 앱 셸과 같은 높이 56 흰 상단 바(로고, 로그인 링크)와 page 바탕 본문이다.
 * 본문의 첫 요소는 `PageHeader` 이고, 내용은 `.content`(가운데 760px) 안에 둔다. 파스텔 번짐은 로그인 화면에만 쓰므로 여기엔 없다.
 */
export function EntryLayout({ children }: { children: ReactNode }) {
  return (
    <Box className={classes.page}>
      <Group component="header" className={classes.top} justify="space-between" wrap="nowrap">
        <Anchor component={Link} to="/login" c="inherit" underline="never" aria-label="연결이">
          <BrandMark />
        </Anchor>
        <Text size="sm" c="dimmed">
          이미 계정이 있나요?{' '}
          <Anchor component={Link} to="/login" size="sm" fw={600}>
            로그인
          </Anchor>
        </Text>
      </Group>
      <Box component="main" className={classes.main}>
        {children}
      </Box>
    </Box>
  );
}

/** 본문 가운데 열. 가입 단계 카드와 승인 대기 안내가 이 폭을 쓴다. */
export function EntryContent({ children }: { children: ReactNode }) {
  return <Box className={classes.content}>{children}</Box>;
}
