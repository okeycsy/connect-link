import { createBrowserRouter, Navigate } from 'react-router-dom';

import { LoginPage } from '../features/account/pages/LoginPage';
import { HomePage } from '../features/home/pages/HomePage';
import { AppShellLayout } from './layout/AppShellLayout';
import { BusinessGuard } from './layout/BusinessGuard';
import { PlaceholderPage } from './layout/PlaceholderPage';
import { DEFAULT_BUSINESS_SLUG, FEATURE_MENU } from './navigation';

/**
 * 주소 구조: /{업무 영역 slug}/{기능}.
 * 업무 영역(레일)은 조회 범위, 기능(보조 메뉴)은 화면을 정한다. docs/service-taxonomy.md 참고.
 */
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: <AppShellLayout />,
    children: [
      { index: true, element: <Navigate to={`/${DEFAULT_BUSINESS_SLUG}/home`} replace /> },
      {
        path: ':business',
        element: <BusinessGuard />,
        children: [
          { index: true, element: <Navigate to="home" replace /> },
          { path: 'home', element: <HomePage /> },
          ...FEATURE_MENU.filter((m) => m.key !== 'home').map((m) => ({
            path: m.path,
            element: <PlaceholderPage title={m.label} description={m.description} />,
          })),
          { path: '*', element: <Navigate to="home" replace /> },
        ],
      },
    ],
  },
]);
