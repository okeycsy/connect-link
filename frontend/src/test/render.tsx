import { MantineProvider } from '@mantine/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderOptions } from '@testing-library/react';
import type { ReactElement, ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { theme } from '../app/theme';

interface Options extends Omit<RenderOptions, 'wrapper'> {
  /** 화면이 읽는 주소. 기본은 계좌통합관리 홈. */
  route?: string;
  /** 주소 패턴. useParams 가 읽을 값을 정한다. */
  path?: string;
}

/** 테스트용 렌더. 실제 앱과 같은 Provider(Mantine, Query, Router)를 감싼다. */
export function renderWithProviders(
  ui: ReactElement,
  { route = '/account-info/home', path = '/:business/home', ...options }: Options = {},
) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <MantineProvider theme={theme}>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={[route]}>
            <Routes>
              <Route path={path} element={children} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </MantineProvider>
    );
  }
  return render(ui, { wrapper: Wrapper, ...options });
}
