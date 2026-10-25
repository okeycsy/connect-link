import { Box, Text } from '@mantine/core';
import {
  IconAddressBook,
  IconCalendarPause,
  IconClipboardCheck,
  IconMessageReply,
  IconMessages,
  IconNews,
} from '@tabler/icons-react';
import type { ComponentType } from 'react';

import classes from './LoginFeatures.module.css';

interface Feature {
  title: string;
  description: string;
  icon: ComponentType<{ size?: number; stroke?: number }>;
  /** 아이콘 원의 색(테마 색 이름). clay·sky·amber 를 번갈아 쓴다. */
  tone: 'clay' | 'sky' | 'amber';
}

/** 사이드바 기능 순서(연락처 → 의견 요청 → 기관 테스트 → 일시중단 → 소통 채널 → 공지·자료실)와 같다. */
const LOGIN_FEATURES: Feature[] = [
  {
    title: '담당자 연락처',
    description: '업무별 결제원·기관 담당자를 찾고, 우리 기관 연락처를 최신으로 지켜요.',
    icon: IconAddressBook,
    tone: 'clay',
  },
  {
    title: '의견 요청과 회신',
    description: '의견 요청을 받고 회신하는 일을 한곳에서 주고받아요.',
    icon: IconMessageReply,
    tone: 'sky',
  },
  {
    title: '기관 테스트',
    description: '요구사항마다 테스트 결과와 의견을 바로 주고받아요.',
    icon: IconClipboardCheck,
    tone: 'amber',
  },
  {
    title: '일시중단 공유',
    description: '서비스 일시중단 일정을 등록하고 미리 함께 확인해요.',
    icon: IconCalendarPause,
    tone: 'clay',
  },
  {
    title: '소통 채널',
    description: '업무별 대화방에서 결제원과 기관이 바로 묻고 답해요.',
    icon: IconMessages,
    tone: 'sky',
  },
  {
    title: '공지·자료실',
    description: '업무 공지와 규약·시행세칙을 한곳에서 찾아봐요.',
    icon: IconNews,
    tone: 'amber',
  },
];

/**
 * 로그인 화면의 기능 소개. 넓은 화면에서는 로고와 로그인 카드 사이에 호(弧)를 따라 여섯 줄로 놓이고,
 * 좁은 화면에서는 호 없이 목록으로 쌓인다. 호와 아이콘은 왼쪽 로고를 가리지 않게 작게 둔다(2026-10-25 SHY 요청). 장식(호와 점)은 화면 읽기에서 뺀다.
 */
export function LoginFeatures() {
  return (
    <Box component="section" aria-label="연결이 주요 기능" className={classes.root}>
      <svg className={classes.arc} viewBox="0 0 106 528" preserveAspectRatio="none" aria-hidden>
        <path d="M0 0 A 528 528 0 0 1 0 528" />
      </svg>
      <Box component="ol" className={classes.list}>
        {LOGIN_FEATURES.map((feature, index) => {
          const Icon = feature.icon;
          return (
            <Box component="li" key={feature.title} className={classes.item} data-index={index}>
              <Box className={classes.iconRing}>
                <Box className={classes.icon} data-tone={feature.tone}>
                  <Icon size={22} stroke={1.7} />
                </Box>
              </Box>
              <div>
                <Text className={classes.number} data-tone={feature.tone}>
                  {String(index + 1).padStart(2, '0')}
                </Text>
                <Text fw={700} fz={16} c="var(--cl-ink)">
                  {feature.title}
                </Text>
                <Text fz={13} c="var(--cl-ink-muted)" lh={1.55} maw={230}>
                  {feature.description}
                </Text>
              </div>
            </Box>
          );
        })}
      </Box>
    </Box>
  );
}
