import { fireEvent, screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { type BusinessOption, SuspensionBusinessPicker } from './SuspensionBusinessPicker';

/** 업무가 많을 때(50개)를 흉내 낸다. */
const options: BusinessOption<string>[] = Array.from({ length: 50 }, (_, i) => ({
  value: `B${i + 1}`,
  label: i % 10 === 0 ? `자동이체 ${i + 1}` : `업무 ${i + 1}`,
}));

function Harness({ initial = [] as string[] }) {
  const [value, setValue] = useState<string[]>(initial);
  return <SuspensionBusinessPicker options={options} value={value} onChange={setValue} />;
}

describe('SuspensionBusinessPicker', () => {
  it('업무가 많으면 찾고, 찾은 업무를 한 번에 고르고, 고른 업무를 목록에서 뺀다', () => {
    renderWithProviders(<Harness initial={['B2']} />);
    expect(screen.getByTestId('suspension-businesses-count')).toHaveTextContent('고른 업무 1개');

    fireEvent.change(screen.getByLabelText('대상 업무 찾기'), { target: { value: '자동이체' } });
    expect(screen.getAllByRole('checkbox')).toHaveLength(5);
    fireEvent.click(screen.getByRole('button', { name: '찾은 업무 모두 고르기' }));
    expect(screen.getByTestId('suspension-businesses-count')).toHaveTextContent('고른 업무 6개');

    fireEvent.click(screen.getByRole('button', { name: '업무 2 빼기' }));
    expect(screen.getByTestId('suspension-businesses-count')).toHaveTextContent('고른 업무 5개');

    fireEvent.click(screen.getByRole('button', { name: '찾은 업무 모두 빼기' }));
    expect(screen.getByTestId('suspension-businesses-count')).toHaveTextContent('고른 업무 0개');
  });

  it('찾기를 지우면 모두 고르기는 전체 업무를 고른다', () => {
    renderWithProviders(<Harness />);
    fireEvent.click(screen.getByRole('button', { name: '모두 고르기' }));
    expect(screen.getByTestId('suspension-businesses-count')).toHaveTextContent('고른 업무 50개');
    expect(screen.getByRole('button', { name: '모두 빼기' })).toBeInTheDocument();
  });
});
