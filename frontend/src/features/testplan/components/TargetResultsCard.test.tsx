import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { TargetResultsCard } from './TargetResultsCard';

const summary = {
  targetId: 31,
  organizationId: 3,
  organizationCode: '004',
  organizationName: 'KB국민은행',
  progressRate: 50,
  completedCount: 0,
  itemCount: 2,
  passed: 1,
  failed: 0,
  inProgress: 0,
  notStarted: 0,
  unreported: 1,
  awaitingReview: 1,
  completed: false,
  lastReportedAt: '2026-10-18T10:00:00',
  testDataStatus: 'CREATED',
  testDataStatusLabel: '생성 완료',
};

const reported = {
  itemId: 11,
  number: '001',
  title: '잔액 조회 전문 응답 확인',
  description: '가용잔액 항목이 채워지는지 확인합니다.',
  result: 'PASSED',
  resultLabel: '정상',
  note: null,
  reportedByName: '김민준',
  reportedAt: '2026-10-18T10:00:00',
  feedbackRate: null,
  feedbackNote: null,
  feedbackByName: null,
  feedbackAt: null,
  feedbackOutdated: false,
  effectiveRate: 100,
  completed: false,
  comments: [] as unknown[],
};

// 결제원 의견(80%)에 기관과 결제원이 답글을 하나씩 이어 단 요구사항.
const discussed = {
  ...reported,
  feedbackRate: 80,
  feedbackNote: '003 전문에 대해 예외사항 발생',
  feedbackByName: '홍성민',
  feedbackByJobTitle: '과장',
  feedbackAt: '2026-10-18T11:00:00',
  effectiveRate: 80,
  comments: [
    {
      id: 1,
      body: '사유코드 09 는 다음 주에 반영합니다.',
      authorKftc: false,
      authorName: '김민준',
      authorJobTitle: '대리',
      writtenAt: '2026-10-18T12:00:00',
      deletable: true,
    },
    {
      id: 2,
      body: '반영되면 다시 등록해 주세요.',
      authorKftc: true,
      authorName: '홍성민',
      authorJobTitle: '과장',
      writtenAt: '2026-10-18T13:00:00',
      deletable: false,
    },
  ],
};

const unreported = {
  itemId: 12,
  number: '002',
  title: '해지 요청 전문 처리 확인',
  description: null,
  result: null,
  resultLabel: null,
  note: null,
  reportedByName: null,
  reportedAt: null,
  feedbackRate: null,
  feedbackNote: null,
  feedbackByName: null,
  feedbackAt: null,
  feedbackOutdated: false,
  effectiveRate: 0,
  completed: false,
  comments: [],
};

type Entry = { itemId: number; result: string; note: string | null };

// vi.mock 은 파일 맨 위로 끌어올려지므로 mock 안에서 쓰는 값은 vi.hoisted 로 만든다.
const state = vi.hoisted(() => ({
  canReport: true,
  discussed: false,
  writes: [] as { path: string; body: unknown }[],
}));

vi.mock('../../../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../../api/client')>();
  const response = (rows: unknown[]) => ({
    planId: 5,
    summary,
    rows,
    canReport: state.canReport,
    canFeedback: !state.canReport,
    canComment: true,
  });
  const rows = () => [state.discussed ? discussed : reported, unreported];
  return {
    ...original,
    api: {
      GET: vi.fn(async (path: string) => {
        if (path === '/api/v1/test-plans/{id}/targets/{organizationId}') {
          return { data: response(rows()), response: new Response() };
        }
        throw new Error(`unexpected GET ${path}`);
      }),
      POST: vi.fn(async (path: string, options: { body: unknown }) => {
        state.writes.push({ path, body: options.body });
        return { data: response(rows()), response: new Response(null, { status: 201 }) };
      }),
      PUT: vi.fn(async (path: string, options: { body: { results?: Entry[] } }) => {
        state.writes.push({ path, body: options.body });
        const results = options.body.results ?? [];
        // 서버처럼 오류인데 특이사항이 없으면 그 행 경로에 오류를 붙인다.
        const missing = results.findIndex((e) => e.result === 'FAILED' && !e.note);
        if (missing >= 0) {
          return {
            error: {
              status: 400,
              code: 'TEST_RESULT_NOTE_REQUIRED',
              detail: '오류면 특이사항에 오류내용을 써 주세요.',
              fieldErrors: [
                {
                  field: `results[${missing}].note`,
                  message: '오류면 특이사항에 오류내용을 써 주세요.',
                },
              ],
            },
            response: new Response(null, { status: 400 }),
          };
        }
        return { data: response(rows()), response: new Response() };
      }),
    },
  };
});

beforeEach(() => {
  state.writes.length = 0;
  state.discussed = false;
});

function renderCard() {
  return renderWithProviders(<TargetResultsCard planId={5} organizationId={3} />, {
    route: '/account-info/tests/5',
    path: '/:business/tests/:id',
  });
}

describe('TargetResultsCard', () => {
  it('기관 담당자는 바뀐 요구사항만 저장하고, 오류인데 특이사항이 없으면 그 칸에 서버 메시지를 보인다', async () => {
    state.canReport = true;
    renderCard();

    const row = await screen.findByTestId('item-002');
    const save = screen.getByRole('button', { name: /결과 저장/ });
    expect(save).toBeDisabled();

    fireEvent.click(within(row).getByRole('radio', { name: '오류' }));
    expect(screen.getByRole('button', { name: '결과 저장 (1)' })).toBeEnabled();
    fireEvent.click(screen.getByRole('button', { name: '결과 저장 (1)' }));
    expect(
      await within(row).findByText('오류면 특이사항에 오류내용을 써 주세요.'),
    ).toBeInTheDocument();

    fireEvent.change(within(row).getByRole('textbox', { name: '002 특이사항' }), {
      target: { value: '해지 사유코드 09 미처리' },
    });
    fireEvent.click(screen.getByRole('button', { name: '결과 저장 (1)' }));
    await waitFor(() => expect(state.writes).toHaveLength(2));
    expect(state.writes[1]).toMatchObject({
      path: '/api/v1/test-plans/{id}/targets/{organizationId}/results',
      body: { results: [{ itemId: 12, result: 'FAILED', note: '해지 사유코드 09 미처리' }] },
    });
    expect(screen.queryByText('완료 확인')).not.toBeInTheDocument();
  });

  it('결제원 담당자는 결과를 보고 완료 확인으로 미니 수행률 100을 남긴다', async () => {
    state.canReport = false;
    renderCard();

    const row = await screen.findByTestId('item-001');
    expect(screen.queryByRole('button', { name: /결과 저장/ })).not.toBeInTheDocument();
    expect(screen.getByTestId('awaiting-review')).toHaveTextContent('확인할 결과 1');
    expect(within(screen.getByTestId('item-002')).getByText('미등록')).toBeInTheDocument();

    fireEvent.click(within(row).getByRole('button', { name: '완료 확인' }));
    await waitFor(() => expect(state.writes).toHaveLength(1));
    expect(state.writes[0]).toMatchObject({
      path: '/api/v1/test-plans/{id}/targets/{organizationId}/items/{itemId}/feedback',
      body: { rate: 100, note: null },
    });
  });

  it('결제원 의견 아래 답글이 쓴 사람 소속과 함께 이어지고, 기관 담당자가 답글을 단다', async () => {
    state.canReport = true;
    state.discussed = true;
    renderCard();

    const row = await screen.findByTestId('item-001');
    const comments = within(row).getAllByTestId('item-comment');
    expect(comments).toHaveLength(2);
    expect(comments[0]).toHaveTextContent('KB국민은행(김민준 대리)');
    expect(comments[1]).toHaveTextContent('결제원(홍성민 과장)');
    // 자기가 쓴 답글만 지우기가 보인다.
    expect(within(comments[0]).getByRole('button', { name: '지우기' })).toBeInTheDocument();
    expect(within(comments[1]).queryByRole('button', { name: '지우기' })).not.toBeInTheDocument();

    fireEvent.click(within(row).getByRole('button', { name: '001 답글' }));
    const input = within(row).getByRole('textbox', { name: '001 답글 입력' });
    expect(within(row).getByRole('button', { name: '답글 달기' })).toBeDisabled();
    fireEvent.change(input, { target: { value: ' 네, 반영 후 다시 등록하겠습니다. ' } });
    fireEvent.click(within(row).getByRole('button', { name: '답글 달기' }));
    await waitFor(() => expect(state.writes).toHaveLength(1));
    expect(state.writes[0]).toMatchObject({
      path: '/api/v1/test-plans/{id}/targets/{organizationId}/items/{itemId}/comments',
      body: { body: '네, 반영 후 다시 등록하겠습니다.' },
    });
  });

  it('의견도 답글도 없는 요구사항에는 답글 버튼이 없다', async () => {
    state.canReport = true;
    renderCard();
    const row = await screen.findByTestId('item-001');
    expect(within(row).queryByRole('button', { name: '001 답글' })).not.toBeInTheDocument();
  });
});
