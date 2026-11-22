/**
 * 타이틀 카드만 빠르게 확인한다(녹화 없이). 카드마다 글자가 다 떠오른 뒤의 모습을 out/cards/{id}.png 로 찍는다.
 *   node e2e/demo-video/preview-cards.ts
 */
import { chromium } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { SCENES } from './scenes/index.ts';
import { startCardServer } from './server.ts';

const here = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.join(here, 'out', 'cards');

async function main() {
  await mkdir(OUT, { recursive: true });
  const server = await startCardServer();
  const browser = await chromium.launch({ executablePath: process.env.PLAYWRIGHT_CHROMIUM_PATH });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  try {
    for (const scene of SCENES) {
      if (scene.kind !== 'card') continue;
      await page.goto(`${server.url}/cards/${encodeURIComponent(scene.id)}`);
      await page.evaluate(() => document.fonts.ready);
      await page.waitForTimeout(2600);
      const file = path.join(OUT, `${scene.id}.png`);
      await page.screenshot({ path: file });
      console.log(`  ${scene.id.padEnd(14)} → ${file}`);
    }
  } finally {
    await browser.close();
    server.close();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
