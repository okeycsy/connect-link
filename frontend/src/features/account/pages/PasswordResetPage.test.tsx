import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { PasswordResetPage } from './PasswordResetPage';

const { GET, POST } = vi.hoisted(() => ({ GET: vi.fn(), POST: vi.fn() }));

vi.mock('../../../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../api/client')>();
  return { ...actual, api: { GET, POST } };
});

const ok = (data: unknown, status = 200) => ({
  data,
  response: new Response(null, { status }),
});
const fail = (status: number, body: unknown) => ({
  error: body,
  response: new Response(null, { status }),
});

beforeEach(() => {
  GET.mockReset();
  POST.mockReset();
  GET.mockImplementation(async (path: string) => {
    if (path === '/api/v1/health') {
      return ok({ status: 'UP', profile: 'prod', serverTime: '2026-10-25T10:00:00+09:00' });
    }
    throw new Error(`예상하지 못한 GET ${path}`);
  });
});

describe('PasswordResetPage', () => {
  it('코드를 받으면 코드와 새 비밀번호 칸이 나오고, 서버의 칸 오류를 그대로 보인다', async () => {
    POST.mockImplementation(async (path: string) => {
      if (path === '/api/v1/password-resets') {
        return ok({ email: 'kim@kb.example.com', resendAfterSeconds: 60 }, 202);
      }
      return fail(400, {
        code: 'VERIFICATION_CODE_MISMATCH',
        detail: '인증 코드가 맞지 않습니다.',
        fieldErrors: [
          {
            field: 'code',
            message: '인증 코드가 맞지 않습니다. 4번 더 틀리면 코드를 다시 받아야 합니다.',
          },
        ],
      });
    });
    renderWithProviders(<PasswordResetPage />, {
      route: '/password-reset',
      path: '/password-reset',
    });

    fireEvent.change(screen.getByLabelText(/회사 이메일/), {
      target: { value: 'KIM@kb.example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: '인증 코드 받기' }));

    expect(await screen.findByTestId('password-reset-sent')).toHaveTextContent(
      'kim@kb.example.com',
    );
    fireEvent.change(screen.getByLabelText(/인증 코드/), { target: { value: '123456' } });
    fireEvent.change(screen.getByLabelText(/^새 비밀번호 \*?$/), {
      target: { value: 'Changed2@' },
    });
    fireEvent.change(screen.getByLabelText(/새 비밀번호 확인/), { target: { value: 'Changed2@' } });
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 바꾸기' }));

    expect(await screen.findByText(/4번 더 틀리면/)).toBeInTheDocument();
    await waitFor(() =>
      expect(POST).toHaveBeenCalledWith('/api/v1/password-resets/confirm', {
        body: { email: 'kim@kb.example.com', code: '123456', newPassword: 'Changed2@' },
      }),
    );
  });
});
