/**
 * 타이틀 카드와 글꼴·로고를 내려 주는 작은 정적 서버. 녹화(record.ts)와 카드 미리보기(preview-cards.ts)가 같이 쓴다.
 *   /cards/{장면 id}  카드 HTML      /fonts/*  Pretendard OTF(프론트 npm 패키지)      /brand/*  앱 로고 자산
 */
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { cardHtml } from './cards.ts';
import { SCENES } from './scenes.ts';

const here = path.dirname(fileURLToPath(import.meta.url));

/** Pretendard OTF 는 프론트의 npm 패키지에서 읽는다(외부 URL 없음). */
function fontsDir(): string {
  const require = createRequire(path.join(here, '..', '..', 'frontend', 'package.json'));
  const pkg = require.resolve('pretendard/package.json');
  return path.join(path.dirname(pkg), 'dist', 'public', 'static');
}

/** 타이틀 카드와 글꼴을 내려 주는 작은 정적 서버. */
export async function startCardServer(): Promise<{ url: string; close: () => void }> {
  const fonts = fontsDir();
  const server = createServer(async (req, res) => {
    const url = new URL(req.url ?? '/', 'http://x');
    if (url.pathname.startsWith('/fonts/')) {
      try {
        const file = await readFile(path.join(fonts, path.basename(url.pathname)));
        res.writeHead(200, { 'content-type': 'font/otf', 'access-control-allow-origin': '*' });
        res.end(file);
      } catch {
        res.writeHead(404).end();
      }
      return;
    }
    if (url.pathname.startsWith('/brand/')) {
      try {
        const file = await readFile(path.join(here, '..', '..', 'frontend', 'src', 'assets', 'brand', path.basename(url.pathname)));
        res.writeHead(200, { 'content-type': 'image/png' });
        res.end(file);
      } catch {
        res.writeHead(404).end();
      }
      return;
    }
    if (url.pathname.startsWith('/cards/')) {
      const id = decodeURIComponent(url.pathname.slice('/cards/'.length));
      const scene = SCENES.find((s) => s.id === id);
      if (!scene || scene.kind !== 'card') {
        res.writeHead(404).end();
        return;
      }
      res.writeHead(200, { 'content-type': 'text/html; charset=utf-8' });
      res.end(cardHtml(scene.card));
      return;
    }
    res.writeHead(404).end();
  });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : 0;
  return { url: `http://127.0.0.1:${port}`, close: () => server.close() };
}

