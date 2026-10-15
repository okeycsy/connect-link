import { Button, Stack, Text, ThemeIcon, Title } from '@mantine/core';
import {
  IconAlertTriangle,
  IconInbox,
  IconLock,
  IconMapQuestion,
  IconShieldLock,
} from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

import { ApiError } from '../../api/client';
import { stateKindOf, type StateKind } from './stateKind';

export type { StateKind } from './stateKind';

const PRESET: Record<StateKind, { icon: typeof IconInbox; title: string; description: string }> = {
  empty: { icon: IconInbox, title: '아직 없습니다', description: '조건에 맞는 항목이 없습니다.' },
  error: {
    icon: IconAlertTriangle,
    title: '불러오지 못했습니다',
    description: '잠시 후 다시 시도해 주세요. 계속되면 관리자에게 알려 주세요.',
  },
  forbidden: {
    icon: IconLock,
    title: '볼 수 있는 범위가 아닙니다',
    description:
      '담당 업무와 기관 밖의 정보입니다. 권한이 필요하면 결제원 담당자에게 요청해 주세요.',
  },
  notFound: {
    icon: IconMapQuestion,
    title: '주소를 찾을 수 없습니다',
    description: '주소가 바뀌었거나 삭제된 항목입니다.',
  },
  internalOnly: {
    icon: IconShieldLock,
    title: '내부망에서만 쓸 수 있습니다',
    description:
      '수정·삭제, 검색·필터, 엑셀 다운로드는 결제원 내부망에서 열어 주세요. 조회는 인터넷망에서도 됩니다.',
  },
};

interface Props {
  kind: StateKind;
  /** 기본 문구 대신 쓸 제목·설명. 서버 오류의 detail 을 설명으로 넘길 수 있다. */
  title?: string;
  description?: ReactNode;
  /** 아래 버튼. 없으면 종류별 기본 동작(홈으로, 다시 시도 없음). */
  action?: ReactNode;
  /** 카드 안에 작게 둘 때. */
  compact?: boolean;
}

/** 빈 화면·오류·권한 없음·주소 없음·내부망 전용 안내. 목록과 상세, 전체 화면 어디서나 같은 모양으로 쓴다. */
export function StateView({ kind, title, description, action, compact = false }: Props) {
  const preset = PRESET[kind];
  const Icon = preset.icon;
  return (
    <Stack
      align="center"
      gap={compact ? 6 : 'sm'}
      py={compact ? 'lg' : 64}
      px="md"
      ta="center"
      data-testid={`state-${kind}`}
    >
      <ThemeIcon
        size={compact ? 40 : 56}
        radius="xl"
        variant="light"
        color={kind === 'error' ? 'red' : kind === 'internalOnly' ? 'amber' : 'clay'}
      >
        <Icon size={compact ? 20 : 28} stroke={1.6} />
      </ThemeIcon>
      <Title order={compact ? 5 : 3}>{title ?? preset.title}</Title>
      <Text c="dimmed" size={compact ? 'sm' : 'md'} maw={440}>
        {description ?? preset.description}
      </Text>
      {action}
    </Stack>
  );
}

/** API 오류 전용. 종류를 고르고 서버 detail 을 설명으로 쓴다. */
export function ErrorState({ error, homeLink }: { error: unknown; homeLink?: string }) {
  const kind = stateKindOf(error);
  const detail = error instanceof ApiError && kind !== 'error' ? error.message : undefined;
  return (
    <StateView
      kind={kind}
      description={detail}
      action={
        homeLink ? (
          <Button component={Link} to={homeLink} variant="light" mt="xs">
            홈으로
          </Button>
        ) : undefined
      }
    />
  );
}
