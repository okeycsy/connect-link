import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { MyInfoPage } from './MyInfoPage';

const admin = {
  email: 'admin@kftc.example.com',
  name: '데모 관리자',
  organizationCode: 'KFTC',
  organizationName: '금융결제원',
  organizationType: 'KFTC',
  organizationTypeLabel: '결제원',
  admin: true,
  businesses: [
    { code: 'ACCOUNT_INFO', slug: 'account-info', label: '계좌통합관리', shortLabel: '계통' },
    { code: 'CMS', slug: 'cms', label: 'CMS', shortLabel: 'CMS' },
  ],
  jobRole: 'IT',
  jobRoleLabel: '전산(운영)',
  itRoles: ['OPS'],
  roleLabel: '결제원 · 전산(운영)',
  networkZone: 'EXTERNAL',
  networkZoneLabel: '인터넷망',
};

const member = {
  ...admin,
  email: 'kim.minjun@kb.example.com',
  name: '김민준',
  organizationCode: '004',
  organizationName: 'KB국민은행',
  organizationType: 'FINANCE_ORGANIZATION',
  organizationTypeLabel: '참가기관',
  admin: false,
  businesses: [admin.businesses[0]],
  jobRole: 'BUSINESS',
  jobRoleLabel: '현업',
  itRoles: [],
};

const state = vi.hoisted(() => ({
  me: {} as Record<string, unknown>,
  withdrawal: {} as Record<string, unknown>,
  posts: [] as { path: string; body: unknown }[],
  navigated: [] as string[],
}));

vi.mock('react-router-dom', async (importOriginal) => {
  const original = await importOriginal<typeof import('react-router-dom')>();
  return { ...original, useNavigate: () => (to: string) => state.navigated.push(to) };
});

vi.mock('../../../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../../api/client')>();
  return {
    ...original,
    api: {
      GET: vi.fn(async (path: string) => {
        if (path === '/api/v1/me') return { data: state.me, response: new Response() };
        if (path === '/api/v1/me/withdrawal') {
          return { data: state.withdrawal, response: new Response() };
        }
        throw new Error(`unexpected GET ${path}`);
      }),
      POST: vi.fn(async (path: string, options: { body: { password: string } }) => {
        state.posts.push({ path, body: options.body });
        if (options.body.password !== 'Welcome1!') {
          return {
            error: {
              status: 400,
              code: 'PASSWORD_MISMATCH',
              detail: '비밀번호가 맞지 않습니다.',
              fieldErrors: [{ field: 'password', message: '비밀번호가 맞지 않습니다.' }],
            },
            response: new Response(null, { status: 400 }),
          };
        }
        return { response: new Response(null, { status: 204 }) };
      }),
    },
  };
});

function renderPage() {
  return renderWithProviders(<MyInfoPage />, { route: '/cms/me', path: '/:business/me' });
}

beforeEach(() => {
  state.posts.length = 0;
  state.navigated.length = 0;
});

describe('MyInfoPage', () => {
  it('계정 정보, 담당 업무·직무와 접속 망을 보여 주고 연락처는 준비 중으로 둔다', async () => {
    state.me = admin;
    state.withdrawal = { allowed: true, blockers: [] };
    renderPage();
    expect(await screen.findByText('데모 관리자')).toBeInTheDocument();
    expect(screen.getByText('금융결제원 · 결제원')).toBeInTheDocument();
    expect(screen.getByText('admin@kftc.example.com')).toBeInTheDocument();
    expect(screen.getByText('관리자')).toBeInTheDocument();
    expect(screen.getByText('계좌통합관리')).toBeInTheDocument();
    expect(screen.getByText('전산(운영)')).toBeInTheDocument();
    expect(screen.getByText('인터넷망')).toBeInTheDocument();
    expect(screen.getByText('준비 중 · 연락처 모듈')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '연락처 수정' })).toBeDisabled();
  });

  it('탈퇴를 막는 까닭이 있으면 그 까닭과 바로 가기를 보이고 탈퇴 버튼을 막는다', async () => {
    state.me = admin;
    state.withdrawal = {
      allowed: false,
      blockers: [
        {
          code: 'LAST_ADMIN',
          message: '포털 관리자가 한 명뿐이라 탈퇴할 수 없습니다.',
          link: null,
        },
        {
          code: 'SUCCESSOR_REQUIRED',
          message: '후임자를 연락처에 먼저 등록해 주세요.',
          link: '/cms/contacts',
        },
      ],
    };
    renderPage();
    const blockers = await screen.findByTestId('withdraw-blockers');
    expect(blockers).toHaveTextContent('지금은 탈퇴할 수 없습니다');
    expect(blockers).toHaveTextContent('포털 관리자가 한 명뿐이라 탈퇴할 수 없습니다.');
    expect(within(blockers).getByRole('link', { name: '바로 가기' })).toHaveAttribute(
      'href',
      '/cms/contacts',
    );
    expect(screen.getByRole('button', { name: '탈퇴하기' })).toBeDisabled();
  });

  it('탈퇴는 비밀번호를 다시 받고, 틀리면 서버 메시지를 칸 아래에 보이며, 맞으면 로그인 화면으로 간다', async () => {
    state.me = member;
    state.withdrawal = { allowed: true, blockers: [] };
    renderPage();

    const open = await screen.findByRole('button', { name: '탈퇴하기' });
    await waitFor(() => expect(open).toBeEnabled());
    fireEvent.click(open);
    const dialog = await screen.findByRole('dialog', { name: '탈퇴' });
    expect(dialog).toHaveTextContent('KB국민은행 김민준 님의 연결고리 계정을 탈퇴합니다.');

    const password = within(dialog).getByLabelText(/비밀번호/);
    fireEvent.change(password, { target: { value: 'Wrong123!' } });
    fireEvent.click(within(dialog).getByRole('button', { name: '탈퇴하기' }));
    expect(await within(dialog).findByText('비밀번호가 맞지 않습니다.')).toBeInTheDocument();
    expect(state.navigated).toEqual([]);

    fireEvent.change(password, { target: { value: 'Welcome1!' } });
    // 칸을 고치면 서버 메시지가 지워진다.
    expect(within(dialog).queryByText('비밀번호가 맞지 않습니다.')).not.toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: '탈퇴하기' }));
    await waitFor(() => expect(state.navigated).toEqual(['/login?withdrawn=1']));
    expect(state.posts).toEqual([
      { path: '/api/v1/me/withdrawal', body: { password: 'Wrong123!' } },
      { path: '/api/v1/me/withdrawal', body: { password: 'Welcome1!' } },
    ]);
  });
});
