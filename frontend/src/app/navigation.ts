import {
  IconBell,
  IconBubbleText,
  IconClipboardCheck,
  IconHome,
  IconMessages,
  IconNews,
  IconPlayerPause,
  IconSettings,
  IconUsers,
  type Icon,
} from '@tabler/icons-react';

/** 사이드바의 묶음. 위에서부터 이 순서로 보인다(frontend/CLAUDE.md "사이드바"). */
export type SidebarGroup = '바로가기' | '업무 기능' | '관리';
export const SIDEBAR_GROUPS: SidebarGroup[] = ['바로가기', '업무 기능', '관리'];

/** 보조 메뉴(기능 축). 모든 업무 영역에서 같은 구성이다. */
export interface FeatureMenuItem {
  key: 'home' | 'contacts' | 'opinions' | 'tests' | 'suspensions' | 'chat' | 'notices' | 'admin';
  path: string;
  label: string;
  description: string;
  icon: Icon;
  group: SidebarGroup;
  /**
   * 관리 그룹은 관리 메뉴를 쓸 수 있는 사람(관리자, 결제원 담당자)에게만 보인다(features/account/hooks 의 canUseAdminMenu).
   * 주소로 들어와도 AdminGuard 가 막는다.
   */
  adminMenu?: boolean;
}

export const FEATURE_MENU: FeatureMenuItem[] = [
  {
    key: 'home',
    path: 'home',
    label: '홈',
    description: '지금 할 일',
    icon: IconHome,
    group: '바로가기',
  },
  {
    key: 'contacts',
    path: 'contacts',
    label: '연락처',
    description: '담당자 조회·수정, 최근 확인일, 정기 변경',
    icon: IconUsers,
    group: '업무 기능',
  },
  {
    key: 'opinions',
    path: 'opinions',
    label: '의견 요청',
    description: '의견 요청 작성과 기관 회신 취합',
    icon: IconMessages,
    group: '업무 기능',
  },
  {
    key: 'tests',
    path: 'tests',
    label: '기관 테스트',
    description: '개발 건별 기관 테스트 수행과 결과',
    icon: IconClipboardCheck,
    group: '업무 기능',
  },
  {
    key: 'suspensions',
    path: 'suspensions',
    label: '일시중단',
    description: '기관 일시중단 등록과 현황',
    icon: IconPlayerPause,
    group: '업무 기능',
  },
  {
    key: 'chat',
    path: 'chat',
    label: '소통 채널',
    description: '결제원·참가기관 업무 대화방, 기관 멘션과 확인',
    icon: IconBubbleText,
    group: '업무 기능',
  },
  {
    key: 'notices',
    path: 'notices',
    label: '공지·자료실',
    description: '업무 공지와 규약·시행세칙, 전 업무 공통 공지',
    icon: IconNews,
    group: '업무 기능',
  },
  {
    key: 'admin',
    path: 'admin',
    label: '관리',
    description: '가입 승인, 알림 대상, 기준정보',
    icon: IconSettings,
    group: '관리',
    adminMenu: true,
  },
];

/** 기능은 아니지만 사이드바 바로가기에 두는 전역 화면. 알림 센터는 읽지 않은 수를 배지로 보인다. */
export interface ShortcutMenuItem {
  key: 'notifications';
  path: string;
  label: string;
  icon: Icon;
}

export const SHORTCUT_MENU: ShortcutMenuItem[] = [
  { key: 'notifications', path: 'notifications', label: '알림 센터', icon: IconBell },
];

/** 관리자의 기본 업무 영역. 그 밖의 사용자는 담당 업무 중 첫 번째로 들어간다(features/account/hooks 의 homeBusinessSlugOf). */
export const DEFAULT_BUSINESS_SLUG = 'account-info';

/** 관리 그룹의 항목. 메뉴 자리는 공통이 정하고, 화면은 담당 프로젝트가 만든다(가입 승인과 기준정보는 공통). */
export interface AdminMenuItem {
  key: 'organizations' | 'target-works' | 'signup-approval' | 'notification-targets';
  label: string;
  description: string;
  /** 업무 영역 아래 상대 경로. 없으면 아직 화면이 없는 자리다. */
  path?: string;
  /** 화면을 만드는 프로젝트. */
  owner: string;
  /** 관리자에게만 보인다. 없으면 관리 메뉴를 쓰는 결제원 담당자에게도 보인다(가입 승인은 맡은 업무만 처리한다). */
  adminOnly?: boolean;
}

export const ADMIN_MENU: AdminMenuItem[] = [
  {
    key: 'organizations',
    label: '기관·업무 기준정보',
    description: '기관 등록, 참여 업무, 이메일 도메인, 사용 여부',
    path: 'admin/organizations',
    owner: '공통',
    adminOnly: true,
  },
  {
    key: 'target-works',
    label: '대상 업무 기준정보',
    description: '일시중단에서 고르는 대상 업무, 묶음, 관련 업무, 사용 여부',
    path: 'admin/target-works',
    owner: '라. 일시중단',
    adminOnly: true,
  },
  {
    key: 'signup-approval',
    label: '가입 승인',
    description: '가입 신청 목록, 이메일 도메인과 기관 일치 여부, 승인·반려',
    path: 'admin/sign-ups',
    owner: '공통',
  },
  {
    key: 'notification-targets',
    label: '알림 대상 설정',
    description: '메일·매터모스트 알림을 받을 사람과 채널',
    owner: '공통',
  },
];
