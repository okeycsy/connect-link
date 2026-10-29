/**
 * 녹화본(out/timeline.json + 조각 길이)과 장면 정의(scenes/)로 시나리오 문서와 내레이션 대본을 만든다.
 *
 *   node e2e/demo-video/docs.ts                 # out/scenario.md, out/narration.md, out/narration.srt
 *   node e2e/demo-video/docs.ts --intro=설명.md  # 장면표 앞에 붙일 글(기본: scenario-intro.md)
 *
 * 장면표의 "화면·동선"은 장면의 summary, "자막·설명"은 장면 코드의 d.caption()/note 문구, 시각은 합성본 기준이다.
 * 장면을 고치고 다시 찍으면 이 문서도 다시 만든다(run.sh 가 끝에 실행한다).
 */
import { readFile, readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

import { CHAPTERS, chapterOf, type Scene } from './scenes/index.ts';
import { HERE, OUT, fittedEntries, readTimeline, type Timed } from './timeline.ts';

/** 내레이션 읽는 속도(한글 음절/초). 차분한 TTS 기본 속도 근처다. 숫자·영문은 음절로 환산한다. */
const SYLLABLES_PER_SECOND = 4.8;
/** 문장 끝마다 쉬는 시간(초). */
const SENTENCE_PAUSE = 0.35;

/** 문구를 읽는 데 걸리는 시간 추정(초). */
export function speechSeconds(text: string): number {
  let syllables = 0;
  for (const ch of text) {
    const code = ch.codePointAt(0) ?? 0;
    if (code >= 0xac00 && code <= 0xd7a3) syllables += 1;
    else if (/[0-9]/.test(ch)) syllables += 1.4;
    else if (/[A-Za-z]/.test(ch)) syllables += 1.2;
  }
  const sentences = Math.max(1, (text.match(/[.!?]/g) ?? []).length);
  return syllables / SYLLABLES_PER_SECOND + sentences * SENTENCE_PAUSE;
}

function srtTime(seconds: number): string {
  const ms = Math.max(0, Math.round(seconds * 1000));
  const h = Math.floor(ms / 3_600_000);
  const m = Math.floor((ms % 3_600_000) / 60_000);
  const sec = Math.floor((ms % 60_000) / 1000);
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')},${String(ms % 1000).padStart(3, '0')}`;
}

function tc1(seconds: number): string {
  const whole = Math.floor(seconds);
  const tenth = Math.round((seconds - whole) * 10) % 10;
  return `${Math.floor(whole / 60)}:${String(whole % 60).padStart(2, '0')}.${tenth}`;
}

function arg(name: string): string | undefined {
  const found = process.argv.find((a) => a.startsWith(`--${name}=`));
  return found?.slice(name.length + 3);
}

function tc(seconds: number): string {
  const whole = Math.round(seconds);
  return `${Math.floor(whole / 60)}:${String(whole % 60).padStart(2, '0')}`;
}

function plain(html: string): string {
  return html.replace(/<b>/g, '**').replace(/<\/b>/g, '**').replace(/\\n/g, ' ');
}

/** 장면 코드에서 자막(d.caption)과 설명(note) 문구를 순서대로 뽑는다. */
async function captionsBySceneId(): Promise<Map<string, string[]>> {
  const dir = path.join(HERE, 'scenes');
  const files = (await readdir(dir)).filter((f) => /^\d\d-.*\.ts$/.test(f)).sort();
  const out = new Map<string, string[]>();
  const pattern = /(?:d\.caption\(|d\.note\(|note:\s*)(?:'((?:[^'\\]|\\.)*)'|`((?:[^`\\]|\\.)*)`)/g;
  for (const file of files) {
    const src = await readFile(path.join(dir, file), 'utf8');
    const starts = [...src.matchAll(/\n\s{4}\{\n\s{6}id: '([^']+)'/g)].map((m) => ({ id: m[1], at: m.index ?? 0 }));
    starts.forEach((s, i) => {
      const block = src.slice(s.at, i + 1 < starts.length ? starts[i + 1].at : undefined);
      const texts: string[] = [];
      for (const m of block.matchAll(pattern)) {
        const t = (m[1] ?? m[2] ?? '').trim();
        if (t && !texts.includes(t)) texts.push(t);
      }
      out.set(s.id, texts);
    });
  }
  return out;
}

function describeCard(scene: Extract<Scene, { kind: 'card' }>): string {
  const c = scene.card;
  const parts: string[] = [];
  if (c.image) parts.push('로고');
  if (c.kicker) parts.push(`머리글 "${c.kicker}"`);
  if (c.chapter) parts.push(`장 제목 **${c.chapter}**(크게)`);
  if (c.stat) parts.push(`숫자 **${c.stat.prefix ?? ''}${c.stat.value.toLocaleString('ko-KR')}${c.stat.unit ?? ''}**이 0에서 올라옴 · "${c.stat.label}"`);
  if (c.title) parts.push(`"${c.title.replace(/\n/g, ' ')}"`);
  if (c.subtitle) parts.push(`작은 글자 "${c.subtitle}"`);
  return `카드 · ${parts.join(' · ')}`;
}

async function main() {
  const timeline = await readTimeline();
  // compose.ts 와 같은 길이 맞춤(재생 속도)을 적용해 시각을 합성본과 맞춘다.
  const { timed, speed, rawTotal } = await fittedEntries(timeline);
  const byId = new Map(timed.map((t) => [t.id, t]));
  const scenes = CHAPTERS.flatMap((c) => c.scenes).filter((s) => byId.has(s.id));
  const total = timed.length ? timed[timed.length - 1].end : 0;
  const captions = await captionsBySceneId();
  const today = new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' });

  const introPath = arg('intro') ?? path.join(HERE, 'scenario-intro.md');
  let introText = '';
  try {
    introText = await readFile(introPath, 'utf8');
  } catch {
    introText = '';
  }

  const lines: string[] = [];
  lines.push('# 연결이 시연영상 — 시나리오 (녹화본 기준)', '');
  lines.push(`작성 ${today} · 영상 길이 **${tc(total)}** · 1920×1080 · 장 ${CHAPTERS.length}개 · 장면 ${scenes.length}개${speed > 1 ? ` · 녹화 ${tc(rawTotal)} 를 재생 ${speed.toFixed(3)}배로 맞춤` : ''}`, '');
  if (introText.trim()) lines.push(introText.trim(), '', '---', '');

  lines.push('## 장 배분', '');
  lines.push('| 장 | 시각 | 길이 | 장면 | 다시 찍기 |', '| --- | --- | --- | --- | --- |');
  for (const chapter of CHAPTERS) {
    const own = chapter.scenes.map((s) => byId.get(s.id)).filter((t): t is Timed => Boolean(t));
    if (!own.length) continue;
    const start = own[0].start;
    const end = own[own.length - 1].end;
    lines.push(`| ${chapter.title} | ${tc(start)}–${tc(end)} | ${Math.round(end - start)}초 | ${own.map((t) => t.id).join(', ')} | \`run.sh --only=${chapter.id}\` |`);
  }
  lines.push('');

  lines.push('## 장면표', '');
  lines.push(
    '표기: **카드** = 큰 글자 화면, **앱** = 실제 화면 녹화. 자막은 두 가지다. 장면 설명은 화면 아래 검은 띠, 강조 설명은 비춘 요소 바로 옆의 말풍선(나머지는 어둡게 가리고 잘리지 않는 만큼만 확대). 시각은 합성본 기준(장면 사이 0.4초 겹침 포함).',
    '',
  );
  lines.push('| # | 시각 | 장 | 장면 | 화면·동선 | 자막·설명 | 내레이션 |', '| --- | --- | --- | --- | --- | --- | --- |');
  scenes.forEach((scene, i) => {
    const t = byId.get(scene.id)!;
    const chapter = chapterOf(scene.id)?.title ?? '';
    const screen = scene.kind === 'card' ? describeCard(scene) : `앱 · ${scene.summary}`;
    const caps = scene.kind === 'card' ? '—' : (captions.get(scene.id) ?? []).map(plain).join(' / ') || '—';
    const spoken = (t.cues ?? []).map((c) => c.text).join(' ') || scene.narration || '—';
    lines.push(`| ${i + 1} | ${tc(t.start)}–${tc(t.end)} | ${chapter} | ${scene.id} | ${screen} | ${caps} | ${spoken} |`);
  });
  lines.push('');

  lines.push('## 연출 규칙', '');
  lines.push('- 한 화면에 한 문장. 카드는 흰 바탕에 파스텔 블롭, Pretendard Bold 84px, 글자가 아래에서 떠오른다. 검정 바탕은 쓰지 않는다.');
  lines.push('- 앱 장면은 실제 화면을 1920×1080으로 녹화한다. 커서는 천천히 움직인다.');
  lines.push('- 강조는 "자막이 가리키는 요소"만 비춘다: 그 요소 둘레만 밝게 남기고 나머지를 어둡게 가린 뒤, 화면에 다 들어오는 배율까지만 확대하고, 설명 말풍선을 요소 바로 옆에 붙인다. 요소가 자라거나 움직이면(섹션이 나타나는 회신 화면 등) 따라간다.');
  lines.push('- 머리 막대 오른쪽에 누가 로그인했는지 알약으로 띄운다: 결제원 담당자는 갈색, 참가기관 담당자는 파랑. 시점이 바뀔 때마다 알약이 바뀐다.');
  lines.push('- 장면 전환은 0.4초 크로스페이드. 첫 장면 페이드인, 끝 페이드아웃. 색은 앱의 디자인 시스템만, 외부 이미지·폰트 없음.');
  lines.push('');
  lines.push('## 고치고 다시 찍는 법', '');
  lines.push('- 장면은 `e2e/demo-video/scenes/` 에 장(chapter)별 파일로 있다(01-intro, 02-flow, 03-tests, 04-suspensions, 05-chat, 06-outro). 문구·동선을 고치면 그 장만 `e2e/demo-video/run.sh --only=<장 id>` 로 다시 찍고 이어 붙인다(1~2분).');
  lines.push('- 장 하나만 따로 짧은 영상으로 뽑으려면 `node e2e/demo-video/compose.ts --chapter=<장 id>` → `out/demo-<장 id>.mp4`.');
  lines.push('- 카드 문구만 고쳤으면 `node e2e/demo-video/preview-cards.ts` 로 먼저 보고 찍는다. 이 문서는 `node e2e/demo-video/docs.ts` 로 다시 만든다.');
  lines.push('');
  await writeFile(path.join(OUT, 'scenario.md'), lines.join('\n'));

  // 내레이션 큐: 합성본 기준 시작 시각, 다음 큐까지의 여유, 예상 읽기 시간. TTS 조각을 이 시각에 맞춰 붙인다.
  type Line = { at: number; text: string; sceneId: string };
  const cueLines: Line[] = [];
  for (const scene of scenes) {
    const t = byId.get(scene.id)!;
    const cues = t.cues?.length ? t.cues : scene.narration ? [{ at: 0, text: scene.narration }] : [];
    for (const c of cues) cueLines.push({ at: Math.max(t.start, t.start + c.at - 0.1), text: c.text, sceneId: scene.id });
  }
  cueLines.sort((a, b) => a.at - b.at);
  const rows = cueLines.map((line, i) => {
    const next = i + 1 < cueLines.length ? cueLines[i + 1].at : total;
    const room = next - line.at;
    const need = speechSeconds(line.text);
    return { ...line, room, need, tight: need > room + 0.2 };
  });
  const tightCount = rows.filter((r) => r.tight).length;

  const n: string[] = [
    '# 연결이 시연영상 내레이션 대본 (녹화본 기준)',
    '',
    `영상 길이 ${tc(total)} · 큐 ${rows.length}개${tightCount ? ` · 빠듯한 큐 ${tightCount}개(⚠)` : ''}${speed > 1 ? ` · 녹화 ${tc(rawTotal)} 를 재생 ${speed.toFixed(3)}배로 맞춤(시각은 맞춘 뒤 기준)` : ''}.`,
    '',
    `- **시작**은 합성본에서 그 문구를 읽기 시작할 시각이다. TTS 조각을 큐마다 만들어 그 시각에 놓거나, 같은 내용의 \`narration.srt\` 를 TTS·편집 도구에 넣는다.`,
    `- **여유**는 다음 큐까지 남은 시간, **예상**은 한글 ${SYLLABLES_PER_SECOND}음절/초 + 문장마다 ${SENTENCE_PAUSE}초로 어림한 읽기 시간이다. ⚠ 는 예상이 여유보다 긴 큐라 TTS 속도를 조금 올리거나 문구를 줄인다.`,
    `- 음성 파일 하나로 만들었으면 \`compose.ts --voice=<파일>\` 로 입힌다.`,
    '',
    '| # | 시작 | 여유 | 예상 | 장면 | 내레이션 |',
    '| --- | --- | --- | --- | --- | --- |',
  ];
  rows.forEach((r, i) => {
    n.push(`| ${i + 1} | ${tc1(r.at)} | ${r.room.toFixed(1)}초 | ${r.need.toFixed(1)}초${r.tight ? ' ⚠' : ''} | ${r.sceneId} | ${r.text} |`);
  });
  n.push('', '## 이어서 읽는 전체 대본', '');
  for (const r of rows) n.push(`[${tc1(r.at)}] ${r.text}`, '');
  await writeFile(path.join(OUT, 'narration.md'), n.join('\n'));

  const srt: string[] = [];
  rows.forEach((r, i) => {
    const end = Math.min(r.at + r.need, i + 1 < rows.length ? rows[i + 1].at - 0.05 : total);
    srt.push(String(i + 1), `${srtTime(r.at)} --> ${srtTime(Math.max(end, r.at + 0.5))}`, r.text, '');
  });
  await writeFile(path.join(OUT, 'narration.srt'), srt.join('\n'));
  await writeFile(path.join(OUT, 'narration-cues.json'), JSON.stringify(rows.map((r) => ({ at: r.at, scene: r.sceneId, text: r.text, room: Number(r.room.toFixed(2)), need: Number(r.need.toFixed(2)) })), null, 2));
  console.log(`문서 작성: ${path.join(OUT, 'scenario.md')}, ${path.join(OUT, 'narration.md')}, narration.srt (${tc(total)}${tightCount ? `, 빠듯한 큐 ${tightCount}개` : ''})`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
