import { Anchor, Box, Button, Card, Group, List, Stack, Text, ThemeIcon } from '@mantine/core';
import {
  IconAddressBook,
  IconCalendarPause,
  IconClipboardCheck,
  IconInfoCircle,
  IconLogin2,
  IconMessageReply,
  IconMessages,
} from '@tabler/icons-react';
import type { ComponentType, ReactNode } from 'react';
import { Link } from 'react-router-dom';

import { PageHeader } from '../../../shared/PageHeader';
import { EntryContent, EntryLayout } from '../components/EntryLayout';
import classes from './UserGuidePage.module.css';

interface GuideSection {
  id: string;
  title: string;
  summary: string;
  icon: ComponentType<{ size?: number; stroke?: number }>;
  steps: ReactNode[];
}

/** 참가기관 담당자가 처음 쓸 때 따라가는 순서. 화면 이름과 버튼 이름은 앱의 문구와 같게 쓴다. */
const SECTIONS: GuideSection[] = [
  {
    id: 'sign-up',
    title: '가입하고 로그인하기',
    summary: '회사 이메일로 가입하고, 결제원 담당자의 승인을 받으면 로그인할 수 있어요.',
    icon: IconLogin2,
    steps: [
      '로그인 화면에서 "회원가입" 을 누르고 회사 이메일로 받은 숫자 6자리 인증 코드를 넣어요.',
      '소속 기관과 맡은 업무(여러 개 가능), 직무(현업·전산), 이름·부서·연락처·비밀번호를 적고 신청해요.',
      '고른 업무를 맡은 결제원 담당자가 확인하고 승인하면 회사 이메일로 알려 드려요. 승인 전에는 로그인할 수 없어요.',
      '승인되면 회사 이메일과 비밀번호로 로그인해요. 가입 때 적은 정보는 우리 기관 담당자 연락처에 자동으로 올라가요.',
    ],
  },
  {
    id: 'contacts',
    title: '담당자 연락처 확인하기',
    summary: '업무별 결제원 담당자와 우리 기관 담당자를 보고, 우리 기관 연락처를 최신으로 지켜요.',
    icon: IconAddressBook,
    steps: [
      '왼쪽 업무를 고르고 "연락처" 메뉴에서 결제원 담당자와 우리 기관 담당자를 봐요.',
      '우리 기관 담당자가 바뀌면 직접 고치거나 새 담당자(후임자)를 더해요.',
      '마지막 확인 뒤 6개월이 지나면 "확인 필요" 로 보이고 로그인할 때 확인 창이 떠요. 바뀐 게 없으면 확인만 눌러요.',
      '정기 변경 주간 메일을 받으면 우리 기관 정보를 살펴보고 "현행화 완료" 를 눌러요.',
    ],
  },
  {
    id: 'opinions',
    title: '의견 요청에 회신하기',
    summary: '결제원이 보낸 의견 요청에 기한 안에 답을 적어 제출해요.',
    icon: IconMessageReply,
    steps: [
      '의견 요청을 받으면 메일과 알림이 와요. "의견 요청" 메뉴에서 회신할 요청을 열어요.',
      '질문에 답을 적고 "임시저장" 해 두었다가, 다 쓰면 "제출" 해요. 필수 질문은 비워 둘 수 없어요.',
      '결제원이 수정을 요청하면 답을 고쳐 다시 제출해요. 마감 전까지 미회신이면 리마인드 메일이 가요.',
    ],
  },
  {
    id: 'tests',
    title: '기관 테스트 결과 등록하기',
    summary: '테스트 요구사항마다 결과를 올리고 결제원 의견을 확인해요.',
    icon: IconClipboardCheck,
    steps: [
      '테스트데이터 준비를 요청받으면 수행 시작 전에 생성 여부와 메모, 필요한 파일을 올려요.',
      '"기관 테스트" 메뉴에서 테스트를 열고 요구사항마다 결과(정상·오류·수행중·미수행)와 특이사항을 적어요.',
      '결제원 의견이 달리면 알림이 와요. 의견 아래 답글로 바로 묻고 답할 수 있어요.',
    ],
  },
  {
    id: 'suspensions',
    title: '일시중단 등록하기',
    summary: '우리 기관의 서비스 일시중단 일정을 미리 알려요.',
    icon: IconCalendarPause,
    steps: [
      '"일시중단" 메뉴에서 "일시중단 등록" 을 누르고 일시, 대상 업무, 중단 이유, 담당자 연락처를 적어요.',
      '공문이 있으면 함께 첨부해요. 저장하면 관련 업무 결제원 담당자에게 알림이 가요.',
      '결제원 담당자가 확인을 마치면 "확정" 으로 바뀌고, 결제원의 조치 사항을 상세에서 볼 수 있어요.',
    ],
  },
  {
    id: 'chat',
    title: '소통 채널에서 대화하기',
    summary: '업무별 대화방에서 결제원과 다른 기관 담당자와 바로 소통해요.',
    icon: IconMessages,
    steps: [
      '맡은 업무의 단체 대화방에 자동으로 들어가 있어요. 결제원이 따로 초대한 방도 함께 보여요.',
      '@기관이름 으로 특정 기관을 부를 수 있고, 받은 메시지에는 "확인" 을 눌러 읽었다고 알려요.',
      '파일을 첨부할 수 있어요. 첨부 파일은 1년 동안 보관돼요.',
    ],
  },
];

/**
 * 이용 가이드. 로그인 없이 보는 참가기관 담당자용 안내 한 장이다(로그인 화면 "가이드 보기").
 * 위에 목차, 아래에 기능마다 카드 한 장(요약 + 순서 목록)을 둔다.
 */
export function UserGuidePage() {
  return (
    <EntryLayout>
      <Stack gap="md">
        <PageHeader
          title="이용 가이드"
          description="참가기관 담당자가 처음 쓸 때 알아 두면 좋은 순서를 정리했어요."
          actions={
            <Button component={Link} to="/sign-up" variant="light">
              회원가입
            </Button>
          }
        />
        <EntryContent>
          <Stack gap="md" data-testid="user-guide">
            <Card>
              <Text fw={700} mb="xs">
                목차
              </Text>
              <Box component="ol" className={classes.toc}>
                {SECTIONS.map((section) => (
                  <li key={section.id}>
                    <Anchor href={`#${section.id}`} size="sm">
                      {section.title}
                    </Anchor>
                  </li>
                ))}
              </Box>
            </Card>

            {SECTIONS.map((section, index) => {
              const Icon = section.icon;
              return (
                <Card key={section.id} id={section.id} className={classes.section}>
                  <Group gap="md" wrap="nowrap" align="flex-start" mb="sm">
                    <ThemeIcon variant="light" color="clay" size={44} radius="md">
                      <Icon size={24} stroke={1.7} />
                    </ThemeIcon>
                    <div>
                      <Text size="sm" fw={700} c="var(--mantine-color-clay-6)">
                        {String(index + 1).padStart(2, '0')}
                      </Text>
                      <Text fw={700} size="lg">
                        {section.title}
                      </Text>
                      <Text size="sm" c="dimmed">
                        {section.summary}
                      </Text>
                    </div>
                  </Group>
                  <List type="ordered" spacing={6} size="sm" className={classes.steps}>
                    {section.steps.map((step, i) => (
                      <List.Item key={i}>{step}</List.Item>
                    ))}
                  </List>
                </Card>
              );
            })}

            <Card>
              <Group gap="md" wrap="nowrap" align="flex-start">
                <ThemeIcon variant="light" color="sky" size={44} radius="md">
                  <IconInfoCircle size={24} stroke={1.7} />
                </ThemeIcon>
                <div>
                  <Text fw={700} size="lg">
                    알아 두기
                  </Text>
                  <List spacing={6} size="sm" mt={6}>
                    <List.Item>
                      인터넷망에서는 연락처 검색·필터, 엑셀 내려받기처럼 일부 기능을 쓸 수 없어요.
                      내부망에서 접속해 주세요.
                    </List.Item>
                    <List.Item>
                      알림은 오른쪽 위 종 모양에서 모아 볼 수 있어요. 중요한 알림은 회사 메일로도
                      가요.
                    </List.Item>
                    <List.Item>
                      담당이 바뀌어 탈퇴할 때는 "내 정보" 에서 탈퇴해요. 우리 기관의 유일한
                      담당자라면 후임자를 먼저 연락처에 더해 주세요.
                    </List.Item>
                  </List>
                </div>
              </Group>
            </Card>
          </Stack>
        </EntryContent>
      </Stack>
    </EntryLayout>
  );
}
