import { fireEvent, screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import type { OpinionRecipientCandidate } from '../hooks';
import { type TargetValue, TargetPicker } from './TargetPicker';

const member = (accountId: number, name: string, subBusiness: string | null) => ({
  accountId,
  name,
  email: `${accountId}@example.com`,
  subBusiness,
  jobRoleLabel: '현업',
  department: '디지털부',
  team: '플랫폼팀',
  jobTitle: '대리',
});

const candidates: OpinionRecipientCandidate[] = [
  {
    organizationId: 1,
    organizationCode: 'GANA',
    organizationName: '가나은행',
    sector: 'BANK',
    sectorLabel: '은행',
    members: [member(11, '김가나', '내 계좌 한눈에'), member(12, '이가나', '오픈뱅킹 안심차단')],
  },
  {
    organizationId: 2,
    organizationCode: 'SAA',
    organizationName: '사아증권',
    sector: 'SECURITIES',
    sectorLabel: '증권사',
    members: [member(21, '최사아', '오픈뱅킹 안심차단')],
  },
];

function Harness({ initial = [] }: { initial?: TargetValue[] }) {
  const [value, setValue] = useState<TargetValue[]>(initial);
  return (
    <TargetPicker
      candidates={candidates}
      loading={false}
      error={null}
      value={value}
      onChange={setValue}
      forms={[{ key: 'n0', name: '은행권' }]}
      lockedOrganizationIds={new Set()}
    />
  );
}

const summary = () => screen.getByTestId('target-summary').textContent;

describe('TargetPicker', () => {
  it('세부업무 칩은 그 세부업무 담당자만 기관을 넘어 고르고, 다시 누르면 뺀다', () => {
    renderWithProviders(<Harness />);
    fireEvent.click(screen.getByRole('checkbox', { name: '오픈뱅킹 안심차단' }));
    expect(screen.getByRole('checkbox', { name: '가나은행 이가나' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: '사아증권 최사아' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: '가나은행 김가나' })).not.toBeChecked();
    expect(summary()).toBe('대상 2곳 · 회신 담당자 2명');

    fireEvent.click(screen.getByRole('checkbox', { name: '오픈뱅킹 안심차단' }));
    expect(summary()).toBe('대상 0곳 · 회신 담당자 0명');
  });

  it('업권 칩은 그 업권 기관을 담당자 전부와 함께 고르고, 업권 양식을 한 번에 정한다', () => {
    renderWithProviders(<Harness />);
    fireEvent.click(screen.getByRole('checkbox', { name: '은행 1곳' }));
    expect(screen.getByRole('checkbox', { name: '가나은행 대상' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: '사아증권 대상' })).not.toBeChecked();
    expect(summary()).toBe('대상 1곳 · 회신 담당자 2명');
    expect(screen.getByRole('textbox', { name: '은행 업권 회신 양식' })).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: '증권사 업권 회신 양식' })).toBeNull();

    // 업권 묶음 머리의 체크로도 그 업권 기관을 모두 고른다.
    fireEvent.click(screen.getByRole('checkbox', { name: '증권사 기관 모두' }));
    expect(screen.getByRole('checkbox', { name: '사아증권 대상' })).toBeChecked();
    expect(summary()).toBe('대상 2곳 · 회신 담당자 3명');
  });

  it('업무 전체를 고른 뒤 한 명만 뺄 수 있다', () => {
    renderWithProviders(<Harness />);
    fireEvent.click(screen.getByRole('button', { name: '이 업무 담당자 모두 고르기' }));
    expect(summary()).toBe('대상 2곳 · 회신 담당자 3명');
    fireEvent.click(screen.getByRole('checkbox', { name: '가나은행 김가나' }));
    expect(summary()).toBe('대상 2곳 · 회신 담당자 2명');
    expect(screen.getByRole('button', { name: '이 업무 담당자 모두 고르기' })).toBeInTheDocument();
  });
});
