import { createBrowserRouter, Navigate } from 'react-router-dom';

import { LoginPage } from '../features/account/pages/LoginPage';
import { MyInfoPage } from '../features/account/pages/MyInfoPage';
import { SignUpApprovalPage } from '../features/account/pages/SignUpApprovalPage';
import { SignUpPage } from '../features/account/pages/SignUpPage';
import { SignUpPendingPage } from '../features/account/pages/SignUpPendingPage';
import { ContactListPage } from '../features/contact/pages/ContactListPage';
import { UpdateCampaignPage } from '../features/contact/pages/UpdateCampaignPage';
import { HomePage } from '../features/home/pages/HomePage';
import { NotificationCenterPage } from '../features/notification/pages/NotificationCenterPage';
import { NotificationOutboxPage } from '../features/notification/pages/NotificationOutboxPage';
import { OpinionDetailPage } from '../features/opinion/pages/OpinionDetailPage';
import { OpinionEditorPage } from '../features/opinion/pages/OpinionEditorPage';
import { OpinionListPage } from '../features/opinion/pages/OpinionListPage';
import { SuspensionDetailPage } from '../features/suspension/pages/SuspensionDetailPage';
import { SuspensionEditorPage } from '../features/suspension/pages/SuspensionEditorPage';
import { SuspensionListPage } from '../features/suspension/pages/SuspensionListPage';
import { TestPlanDetailPage } from '../features/testplan/pages/TestPlanDetailPage';
import { TestPlanEditorPage } from '../features/testplan/pages/TestPlanEditorPage';
import { TestPlanListPage } from '../features/testplan/pages/TestPlanListPage';
import { TestPlanRecipientsPage } from '../features/testplan/pages/TestPlanRecipientsPage';
import { OrganizationAdminPage } from '../features/organization/pages/OrganizationAdminPage';
import { AdminGuard } from './layout/AdminGuard';
import { AdminHubPage } from './layout/AdminHubPage';
import { DemoAttachmentsPage } from './layout/DemoAttachmentsPage';
import { DemoStatesPage } from './layout/DemoStatesPage';
import { NotFoundPage } from './layout/NotFoundPage';
import { RouteErrorPage } from './layout/RouteErrorPage';
import { AppShellLayout } from './layout/AppShellLayout';
import { BusinessGuard } from './layout/BusinessGuard';
import { PlaceholderPage } from './layout/PlaceholderPage';
import { DEFAULT_BUSINESS_SLUG, FEATURE_MENU, type FeatureMenuItem } from './navigation';

/** 화면이 있는 기능. 나머지 기능은 "준비 중" 자리를 보인다. */
const BUILT_FEATURES: FeatureMenuItem['key'][] = [
  'home',
  'admin',
  'contacts',
  'opinions',
  'tests',
  'suspensions',
];

/**
 * 주소 구조: /{업무 영역 slug}/{기능}.
 * 업무 영역(레일)은 조회 범위, 기능(보조 메뉴)은 화면을 정한다. docs/service-taxonomy.md 참고.
 */
export const router = createBrowserRouter([
  // 로그인 전 화면: 로그인, 회원가입(4단계), 승인 대기
  { path: '/login', element: <LoginPage /> },
  { path: '/sign-up', element: <SignUpPage /> },
  { path: '/sign-up/pending', element: <SignUpPendingPage /> },
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
          { path: 'demo/attachments', element: <DemoAttachmentsPage /> },
          // 관리 그룹: 허브와 공통 몫인 가입 승인(결제원 담당자와 관리자), 기준정보(관리자만).
          // 나머지 항목은 담당 프로젝트가 admin/ 아래에 더한다.
          {
            path: 'admin',
            element: <AdminGuard />,
            children: [
              { index: true, element: <AdminHubPage /> },
              { path: 'sign-ups', element: <SignUpApprovalPage /> },
              // 가. 정기 변경 주간(그 업무의 결제원 담당자와 관리자).
              { path: 'update-campaigns', element: <UpdateCampaignPage /> },
              {
                element: <AdminGuard adminOnly />,
                children: [{ path: 'organizations', element: <OrganizationAdminPage /> }],
              },
            ],
          },
          // 가. 연락처: 목록(기관별 묶음, ?id= 상세 패널).
          { path: 'contacts', element: <ContactListPage /> },
          // 나. 의견 요청: 목록, 작성, 상세(회신 현황·취합표 탭, 기관 회신), 고치기.
          { path: 'opinions', element: <OpinionListPage /> },
          { path: 'opinions/new', element: <OpinionEditorPage /> },
          { path: 'opinions/:id', element: <OpinionDetailPage /> },
          { path: 'opinions/:id/edit', element: <OpinionEditorPage /> },
          // 다. 기관 테스트: 목록, 만들기, 수행·결과 관리(결과·내용·이력 탭), 고치기, 테스트별 결과 등록 알림 설정.
          { path: 'tests', element: <TestPlanListPage /> },
          { path: 'tests/new', element: <TestPlanEditorPage /> },
          { path: 'tests/:id', element: <TestPlanDetailPage /> },
          { path: 'tests/:id/edit', element: <TestPlanEditorPage /> },
          { path: 'tests/:id/recipients', element: <TestPlanRecipientsPage /> },
          { path: 'suspensions', element: <SuspensionListPage /> },
          { path: 'suspensions/new', element: <SuspensionEditorPage /> },
          { path: 'suspensions/:id', element: <SuspensionDetailPage /> },
          { path: 'suspensions/:id/edit', element: <SuspensionEditorPage /> },
          ...FEATURE_MENU.filter((m) => !BUILT_FEATURES.includes(m.key)).map((m) => ({
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
