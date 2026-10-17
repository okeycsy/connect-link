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
  questions: [],
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

const withQuestions = {
  ...request,
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
} as OpinionRequestDetail;

type Body = {
  type: string | null;
  content: string;
  answers?: { questionId: number; optionIds?: number[]; text?: string }[];
};

const empty = {
  requestId: 7,
  targetId: 3,
  organizationName: '가나은행',
  replyId: null,
  status: null,
  statusLabel: '미작성',
  type: null,
  typeLabel: null,
  content: null,
  draftType: null,
  draftContent: null,
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
            draftType: options.body.type,
            draftContent: options.body.content,
          },
          response: new Response(),
        };
      }),
      POST: vi.fn(async (path: string, options: { body: Body }) => {
        calls.writes.push({ method: 'POST', path, body: options.body });
        if (
          options.body.type === 'AGREE' &&
          options.body.answers?.length &&
          !options.body.answers.some((a) => a.questionId === 21)
        ) {
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
        if (options.body.type === 'DISAGREE' && !options.body.content) {
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
            answers: (options.body.answers ?? []).map((a) => ({
              questionId: a.questionId,
              optionIds: a.optionIds ?? [],
              text: a.text ?? null,
            })),
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
      body: { type: 'DISAGREE', content: '시행 시기를 늦춰 주세요.', answers: [] },
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

  it('질문에 답해 제출하고, 필수 질문을 비우면 그 질문 아래에 서버 메시지를 보인다', async () => {
    renderWithProviders(<MyReplyPanel request={withQuestions} />, {
      route: '/account-info/opinions/7',
      path: '/:business/opinions/:id',
    });

    fireEvent.click(await screen.findByRole('radio', { name: '동의' }));
    fireEvent.click(screen.getByRole('checkbox', { name: '정산' }));
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
    expect(calls.writes.at(-1)?.body).toEqual({
      type: 'AGREE',
      content: '',
      answers: [
        { questionId: 21, optionIds: [32] },
        { questionId: 22, optionIds: [33, 34] },
        { questionId: 23, text: '약 1만 건' },
      ],
    });
  });
});
