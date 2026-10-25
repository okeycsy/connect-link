import { fireEvent, screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';

import { renderWithProviders } from '../test/render';
import { type ContactTargetCandidate, ContactTargetPicker, type TargetValue } from './ContactTargetPicker';

const member = (contactId: number, name: string, subBusiness: string | null) => ({
  contactId,
  name,
  email: `${contactId}@example.com`,
  subBusiness,
  jobRoleLabel: '현업',
  department: '디지털부',
  team: '플랫폼팀',
  jobTitle: '대리',
});

const candidates: ContactTargetCandidate[] = [
  {
    organizationId: 1,
    organizationCode: '004',
    organizationName: 'KB국민은행',
    sector: 'BANK',
    sectorLabel: '은행',
    members: [member(11, '김민준', '내 계좌 한눈에'), member(12, '이도현', '오픈뱅킹 안심차단')],
  },
  {
    organizationId: 2,
    organizationCode: '240',
    organizationName: '삼성증권',
    sector: 'SECURITIES',
    sectorLabel: '증권사',
    members: [member(21, '최유진', '오픈뱅킹 안심차단')],
  },
];

function Harness({ initial = [] }: { initial?: TargetValue[] }) {
  const [value, setValue] = useState<TargetValue[]>(initial);
  return (
    <ContactTargetPicker
      candidates={candidates}
      loading={false}
      error={null}
      value={value}
      onChange={setValue}
      lockedOrganizationIds={new Set()}
      labels={{ person: '회신 담당자', locked: '회신 있음', noMembers: '담당자 연락처가 없습니다.' }}
    />
  );
}

const summary = () => screen.getByTestId('target-summary').textContent;

describe('ContactTargetPicker', () => {
  it('세부업무 칩은 그 세부업무 담당자만 기관을 넘어 고르고, 다시 누르면 뺀다', () => {
    renderWithProviders(<Harness />);
    fireEvent.click(screen.getByRole('checkbox', { name: '오픈뱅킹 안심차단' }));
    expect(screen.getByRole('checkbox', { name: 'KB국민은행 이도현' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: '삼성증권 최유진' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'KB국민은행 김민준' })).not.toBeChecked();
    expect(summary()).toBe('대상 2곳 · 회신 담당자 2명');

    fireEvent.click(screen.getByRole('checkbox', { name: '오픈뱅킹 안심차단' }));
    expect(summary()).toBe('대상 0곳 · 회신 담당자 0명');
  });

  it('업권 칩은 그 업권 기관을 담당자 전부와 함께 고른다', () => {
    renderWithProviders(<Harness />);
    fireEvent.click(screen.getByRole('checkbox', { name: '은행 1곳' }));
    expect(screen.getByRole('checkbox', { name: 'KB국민은행 대상' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: '삼성증권 대상' })).not.toBeChecked();
    expect(summary()).toBe('대상 1곳 · 회신 담당자 2명');

    // 업권 묶음 머리의 체크로도 그 업권 기관을 모두 고른다.
    fireEvent.click(screen.getByRole('checkbox', { name: '증권사 기관 모두' }));
    expect(screen.getByRole('checkbox', { name: '삼성증권 대상' })).toBeChecked();
    expect(summary()).toBe('대상 2곳 · 회신 담당자 3명');
  });

  it('업무 전체를 고른 뒤 한 명만 뺄 수 있다', () => {
    renderWithProviders(<Harness />);
    fireEvent.click(screen.getByRole('button', { name: '이 업무 담당자 모두 고르기' }));
    expect(summary()).toBe('대상 2곳 · 회신 담당자 3명');
    fireEvent.click(screen.getByRole('checkbox', { name: 'KB국민은행 김민준' }));
    expect(summary()).toBe('대상 2곳 · 회신 담당자 2명');
    expect(screen.getByRole('button', { name: '이 업무 담당자 모두 고르기' })).toBeInTheDocument();
  });
});
