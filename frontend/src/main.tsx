import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';
import './app/fonts.css';
import './app/global.css';

import { MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';

import { router } from './app/router';
import { accountKeys, isUnauthorized } from './features/account/hooks';
import { theme } from './app/theme';

const queryClient: QueryClient = new QueryClient({
  // 쓰는 도중 세션이 끝나 어떤 API 든 401 이 나면 로그인 사용자를 다시 읽는다. 그것도 401 이면 셸이 로그인 화면으로 보낸다.
  queryCache: new QueryCache({
    onError: (error, query) => {
      if (isUnauthorized(error) && query.queryKey[0] !== accountKeys.me[0]) {
        void queryClient.invalidateQueries({ queryKey: accountKeys.me });
      }
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => !isUnauthorized(error) && failureCount < 1,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MantineProvider theme={theme} defaultColorScheme="light">
      <Notifications position="top-right" />
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </MantineProvider>
  </StrictMode>,
);
