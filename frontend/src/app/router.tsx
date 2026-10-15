import { createBrowserRouter, Navigate } from 'react-router-dom';

import { LoginPage } from '../features/account/pages/LoginPage';
import { MyInfoPage } from '../features/account/pages/MyInfoPage';
import { HomePage } from '../features/home/pages/HomePage';
import { NotificationCenterPage } from '../features/notification/pages/NotificationCenterPage';
import { NotificationOutboxPage } from '../features/notification/pages/NotificationOutboxPage';
import { OrganizationAdminPage } from '../features/organization/pages/OrganizationAdminPage';
import { AdminHubPage } from './layout/AdminHubPage';
import { DemoStatesPage } from './layout/DemoStatesPage';
import { NotFoundPage } from './layout/NotFoundPage';
import { RouteErrorPage } from './layout/RouteErrorPage';
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
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <Navigate to={`/${DEFAULT_BUSINESS_SLUG}/home`} replace /> },
      {
        path: ':business',
        element: <BusinessGuard />,
        children: [
          { index: true, element: <Navigate to="home" replace /> },
          { path: 'home', element: <HomePage /> },
          // 전역 화면(알림 센터, 데모 도구)도 레일 문맥을 잃지 않도록 업무 영역 아래에 둔다.
          { path: 'notifications', element: <NotificationCenterPage /> },
          { path: 'me', element: <MyInfoPage /> },
          { path: 'demo/outbox', element: <NotificationOutboxPage /> },
          { path: 'demo/states', element: <DemoStatesPage /> },
          // 관리 그룹: 허브와 공통 몫인 기준정보. 나머지 항목은 담당 프로젝트가 admin/ 아래에 더한다.
          { path: 'admin', element: <AdminHubPage /> },
          { path: 'admin/organizations', element: <OrganizationAdminPage /> },
          ...FEATURE_MENU.filter((m) => m.key !== 'home' && m.key !== 'admin').map((m) => ({
            path: m.path,
            element: <PlaceholderPage title={m.label} description={m.description} />,
          })),
          // 업무 영역은 맞고 기능 주소만 틀리면 홈으로 보내지 않고 "주소 없음" 을 보여 준다.
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]);
