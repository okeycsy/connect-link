import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../test/render';
import { ChatMessageItem } from './components/ChatMessageItem';
import { formatChatTime, splitMentions } from './components/chatFormat';
import {
  type ChatInviteCandidate,
  type ChatMessage,
  filterInviteCandidates,
  groupAcksByOrganization,
  groupInviteCandidates,
  type InviteFilter,
  mentionQueryAt,
} from './hooks';

const MESSAGE: ChatMessage = {
  id: 11,
  senderEmail: 'hong.sungmin@kftc.example.com',
  senderName: '홍성민',
  senderOrganizationCode: 'KFTC',
  senderOrganizationName: '금융결제원',
  senderKftc: true,
  mine: false,
  content: '@KB국민은행 @삼성증권 내일 10시에 전문 테스트를 시작합니다.',
  sentAt: '2026-10-21T09:10:00',
  unreadCount: 3,
  mentions: [
    { organizationCode: '004', label: 'KB국민은행' },
    { organizationCode: '240', label: '삼성증권' },
  ],
  mentionsMe: true,
  acks: [
    { organizationCode: '004', organizationName: 'KB국민은행', name: '김민준', mine: false },
    { organizationCode: '004', organizationName: 'KB국민은행', name: '이서준', mine: true },
  ],
  withAttachments: true,
  attachments: [
    {
      id: 5,
      ownerType: 'CHAT_MESSAGE',
      ownerId: 11,
      name: '전문 테스트 일정.xlsx',
      contentType: 'application/vnd.ms-excel',
      size: 2048,
      uploadedBy: '홍성민',
      uploadedAt: '2026-10-21T09:10:00',
    },
  ],
  attachmentsExpired: false,
  canAck: true,
};

const CANDIDATES: ChatInviteCandidate[] = [
  {
    email: 'ops@kftc.example.com',
    name: '이운영',
    organizationCode: 'KFTC',
    organizationName: '금융결제원',
    kftc: true,
    roleLabel: '결제원 · 전산(운영)',
    businesses: ['ACCOUNT_INFO', 'CMS'],
    jobRole: 'IT',
    itRoles: ['OPS'],
  },
  {
    email: 'cms@kftc.example.com',
    name: '박하늘',
    organizationCode: 'KFTC',
    organizationName: '금융결제원',
    kftc: true,
    roleLabel: '결제원 · 현업',
    businesses: ['CMS'],
    jobRole: 'BUSINESS',
    itRoles: [],
  },
  {
    email: 'dev@kb.example.com',
    name: '김개발',
    organizationCode: '004',
    organizationName: 'KB국민은행',
    kftc: false,
    roleLabel: '참가기관 · 전산(개발)',
    businesses: ['ACCOUNT_INFO'],
    jobRole: 'IT',
    itRoles: ['DEV'],
  },
];

const names = (list: ChatInviteCandidate[]) => list.map((c) => c.name);

describe('초대 대상 거르기', () => {
  it('업무·소속·직무·전산 세부 직무로 거른다', () => {
    const all: InviteFilter = { business: 'ALL', side: 'ALL', jobRole: 'ALL', itRoles: [] };
    expect(names(filterInviteCandidates(CANDIDATES, { ...all, business: 'ACCOUNT_INFO' }))).toEqual(
      ['이운영', '김개발'],
    );
    expect(names(filterInviteCandidates(CANDIDATES, { ...all, side: 'KFTC' }))).toEqual([
      '이운영',
      '박하늘',
    ]);
    expect(names(filterInviteCandidates(CANDIDATES, { ...all, jobRole: 'BUSINESS' }))).toEqual([
      '박하늘',
    ]);
    expect(
      names(filterInviteCandidates(CANDIDATES, { ...all, jobRole: 'IT', itRoles: ['DEV'] })),
    ).toEqual(['김개발']);
  });

  it('기관별로 묶는다', () => {
    const groups = groupInviteCandidates(CANDIDATES);
    expect(groups.map((g) => [g.label, g.items.length])).toEqual([
      ['결제원', 2],
      ['KB국민은행', 1],
    ]);
  });
});

describe('소통 채널 도우미', () => {
  it('입력 중인 @낱말만 멘션 검색어로 본다', () => {
    expect(mentionQueryAt('안녕하세요 @KB', 14)).toBe('KB');
    expect(mentionQueryAt('@', 1)).toBe('');
    expect(mentionQueryAt('메일 a@b', 7)).toBeNull();
    expect(mentionQueryAt('@KB국민은행 확인', 11)).toBeNull();
  });

  it('서버가 알려 준 멘션만 강조한다', () => {
    const parts = splitMentions('@KB국민은행 @없는기관 확인 부탁', ['KB국민은행']);
    expect(parts).toEqual([
      { text: '@KB국민은행', mention: true },
      { text: ' @없는기관 확인 부탁', mention: false },
    ]);
  });

  it('확인은 기관별로 묶고 내가 누른 것을 표시한다', () => {
    const groups = groupAcksByOrganization(MESSAGE.acks);
    expect(groups).toEqual([
      {
        organizationCode: '004',
        organizationName: 'KB국민은행',
        names: ['김민준', '이서준'],
        mine: true,
      },
    ]);
  });

  it('오늘이면 시각만, 아니면 날짜를 보인다', () => {
    const now = new Date('2026-10-21T15:00:00');
    expect(formatChatTime('2026-10-21T09:10:00', now)).toBe('09:10');
    expect(formatChatTime('2026-10-20T09:10:00', now)).toBe('2026-10-20');
  });
});

describe('ChatMessageItem', () => {
  it('멘션·첨부·읽지 않은 수·기관별 확인을 보이고 확인 취소를 부른다', () => {
    const onToggleAck = vi.fn();
    renderWithProviders(
      <ChatMessageItem
        message={MESSAGE}
        continued={false}
        onToggleAck={onToggleAck}
        ackPending={false}
      />,
    );

    expect(screen.getByText('@KB국민은행')).toHaveClass(/mention/);
    expect(screen.getByRole('link', { name: '전문 테스트 일정.xlsx' })).toHaveAttribute(
      'href',
      '/api/v1/attachments/5/download',
    );
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByTestId('chat-ack')).toHaveTextContent('KB국민은행 2');

    fireEvent.click(screen.getByRole('button', { name: '확인 취소' }));
    expect(onToggleAck).toHaveBeenCalledWith(MESSAGE, true);
  });

  it('보관 기간이 지난 첨부는 안내만 보인다', () => {
    renderWithProviders(
      <ChatMessageItem
        message={{ ...MESSAGE, attachments: [], attachmentsExpired: true, canAck: false }}
        continued={false}
        onToggleAck={vi.fn()}
        ackPending={false}
      />,
    );
    expect(screen.getByText('보관 기간이 지나 첨부 파일을 지웠습니다.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /확인/ })).toBeNull();
  });
});
