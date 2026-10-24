import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { UserGuidePage } from './UserGuidePage';

describe('UserGuidePage', () => {
  it('목차에서 기능 여섯 개의 가이드로 간다', () => {
    renderWithProviders(<UserGuidePage />, { route: '/guide', path: '/guide' });

    expect(screen.getByRole('link', { name: /회원가입 및 계정 생성 안내/ })).toHaveAttribute(
      'href',
      '/guide/sign-up',
    );
    expect(screen.getByRole('link', { name: /업무별 채팅 이용 안내/ })).toHaveAttribute(
      'href',
      '/guide/chat',
    );
  });

  it('기능 하나의 단계와 알아 두기, 다음 가이드 링크를 보인다', () => {
    renderWithProviders(<UserGuidePage />, { route: '/guide/contacts', path: '/guide/:topic' });

    expect(screen.getByRole('heading', { name: '담당자 정보 현행화 방법' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '정기 변경(현행화) 주간' })).toBeInTheDocument();
    expect(screen.getByText('알아 두기')).toBeInTheDocument();
    expect(
      screen.getAllByRole('link', { name: /의견 요청·회신 작성 가이드/ }).at(-1),
    ).toHaveAttribute('href', '/guide/opinions');
  });
});
