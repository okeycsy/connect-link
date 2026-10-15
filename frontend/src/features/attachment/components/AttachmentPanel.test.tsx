import { fireEvent, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

import { renderWithProviders } from '../../../test/render';
import { AttachmentPanel } from './AttachmentPanel';

const state = vi.hoisted(() => ({
  files: [
    {
      id: 11,
      ownerType: 'DEMO',
      ownerId: 1,
      name: '공문.pdf',
      contentType: 'application/pdf',
      size: 2048,
      uploadedBy: 'admin@kftc.example.com',
      uploadedAt: '2026-10-15T10:00:00',
    },
  ],
  post: vi.fn(),
  del: vi.fn(),
}));

vi.mock('../../../api/client', () => ({
  api: {
    GET: vi.fn(async (path: string) => {
      if (path !== '/api/v1/attachments') throw new Error(`unexpected GET ${path}`);
      return { data: state.files, response: new Response() };
    }),
    POST: vi.fn(async (...args: unknown[]) => {
      state.post(...args);
      const added = { ...state.files[0], id: 12, name: '근거자료.xlsx' };
      state.files = [...state.files, added];
      return { data: added, response: new Response() };
    }),
    DELETE: vi.fn(async (...args: unknown[]) => {
      state.del(...args);
      state.files = state.files.filter((f) => f.id !== 11);
      return { data: undefined, response: new Response(null, { status: 204 }) };
    }),
  },
  unwrap: (r: { data: unknown }) => r.data,
  ApiError: class extends Error {},
}));

describe('AttachmentPanel', () => {
  it('목록을 보여 주고, 파일을 고르면 올리고, 지우기는 확인 뒤에 부른다', async () => {
    renderWithProviders(<AttachmentPanel ownerType="DEMO" ownerId={1} editable />);

    const link = await screen.findByRole('link', { name: '공문.pdf' });
    expect(link).toHaveAttribute('href', '/api/v1/attachments/11/download');
    expect(
      screen.getByText(/2.0 KB · admin@kftc.example.com · 2026-10-15 10:00/),
    ).toBeInTheDocument();

    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['x'], '근거자료.xlsx', { type: 'application/vnd.ms-excel' });
    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => expect(state.post).toHaveBeenCalled());
    const [path, options] = state.post.mock.calls[0] as [string, { params: { query: unknown } }];
    expect(path).toBe('/api/v1/attachments');
    expect(options.params.query).toEqual({ ownerType: 'DEMO', ownerId: 1 });
    expect(await screen.findByRole('link', { name: '근거자료.xlsx' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '공문.pdf 지우기' }));
    fireEvent.click(await screen.findByRole('button', { name: '지우기' }));
    await waitFor(() =>
      expect(state.del).toHaveBeenCalledWith('/api/v1/attachments/{id}', {
        params: { path: { id: 11 } },
      }),
    );
    await waitFor(() =>
      expect(screen.queryByRole('link', { name: '공문.pdf' })).not.toBeInTheDocument(),
    );
  });

  it('읽기 전용이면 올리기·지우기 버튼이 없다', async () => {
    renderWithProviders(<AttachmentPanel ownerType="DEMO" ownerId={1} />);
    await screen.findByRole('list', { name: '첨부파일' });
    expect(screen.queryByRole('button', { name: '파일 올리기' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /지우기/ })).not.toBeInTheDocument();
  });
});
