import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { LoginPage } from './LoginPage';

const { GET, POST } = vi.hoisted(() => ({ GET: vi.fn(), POST: vi.fn() }));

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>();
  return { ...actual, api: { GET, POST } };
});

const ok = (data: unknown) => ({ data, response: new Response(null, { status: 200 }) });
const fail = (status: number, body: unknown) => ({
  error: body,
  response: new Response(null, { status }),
});

const demoAccounts = {
  password: 'demo1234!',
  accounts: [
    {
      email: 'admin@kftc.example.com',
      name: '데모 관리자',
      organizationName: '금융결제원',
      roleLabel: '결제원 · 전산(운영)',
      admin: true,
      businessLabels: ['계좌통합관리', '자동이체통합관리', '대출이동', 'CMS'],
    },
    {
      email: 'kim.minjun@kb.example.com',
      name: '김민준',
      organizationName: 'KB국민은행',
      roleLabel: '참가기관 · 현업',
      admin: false,
      businessLabels: ['계좌통합관리'],
    },
  ],
};

beforeEach(() => {
  GET.mockReset();
  POST.mockReset();
  GET.mockImplementation(async (path: string) => {
    switch (path) {
      case '/api/v1/me':
        return fail(401, { code: 'UNAUTHORIZED', detail: '로그인이 필요합니다.' });
      case '/api/v1/health':
        return ok({ status: 'UP', profile: 'demo', serverTime: '2026-10-15T10:00:00+09:00' });
      case '/api/v1/businesses':
        return ok([]);
      case '/api/v1/demo/accounts':
        return ok(demoAccounts);
      case '/api/v1/support-info':
        return ok({ inquiryEmail: 'help@connect-link.example.com' });
      default:
        throw new Error(`예상하지 못한 GET ${path}`);
    }
  });
});

describe('LoginPage', () => {
  it('이용 가이드와 서버가 알려 준 문의 메일 주소를 보인다', async () => {
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    expect(screen.getByRole('region', { name: '연결이 주요 기능' })).toHaveTextContent(
      '담당자 연락처',
    );
    expect(screen.getByRole('link', { name: '가이드 보기' })).toHaveAttribute('href', '/guide');
    expect(await screen.findByRole('link', { name: '문의 메일 보내기' })).toHaveAttribute(
      'href',
      'mailto:help@connect-link.example.com',
    );
  });

  it('문의 메일 주소가 없으면 문의하기 칸을 뺀다', async () => {
    const base = GET.getMockImplementation()!;
    GET.mockImplementation(async (path: string) =>
      path === '/api/v1/support-info' ? ok({}) : base(path),
    );
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    await waitFor(() => expect(GET).toHaveBeenCalledWith('/api/v1/support-info'));
    expect(screen.getByRole('link', { name: '가이드 보기' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '문의 메일 보내기' })).not.toBeInTheDocument();
  });

  it('틀린 비밀번호면 서버 메시지를 그대로 보여 준다', async () => {
    POST.mockResolvedValue(
      fail(401, { code: 'INVALID_CREDENTIALS', detail: '이메일 또는 비밀번호가 맞지 않습니다.' }),
    );
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    fireEvent.change(screen.getByLabelText('회사 이메일'), {
      target: { value: 'kim.minjun@kb.example.com' },
    });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByTestId('login-error')).toHaveTextContent(
      '이메일 또는 비밀번호가 맞지 않습니다.',
    );
    expect(POST).toHaveBeenCalledWith('/api/v1/auth/login', {
      body: { email: 'kim.minjun@kb.example.com', password: 'wrong' },
    });
  });

  it('탈퇴하고 오면 탈퇴 완료 안내를 보이고, 로그인에 실패하면 실패 안내로 바꾼다', async () => {
    POST.mockResolvedValue(
      fail(401, { code: 'INVALID_CREDENTIALS', detail: '이메일 또는 비밀번호가 맞지 않습니다.' }),
    );
    renderWithProviders(<LoginPage />, { route: '/login?withdrawn=1', path: '/login' });
    expect(await screen.findByTestId('login-withdrawn')).toHaveTextContent(
      '탈퇴가 완료되었습니다.',
    );

    fireEvent.change(screen.getByLabelText('회사 이메일'), {
      target: { value: 'kim.minjun@kb.example.com' },
    });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));
    expect(await screen.findByTestId('login-error')).toBeInTheDocument();
    expect(screen.queryByTestId('login-withdrawn')).not.toBeInTheDocument();
  });

  it('빈 칸이면 서버를 부르지 않고 필드 메시지를 보여 준다', async () => {
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));
    expect(await screen.findByText('회사 이메일을 입력해 주세요.')).toBeInTheDocument();
    expect(screen.getByText('비밀번호를 입력해 주세요.')).toBeInTheDocument();
    expect(POST).not.toHaveBeenCalled();
  });

  it('demo 프로필이면 역할별 데모 계정을 보여 주고, 고르면 공통 비밀번호로 로그인한다', async () => {
    POST.mockResolvedValue(fail(403, { code: 'ACCOUNT_PENDING_APPROVAL', detail: '승인 전' }));
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' });

    const list = await screen.findByTestId('demo-accounts');
    expect(list).toHaveTextContent('데모 관리자');
    expect(list).toHaveTextContent('참가기관 · 현업');
    expect(screen.getByText('demo1234!')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '김민준 (참가기관 · 현업)로 로그인' }));
    await waitFor(() =>
      expect(POST).toHaveBeenCalledWith('/api/v1/auth/login', {
        body: { email: 'kim.minjun@kb.example.com', password: 'demo1234!' },
      }),
    );
    expect(await screen.findByTestId('login-error')).toHaveTextContent('승인 전');
  });
});
