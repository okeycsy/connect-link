/**
 * 시연영상 녹화기. 장면(scenes/ 의 장별 파일)마다 새 브라우저 문맥을 열어 1920×1080 으로 녹화하고
 * out/segments/{번호}-{id}.webm 과 out/timeline.json 을 남긴다. 합치기는 compose.ts.
 *
 *   node e2e/demo-video/record.ts                       # 전체
 *   node e2e/demo-video/record.ts --only=suspensions    # 장(chapter) 하나만 다시 찍기
 *   node e2e/demo-video/record.ts --only=flow-branch,chat   # 장면 id 로도 된다
 * 일부만 다시 찍으면 나머지 조각과 timeline.json 은 그대로 두고 그 장면만 바꿔 끼운다.
 *
 * 앱은 app.jar 를 demo 프로필로 미리 띄워 둔다(기본 http://localhost:8080, DEMO_BASE_URL 로 바꾼다).
 * Chromium 경로는 PLAYWRIGHT_CHROMIUM_PATH 로 줄 수 있다.
 */
import { chromium } from '@playwright/test';
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { Director } from './helpers.ts';
import { OVERLAY_SCRIPT } from './overlay.ts';
import { SCENES, resolveSceneIds, type Scene } from './scenes/index.ts';
import { startCardServer } from './server.ts';
import type { Entry } from './timeline.ts';

const here = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.join(here, 'out');
const SEGMENTS = path.join(OUT, 'segments');
const BASE_URL = process.env.DEMO_BASE_URL ?? 'http://localhost:8080';
const WIDTH = 1920;
const HEIGHT = 1080;

async function recordScene(
  browser: Awaited<ReturnType<typeof chromium.launch>>,
  cardServer: string,
  index: number,
  scene: Scene,
) {
  const context = await browser.newContext({
    baseURL: BASE_URL,
    viewport: { width: WIDTH, height: HEIGHT },
    locale: 'ko-KR',
    timezoneId: 'Asia/Seoul',
    recordVideo: { dir: SEGMENTS, size: { width: WIDTH, height: HEIGHT } },
    reducedMotion: 'no-preference',
  });
  await context.addInitScript(OVERLAY_SCRIPT);
  const page = await context.newPage();
  const started = Date.now();
  const director = new Director(page);
  let failure: unknown = null;
  try {
    if (scene.kind === 'card') {
      await page.goto(`${cardServer}/cards/${encodeURIComponent(scene.id)}`);
      await page.evaluate(() => document.fonts.ready);
      await page.waitForTimeout(scene.duration);
    } else {
      await scene.run(director, page);
    }
  } catch (e) {
    failure = e;
  }
  // 실패해도 영상은 남긴다(어디서 멈췄는지 보려고). 그 뒤 실패를 알린다.
  const video = page.video();
  await context.close();
  const file = path.join(SEGMENTS, `${String(index).padStart(2, '0')}-${scene.id}.webm`);
  await video!.saveAs(file);
  await video!.delete();
  const seconds = (Date.now() - started) / 1000;
  if (failure) {
    console.error(`  ${scene.id.padEnd(16)} 실패 (${seconds.toFixed(1)}s) → ${file}`);
    throw failure;
  }
  console.log(`  ${scene.id.padEnd(16)} ${seconds.toFixed(1)}s`);
  // 내레이션 큐: 카드는 장면 머리에서 narration 한 줄, 앱 장면은 d.say() 가 남긴 시각과 문구.
  const cues = scene.kind === 'card' ? (scene.narration ? [{ at: 0, text: scene.narration }] : []) : director.cues;
  return { id: scene.id, file: path.basename(file), kind: scene.kind, seconds, cues };
}

async function main() {
  const onlyArg = process.argv.find((a) => a.startsWith('--only='));
  const only = onlyArg ? resolveSceneIds(onlyArg.slice('--only='.length).split(',').filter(Boolean)) : null;

  await mkdir(SEGMENTS, { recursive: true });
  if (!only) {
    await rm(SEGMENTS, { recursive: true, force: true });
    await mkdir(SEGMENTS, { recursive: true });
  }

  const cardServer = await startCardServer();
  const browser = await chromium.launch({
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_PATH,
    args: ['--font-render-hinting=none', '--disable-lcd-text', '--hide-scrollbars'],
  });

  const timelinePath = path.join(OUT, 'timeline.json');
  let timeline: Entry[] = [];
  if (only) {
    try {
      timeline = JSON.parse(await readFile(timelinePath, 'utf8'));
    } catch {
      timeline = [];
    }
  }

  console.log(`장면 ${only ? only.size : SCENES.length}개 녹화 시작 (${WIDTH}×${HEIGHT})`);
  try {
    for (const [index, scene] of SCENES.entries()) {
      if (only && !only.has(scene.id)) continue;
      const entry = await recordScene(browser, cardServer.url, index + 1, scene);
      const at = timeline.findIndex((t) => t.id === scene.id);
      if (at >= 0) timeline[at] = entry;
      else timeline.push(entry);
    }
  } finally {
    await browser.close();
    cardServer.close();
  }
  timeline.sort((a, b) => a.file.localeCompare(b.file));
  await writeFile(timelinePath, JSON.stringify(timeline, null, 2));
  const total = timeline.reduce((s, t) => s + t.seconds, 0);
  console.log(`총 ${total.toFixed(1)}초 (${timeline.length}장면) → ${timelinePath}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
