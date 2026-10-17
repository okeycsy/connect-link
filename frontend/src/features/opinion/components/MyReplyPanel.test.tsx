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
  questions: [
    {
      id: 21,
      type: 'SINGLE_CHOICE',
      typeLabel: '객관식',
      title: '적용 시기',
      required: true,
      options: [
        { id: 31, label: '1월' },
        { id: 32, label: '7월' },
      ],
    },
    {
      id: 22,
      type: 'MULTIPLE_CHOICE',
      typeLabel: '체크박스',
      title: '영향받는 업무',
      required: false,
      options: [
        { id: 33, label: '정산' },
        { id: 34, label: '안내' },
      ],
    },
    {
      id: 23,
      type: 'TEXT',
      typeLabel: '주관식',
      title: '월 조회 건수',
      required: false,
      options: [],
    },
  ],
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

type Body = {
  answers?: { questionId: number; optionIds?: number[]; text?: string }[];
};

const empty = {
  requestId: 7,
  targetId: 3,
  organizationName: '가나은행',
  replyId: null,
  status: null,
  statusLabel: '미작성',
  answers: [],
  draftAnswers: [],
  replierName: null,
  submittedAt: null,
  revisionReason: null,
  revisionRequestedAt: null,
  revisionRejectionReason: null,
  editable: true,
  revisionRequestable: false,
};

const toAnswers = (body: Body) =>
  (body.answers ?? []).map((a) => ({
    questionId: a.questionId,
    optionIds: a.optionIds ?? [],
    text: a.text ?? null,
  }));

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
      PUT: vi.fn(async (path: string, options: { body: Body }) => {
        calls.writes.push({ method: 'PUT', path, body: options.body });
        return {
          data: {
            ...empty,
            replyId: 11,
            status: 'DRAFT',
            statusLabel: '임시저장',
            draftAnswers: toAnswers(options.body),
          },
          response: new Response(),
        };
      }),
      POST: vi.fn(async (path: string, options: { body: Body }) => {
        calls.writes.push({ method: 'POST', path, body: options.body });
        // 서버처럼 필수 질문(21)에 답하지 않으면 그 질문 칸에 오류를 붙인다.
        if (!options.body.answers?.some((a) => a.questionId === 21)) {
          return {
            error: {
              status: 400,
              code: 'OPINION_ANSWER_REQUIRED',
              detail: '필수 질문입니다. 답해 주세요.',
              fieldErrors: [{ field: 'answers.21', message: '필수 질문입니다. 답해 주세요.' }],
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
            answers: toAnswers(options.body),
            replierName: '김가나',
            submittedAt: '2026-10-17T10:00:00',
            editable: false,
            revisionRequestable: true,
          },
          response: new Response(),
        };
      }),
    },
  };
});

beforeEach(() => {
  calls.writes.length = 0;
});

describe('MyReplyPanel', () => {
  it('의견 유형·세부의견 칸 없이 질문만 보인다', async () => {
    renderWithProviders(<MyReplyPanel request={request} />, {
      route: '/account-info/opinions/7',
      path: '/:business/opinions/:id',
    });
    expect(await screen.findByRole('radio', { name: '7월' })).toBeInTheDocument();
    expect(screen.queryByRole('radio', { name: '동의' })).toBeNull();
    expect(screen.queryByLabelText('세부의견')).toBeNull();
  });

  it('임시저장하면 저장 안내가 뜨고 근거자료를 올릴 수 있게 된다', async () => {
    renderWithProviders(<MyReplyPanel request={request} />, {
      route: '/account-info/opinions/7',
      path: '/:business/opinions/:id',
    });

    expect(
      await screen.findByText('임시저장하면 근거자료를 올릴 수 있습니다.'),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole('radio', { name: '1월' }));
    fireEvent.click(screen.getByRole('button', { name: '임시저장' }));
    expect(await screen.findByTestId('reply-notice')).toHaveTextContent('임시저장했습니다');
    await waitFor(() => expect(screen.getByText('근거자료 올리기')).toBeInTheDocument());
    expect(calls.writes[0]).toMatchObject({
      method: 'PUT',
      body: { answers: [{ questionId: 21, optionIds: [31] }] },
    });
  });

  it('질문에 답해 제출하고, 필수 질문을 비우면 그 질문 아래에 서버 메시지를 보인다', async () => {
    renderWithProviders(<MyReplyPanel request={request} />, {
      route: '/account-info/opinions/7',
      path: '/:business/opinions/:id',
    });

    fireEvent.click(await screen.findByRole('checkbox', { name: '정산' }));
    fireEvent.click(screen.getByRole('button', { name: '최종제출' }));
    expect(await screen.findByText('필수 질문입니다. 답해 주세요.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('radio', { name: '7월' }));
    expect(screen.queryByText('필수 질문입니다. 답해 주세요.')).toBeNull();
    fireEvent.click(screen.getByRole('checkbox', { name: '안내' }));
    fireEvent.change(screen.getByRole('textbox', { name: '3. 월 조회 건수' }), {
      target: { value: '약 1만 건' },
    });
    fireEvent.click(screen.getByRole('button', { name: '최종제출' }));

    const submitted = await screen.findByTestId('submitted-answers');
    expect(submitted).toHaveTextContent('1. 적용 시기7월');
    expect(submitted).toHaveTextContent('2. 영향받는 업무정산, 안내');
    expect(submitted).toHaveTextContent('3. 월 조회 건수약 1만 건');
    expect(screen.getByRole('button', { name: '수정 요청' })).toBeInTheDocument();
    expect(calls.writes.at(-1)?.body).toEqual({
      answers: [
        { questionId: 21, optionIds: [32] },
        { questionId: 22, optionIds: [33, 34] },
        { questionId: 23, text: '약 1만 건' },
      ],
    });
  });
});
