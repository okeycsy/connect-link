import {
  IconClipboardCheck,
  IconHome,
  IconMessages,
  IconPlayerPause,
  IconSettings,
  IconUsers,
  type Icon,
} from '@tabler/icons-react';

/** 보조 메뉴(기능 축). 모든 업무 영역에서 같은 구성이다. */
export interface FeatureMenuItem {
  key: 'home' | 'contacts' | 'opinions' | 'tests' | 'suspensions' | 'admin';
  path: string;
  label: string;
  description: string;
  icon: Icon;
  /** 관리 그룹은 권한자에게만 보인다. 권한은 account 모듈이 붙은 뒤 서버 응답으로 정한다. */
  adminOnly?: boolean;
}

export const FEATURE_MENU: FeatureMenuItem[] = [
  { key: 'home', path: 'home', label: '홈', description: '지금 할 일', icon: IconHome },
  {
    key: 'contacts',
    path: 'contacts',
    label: '연락처',
    description: '담당자 조회·수정, 최근 확인일, 정기 변경',
    icon: IconUsers,
  },
  {
    key: 'opinions',
    path: 'opinions',
    label: '의견 요청',
    description: '의견 요청 작성과 기관 회신 취합',
    icon: IconMessages,
  },
  {
    key: 'tests',
    path: 'tests',
    label: '기관 테스트',
    description: '개발 건별 기관 테스트 수행과 결과',
    icon: IconClipboardCheck,
  },
  {
    key: 'suspensions',
    path: 'suspensions',
    label: '일시중단',
    description: '기관 일시중단 등록과 현황',
    icon: IconPlayerPause,
  },
  {
    key: 'admin',
    path: 'admin',
    label: '관리',
    description: '가입 승인, 정기 변경 주간, 알림 대상, 기준정보',
    icon: IconSettings,
    adminOnly: true,
  },
];

/** 로그인 직후 보여 줄 업무 영역. 사용자 소속 업무로 바꾸는 것은 account 모듈이 붙은 뒤다. */
export const DEFAULT_BUSINESS_SLUG = 'account-info';
