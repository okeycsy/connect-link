import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { SignUpPage } from './SignUpPage';

const { GET, POST, navigate } = vi.hoisted(() => ({
  GET: vi.fn(),
  POST: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>();
  return { ...actual, api: { GET, POST } };
});

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigate };
});

const ok = (data: unknown, status = 200) => ({ data, response: new Response(null, { status }) });
const fail = (status: number, body: unknown) => ({
  error: body,
  response: new Response(null, { status }),
});

const businesses = [
  { code: 'ACCOUNT_INFO', slug: 'account-info', label: '계좌통합관리', shortLabel: '계통' },
  { code: 'AUTO_TRANSFER', slug: 'auto-transfer', label: '자동이체통합관리', shortLabel: '자통' },
  { code: 'LOAN_TRANSFER', slug: 'loan-transfer', label: '대출이동', shortLabel: '대출' },
  { code: 'CMS', slug: 'cms', label: 'CMS', shortLabel: 'CMS' },
];

const organizations = [
  {
    id: 1,
    code: 'KFTC',
    name: '금융결제원',
    type: 'KFTC',
    typeLabel: '결제원',
    businesses: ['ACCOUNT_INFO', 'AUTO_TRANSFER', 'LOAN_TRANSFER', 'CMS'],
  },
  {
    id: 3,
    code: '088',
    name: '신한은행',
    type: 'FINANCE_ORGANIZATION',
    typeLabel: '참가기관',
    businesses: ['ACCOUNT_INFO', 'AUTO_TRANSFER', 'CMS'],
  },
  {
    id: 11,
    code: 'HAHA',
    name: '하하통신',
    type: 'USE_ORGANIZATION',
    typeLabel: '이용기관',
    businesses: ['CMS'],
  },
];

const jobRoles = [
  { code: 'BUSINESS', label: '현업', itRoles: [] },
  {
    code: 'IT',
    label: '전산',
    itRoles: [
      { code: 'DEV', label: '개발' },
      { code: 'OPS', label: '운영' },
      { code: 'INFRA', label: '인프라' },
      { code: 'EXTERNAL_LINK', label: '대외계' },
      { code: 'CONTROL_ROOM', label: '상황실' },
    ],
  },
];

const EMAIL = 'new.person@shinhan.example.com';

beforeEach(() => {
  GET.mockReset();
  POST.mockReset();
  navigate.mockReset();
  GET.mockImplementation(async (path: string) => {
    switch (path) {
      case '/api/v1/health':
        return ok({ status: 'UP', profile: 'prod', serverTime: '2026-10-16T10:00:00+09:00' });
      case '/api/v1/businesses':
        return ok(businesses);
      case '/api/v1/organizations':
        return ok(organizations);
      case '/api/v1/job-roles':
        return ok(jobRoles);
      default:
        throw new Error(`예상하지 못한 GET ${path}`);
    }
  });
});

function change(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label, { exact: false }), { target: { value } });
}

async function verifyEmail() {
  change('회사 이메일', EMAIL);
  fireEvent.click(screen.getByRole('button', { name: '인증 코드 받기' }));
  await screen.findByLabelText('인증 코드', { exact: false });
  change('인증 코드', '123456');
  fireEvent.click(screen.getByRole('button', { name: '확인' }));
  await screen.findByTestId('email-verified');
}

describe('SignUpPage', () => {
  it('이메일을 인증하고 네 단계를 거쳐 한 번에 신청하면 승인 대기 화면으로 간다', async () => {
    POST.mockImplementation(async (path: string, { body }: { body: unknown }) => {
      switch (path) {
        case '/api/v1/email-verifications':
          return ok(
            {
              email: EMAIL,
              expiresAt: '2026-10-16T10:10:00',
              resendAfterSeconds: 60,
            },
            201,
          );
        case '/api/v1/email-verifications/confirm':
          return ok({
            email: EMAIL,
            verificationToken: 'token-1',
            tokenExpiresAt: '2026-10-16T10:40:00',
            suggestedOrganizationIds: [3],
          });
        case '/api/v1/sign-ups':
          return ok({ ...(body as object), status: 'PENDING_APPROVAL' }, 201);
        default:
          throw new Error(`예상하지 못한 POST ${path}`);
      }
    });
    renderWithProviders(<SignUpPage />, { route: '/sign-up', path: '/sign-up' });

    // ① 인증 전에는 다음으로 갈 수 없다.
    expect(screen.getByRole('button', { name: '다음' })).toBeDisabled();
    await verifyEmail();
    fireEvent.click(screen.getByRole('button', { name: '다음' }));

    // ② 이메일 도메인이 같은 신한은행이 미리 골라져 있고, 참여하지 않는 대출이동은 못 고른다.
    await screen.findByTestId('sign-up-step-2');
    expect(await screen.findByDisplayValue('신한은행')).toBeInTheDocument();
    expect(screen.getByText('회사 이메일 도메인과 맞는 기관입니다.')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: '대출이동' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: '다음' }));
    expect(await screen.findByText('담당 업무를 하나 이상 골라 주세요.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('checkbox', { name: 'CMS' }));
    change('CMS 세부업무', 'CMS 출금이체');
    fireEvent.click(screen.getByRole('checkbox', { name: '계좌통합관리' }));
    fireEvent.click(screen.getByRole('button', { name: '다음' }));

    // ③ 전산이면 세부 직무를 골라야 넘어간다.
    await screen.findByTestId('sign-up-step-3');
    fireEvent.click(await screen.findByRole('radio', { name: '전산' }));
    fireEvent.click(screen.getByRole('button', { name: '다음' }));
    expect(await screen.findByText('전산 세부 직무를 하나 이상 골라 주세요.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('checkbox', { name: '운영' }));
    fireEvent.click(screen.getByRole('button', { name: '다음' }));

    // ④ 앞 단계 요약을 보고 담당자 정보를 적는다.
    const summary = await screen.findByTestId('sign-up-summary');
    expect(summary).toHaveTextContent('신한은행');
    expect(summary).toHaveTextContent('계좌통합관리');
    expect(summary).toHaveTextContent('CMS · CMS 출금이체');
    expect(summary).toHaveTextContent('전산(운영)');
    change('이름', '새담당');
    change('직급', '대리');
    change('부서', '디지털금융부');
    change('팀', '플랫폼팀');
    change('내선번호', '1234');
    change('비밀번호 확인', 'Welcome1?');
    fireEvent.change(screen.getAllByLabelText(/^비밀번호/, { selector: 'input' })[0], {
      target: { value: 'Welcome1!' },
    });
    fireEvent.click(screen.getByRole('button', { name: '가입 신청' }));
    expect(await screen.findByText('비밀번호가 서로 다릅니다.')).toBeInTheDocument();
    change('비밀번호 확인', 'Welcome1!');
    fireEvent.click(screen.getByRole('button', { name: '가입 신청' }));

    await waitFor(() => expect(navigate).toHaveBeenCalled());
    expect(POST).toHaveBeenCalledWith('/api/v1/sign-ups', {
      body: {
        verificationToken: 'token-1',
        email: EMAIL,
        organizationId: 3,
        // 업무 순서로 보낸다(고른 순서와 무관).
        businesses: [
          { business: 'ACCOUNT_INFO', subBusiness: null },
          { business: 'CMS', subBusiness: 'CMS 출금이체' },
        ],
        jobRole: 'IT',
        itRoles: ['OPS'],
        name: '새담당',
        department: '디지털금융부',
        team: '플랫폼팀',
        jobTitle: '대리',
        extensionPhone: '1234',
        mobilePhone: null,
        password: 'Welcome1!',
      },
    });
    expect(navigate.mock.calls[0][0]).toBe('/sign-up/pending');
  });

  it('틀린 코드와 이미 가입한 이메일은 서버 메시지를 그 칸 아래에 보여 준다', async () => {
    POST.mockImplementation(async (path: string) => {
      if (path === '/api/v1/email-verifications') {
        return ok(
          {
            email: EMAIL,
            expiresAt: '2026-10-16T10:10:00',
            resendAfterSeconds: 60,
          },
          201,
        );
      }
      return fail(400, {
        code: 'VERIFICATION_CODE_MISMATCH',
        detail: '인증 코드가 맞지 않습니다. 4번 더 틀리면 코드를 다시 받아야 합니다.',
        fieldErrors: [
          {
            field: 'code',
            message: '인증 코드가 맞지 않습니다. 4번 더 틀리면 코드를 다시 받아야 합니다.',
          },
        ],
      });
    });
    renderWithProviders(<SignUpPage />, { route: '/sign-up', path: '/sign-up' });

    change('회사 이메일', EMAIL);
    fireEvent.click(screen.getByRole('button', { name: '인증 코드 받기' }));
    await screen.findByLabelText('인증 코드', { exact: false });
    // 재발송 간격 안에는 다시 받기를 못 누른다.
    expect(screen.getByRole('button', { name: /코드 다시 받기 \(\d+초\)/ })).toBeDisabled();
    change('인증 코드', '000000');
    fireEvent.click(screen.getByRole('button', { name: '확인' }));
    expect(
      await screen.findByText(
        '인증 코드가 맞지 않습니다. 4번 더 틀리면 코드를 다시 받아야 합니다.',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '다음' })).toBeDisabled();
  });
  it('서버에 보낸 코드 기록이 없으면 바로 다시 받을 수 있게 이메일 칸으로 되돌린다', async () => {
    const message = '이 이메일로 보낸 인증 코드 기록이 없습니다. 인증 코드를 다시 받아 주세요.';
    POST.mockImplementation(async (path: string) => {
      if (path === '/api/v1/email-verifications') {
        return ok({ email: EMAIL, expiresAt: '2026-10-16T10:10:00', resendAfterSeconds: 60 }, 201);
      }
      return fail(400, {
        code: 'VERIFICATION_NOT_REQUESTED',
        detail: message,
        fieldErrors: [{ field: 'code', message }],
      });
    });
    renderWithProviders(<SignUpPage />, { route: '/sign-up', path: '/sign-up' });

    change('회사 이메일', EMAIL);
    fireEvent.click(screen.getByRole('button', { name: '인증 코드 받기' }));
    await screen.findByLabelText('인증 코드', { exact: false });
    change('인증 코드', '123456');
    fireEvent.click(screen.getByRole('button', { name: '확인' }));

    expect(await screen.findByText(message)).toBeInTheDocument();
    // 재발송 간격을 기다리지 않고 같은 이메일로 바로 다시 받는다.
    expect(screen.queryByLabelText('인증 코드', { exact: false })).not.toBeInTheDocument();
    expect(screen.getByLabelText('회사 이메일', { exact: false })).toHaveValue(EMAIL);
    expect(screen.getByRole('button', { name: '인증 코드 받기' })).toBeEnabled();
  });
});
