import { describe, expect, it } from 'vitest';

import {
  canUseAdminMenu,
  canViewBusiness,
  type CurrentUserResponse,
  homeBusinessSlugOf,
  safeNextPath,
} from './hooks';

const business = (code: string, slug: string) => ({ code, slug, label: code, shortLabel: code });

const member = {
  admin: false,
  organizationType: 'FINANCE_ORGANIZATION',
  businesses: [business('AUTO_TRANSFER', 'auto-transfer'), business('CMS', 'cms')],
} as unknown as CurrentUserResponse;
const admin = {
  admin: true,
  organizationType: 'KFTC',
  businesses: [],
} as unknown as CurrentUserResponse;
const kftcStaff = { ...member, organizationType: 'KFTC' } as CurrentUserResponse;

describe('계정 화면 규칙', () => {
  it('담당 업무만 볼 수 있고 관리자는 전 업무를 본다', () => {
    expect(canViewBusiness(member, 'cms')).toBe(true);
    expect(canViewBusiness(member, 'account-info')).toBe(false);
    expect(canViewBusiness(admin, 'account-info')).toBe(true);
  });

  it('로그인 뒤에는 담당 업무 중 첫 번째로, 관리자는 기본 업무로 간다', () => {
    expect(homeBusinessSlugOf(member)).toBe('auto-transfer');
    expect(homeBusinessSlugOf(admin)).toBe('account-info');
  });

  it('관리 메뉴는 관리자와 결제원 담당자만 쓰고 참가기관 담당자는 쓰지 못한다', () => {
    expect(canUseAdminMenu(admin)).toBe(true);
    expect(canUseAdminMenu(kftcStaff)).toBe(true);
    expect(canUseAdminMenu(member)).toBe(false);
  });

  it('돌아갈 주소는 이 앱 안의 경로만 받는다', () => {
    expect(safeNextPath('/cms/notifications?x=1')).toBe('/cms/notifications?x=1');
    expect(safeNextPath('https://evil.example.com')).toBeUndefined();
    expect(safeNextPath('//evil.example.com')).toBeUndefined();
    expect(safeNextPath('/\\evil.example.com')).toBeUndefined();
    expect(safeNextPath('/login')).toBeUndefined();
    expect(safeNextPath(null)).toBeUndefined();
  });
});
