import { fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { TestDataCard } from './TestDataCard';

// vi.mock 은 파일 맨 위로 끌어올려지므로 mock 안에서 쓰는 값은 vi.hoisted 로 만든다.
const state = vi.hoisted(() => ({
  canEdit: true,
  writes: [] as { path: string; body: unknown }[],
}));

vi.mock('../../../api/client', async (importOriginal) => {
  const original = await importOriginal<typeof import('../../../api/client')>();
  const response = (status: string, note: string | null) => ({
    planId: 5,
    targetId: 31,
    organizationId: 3,
    organizationName: 'KB국민은행',
    status,
    statusLabel: status === 'CREATED' ? '생성 완료' : '미생성',
    note,
    fileCount: 0,
    reportedByName: note ? '김민준' : null,
    reportedAt: note ? '2026-10-18T10:00:00' : null,
    canEdit: state.canEdit,
  });
  return {
    ...original,
    api: {
      GET: vi.fn(async (path: string, options: { params: { query?: { ownerType?: string } } }) => {
        if (path === '/api/v1/test-plans/{id}/targets/{organizationId}/test-data') {
          return {
            data: state.canEdit
              ? response('NOT_CREATED', null)
              : response('CREATED', '테스트 계좌 10개'),
            response: new Response(),
          };
        }
        if (path === '/api/v1/attachments') {
          const form = options.params.query?.ownerType === 'TEST_DATA_FORM';
          return {
            data: form
              ? [
                  {
                    id: 1,
                    ownerType: 'TEST_DATA_FORM',
                    ownerId: 5,
                    name: '테스트데이터 양식.xlsx',
                    contentType: 'application/octet-stream',
                    size: 2048,
                    createdBy: '홍성민',
                    createdAt: '2026-10-17T09:00:00',
                  },
                ]
              : [],
            response: new Response(),
          };
        }
        throw new Error(`unexpected GET ${path}`);
      }),
      PUT: vi.fn(
        async (path: string, options: { body: { status: string; note: string | null } }) => {
          state.writes.push({ path, body: options.body });
          return {
            data: response(options.body.status, options.body.note),
            response: new Response(),
          };
        },
      ),
    },
  };
});

beforeEach(() => {
  state.writes.length = 0;
});

function renderCard() {
  const request = {
    dueDate: '2026-10-25',
    instruction: '테스트 계좌 10개 이상',
    requestedAt: '2026-10-20T09:00:00',
    overdue: false,
    createdCount: 0,
    targetCount: 1,
  };
  return renderWithProviders(<TestDataCard planId={5} organizationId={3} request={request} />, {
    route: '/account-info/tests/5',
    path: '/:business/tests/:id',
  });
}

describe('TestDataCard', () => {
  it('기관 담당자는 결제원 양식을 보고 생성 여부와 메모를 저장한다', async () => {
    state.canEdit = true;
    renderCard();

    expect(await screen.findByText('테스트데이터 양식.xlsx')).toBeInTheDocument();
    expect(screen.getByTestId('test-data-due')).toHaveTextContent('2026-10-25');
    expect(screen.getByText('테스트 계좌 10개 이상')).toBeInTheDocument();
    const save = screen.getByRole('button', { name: '테스트데이터 저장' });
    expect(save).toBeDisabled();

    fireEvent.click(screen.getByText('생성 완료'));
    fireEvent.change(screen.getByRole('textbox', { name: '메모' }), {
      target: { value: '  테스트 계좌 10개  ' },
    });
    fireEvent.click(save);
    await waitFor(() => expect(state.writes).toHaveLength(1));
    expect(state.writes[0]).toMatchObject({
      path: '/api/v1/test-plans/{id}/targets/{organizationId}/test-data',
      body: { status: 'CREATED', note: '테스트 계좌 10개' },
    });
    expect(await screen.findByRole('button', { name: '테스트데이터 올리기' })).toBeInTheDocument();
  });

  it('결제원 담당자는 기관의 생성 여부와 메모를 읽기만 한다', async () => {
    state.canEdit = false;
    renderCard();

    expect(await screen.findByText('테스트 계좌 10개')).toBeInTheDocument();
    expect(screen.getByTestId('test-data')).toHaveTextContent('생성 완료');
    expect(screen.queryByRole('button', { name: '테스트데이터 저장' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '테스트데이터 올리기' })).not.toBeInTheDocument();
  });
});
