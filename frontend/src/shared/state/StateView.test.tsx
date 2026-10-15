import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { ApiError } from '../../api/client';
import { renderWithProviders } from '../../test/render';
import { stateKindOf } from './stateKind';
import { ErrorState } from './StateView';

describe('StateView', () => {
  it('API 오류를 종류별 안내로 바꾼다', () => {
    expect(stateKindOf(new ApiError(404, { code: 'ORGANIZATION_NOT_FOUND' }))).toBe('notFound');
    expect(stateKindOf(new ApiError(403, { code: 'ACCESS_DENIED' }))).toBe('forbidden');
    expect(stateKindOf(new ApiError(403, { code: 'INTERNAL_ONLY' }))).toBe('internalOnly');
    expect(stateKindOf(new ApiError(500, {}))).toBe('error');
    expect(stateKindOf(new Error('x'))).toBe('error');
  });

  it('내부망 전용 오류는 서버 안내문을 그대로 보여 준다', () => {
    renderWithProviders(
      <ErrorState
        error={
          new ApiError(403, {
            code: 'INTERNAL_ONLY',
            detail: '엑셀 다운로드은(는) 내부망에서만 쓸 수 있습니다.',
          })
        }
        homeLink="/cms/home"
      />,
    );
    expect(screen.getByTestId('state-internalOnly')).toBeInTheDocument();
    expect(
      screen.getByText('엑셀 다운로드은(는) 내부망에서만 쓸 수 있습니다.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '홈으로' })).toHaveAttribute('href', '/cms/home');
  });
});
