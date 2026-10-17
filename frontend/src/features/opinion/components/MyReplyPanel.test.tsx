import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import type { OpinionRequestDetail } from '../hooks';
import { MyReplyPanel } from './MyReplyPanel';

const request = {
  id: 7,
  business: 'ACCOUNT_INFO',
  businessSlug: 'account-info',
  businessLabel: '계좌통합관리',
  title: '수수료 체계 개편 의견 요청',
  content: '의견을 주세요.',
  dueDate: '2026-10-20',
  daysLeft: 3,
  status: 'OPEN',
  statusLabel: '회신중',
  cancelled: false,
  cancelReason: null,
  ownerName: '홍길동',
  openedAt: '2026-10-10T09:00:00',
  closedAt: null,
  createdAt: '2026-10-09T09:00:00',
  updatedAt: '2026-10-10T09:00:00',
  forms: [],
  targets: [],
  permissions: {
    manage: false,
    edit: false,
    open: false,
    close: false,
    cancel: false,
    delete: false,
    reply: true,
  },
} as OpinionRequestDetail;

const empty = {
  requestId: 7,
  targetId: 3,
  organizationName: '가나은행',
  formName: null,
  replyId: null,
  status: null,
  statusLabel: '미작성',
  type: null,
  typeLabel: null,
  content: null,
  draftType: null,
  draftContent: null,
  replierName: null,
  submittedAt: null,
  revisionReason: null,
  revisionRequestedAt: null,
  revisionRejectionReason: null,
  editable: true,
  revisionRequestable: false,
};

const calls = vi.hoisted(() => ({
  writes: [] as { method: string; path: string; body: unknown }[],
}));

vi.mock('../../../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../../api/client')>();
  return {
    ...original,
    api: {
      GET: vi.fn(async (path: string) => {
        if (path === '/api/v1/opinion-requests/{requestId}/my-reply') {
          return { data: empty, response: new Response() };
        }
        if (path === '/api/v1/attachments') return { data: [], response: new Response() };
        throw new Error(`unexpected GET ${path}`);
      }),
      PUT: vi.fn(
        async (path: string, options: { body: { type: string | null; content: string } }) => {
          calls.writes.push({ method: 'PUT', path, body: options.body });
          return {
            data: {
              ...empty,
              replyId: 11,
              status: 'DRAFT',
              statusLabel: '임시저장',
              draftType: options.body.type,
              draftContent: options.body.content,
            },
            response: new Response(),
          };
        },
      ),
      POST: vi.fn(
        async (path: string, options: { body: { type: string | null; content: string } }) => {
          calls.writes.push({ method: 'POST', path, body: options.body });
          if (!options.body.content) {
            return {
              error: {
                status: 400,
                code: 'OPINION_REPLY_CONTENT_REQUIRED',
                detail: '입력값을 확인해 주세요.',
                fieldErrors: [{ field: 'content', message: '이견은(는) 세부의견을 적어 주세요.' }],
              },
              response: new Response(null, { status: 400 }),
            };
          }
          return {
            data: {
              ...empty,
              replyId: 11,
              status: 'SUBMITTED',
              statusLabel: '제출',
              type: options.body.type,
              content: options.body.content,
              replierName: '김가나',
              submittedAt: '2026-10-17T10:00:00',
              editable: false,
              revisionRequestable: true,
            },
            response: new Response(),
          };
        },
      ),
    },
  };
});

beforeEach(() => {
  calls.writes.length = 0;
});

describe('MyReplyPanel', () => {
  it('세부의견 없이 이견을 제출하면 서버 메시지를 칸 아래에 보이고, 고쳐 제출하면 제출 내용을 보인다', async () => {
    renderWithProviders(<MyReplyPanel request={request} />, {
      route: '/account-info/opinions/7',
      path: '/:business/opinions/:id',
    });

    fireEvent.click(await screen.findByRole('radio', { name: '이견' }));
    fireEvent.click(screen.getByRole('button', { name: '최종제출' }));
    expect(await screen.findByText('이견은(는) 세부의견을 적어 주세요.')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('세부의견'), {
      target: { value: '시행 시기를 늦춰 주세요.' },
    });
    fireEvent.click(screen.getByRole('button', { name: '최종제출' }));
    expect(await screen.findByTestId('submitted-reply')).toHaveTextContent(
      '시행 시기를 늦춰 주세요.',
    );
    expect(screen.getByRole('button', { name: '수정 요청' })).toBeInTheDocument();
    expect(calls.writes.at(-1)).toEqual({
      method: 'POST',
      path: '/api/v1/opinion-requests/{requestId}/my-reply/submission',
      body: { type: 'DISAGREE', content: '시행 시기를 늦춰 주세요.' },
    });
  });

  it('임시저장하면 저장 안내가 뜨고 근거자료를 올릴 수 있게 된다', async () => {
    renderWithProviders(<MyReplyPanel request={request} />, {
      route: '/account-info/opinions/7',
      path: '/:business/opinions/:id',
    });

    expect(
      await screen.findByText('임시저장하면 근거자료를 올릴 수 있습니다.'),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole('radio', { name: '동의' }));
    fireEvent.click(screen.getByRole('button', { name: '임시저장' }));
    expect(await screen.findByTestId('reply-notice')).toHaveTextContent('임시저장했습니다');
    await waitFor(() => expect(screen.getByText('근거자료 올리기')).toBeInTheDocument());
    expect(calls.writes[0]).toMatchObject({ method: 'PUT', body: { type: 'AGREE', content: '' } });
  });
});
