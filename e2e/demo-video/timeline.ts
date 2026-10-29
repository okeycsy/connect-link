/**
 * 녹화 조각(out/segments/*.webm)과 timeline.json 을 읽어, 합성본에서 장면이 몇 초에 시작하고 끝나는지 계산한다.
 * compose.ts(합성)와 docs.ts(시나리오 문서)가 같은 숫자를 쓰도록 여기 한 곳에 둔다.
 */
import { spawn } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const HERE = path.dirname(fileURLToPath(import.meta.url));
export const OUT = path.join(HERE, 'out');
export const SEGMENTS = path.join(OUT, 'segments');
/** 장면 사이 크로스페이드(초). */
export const FADE = 0.4;
/** 장면마다 녹화 머리에 남는 빈 화면(브라우저 문맥을 여는 시간, 초). 잘라 낸다. */
export const HEAD_TRIM = 0.35;
/**
 * 합성본 목표 길이(초). 녹화 길이는 기계 속도에 따라 한 번에 몇 초씩 달라지므로, 이보다 길면 재생 속도를 조금 올려 맞춘다.
 * 심사 제한 3분, SHY 요청 2분 55초.
 */
export const TARGET_SECONDS = 175;
/** 길이를 맞출 때 올릴 수 있는 재생 속도 상한. 이 안에서는 커서·타자 속도 차이가 눈에 띄지 않는다. 넘으면 장면을 줄여야 한다. */
export const MAX_SPEED = 1.08;

/** 내레이션 큐: 장면 머리(녹화 시작)에서 몇 초 뒤에 읽기 시작하는 문구. */
export type Cue = { at: number; text: string };
export type Entry = { id: string; file: string; kind: string; seconds: number; cues?: Cue[] };
export type Timed = Entry & { duration: number; start: number; end: number };
/** 길이 맞춤 결과. speed 는 재생 속도(1 이면 그대로), rawTotal 은 맞추기 전 길이. */
export type Fitted = { timed: Timed[]; speed: number; rawTotal: number; total: number };

export async function readTimeline(): Promise<Entry[]> {
  return JSON.parse(await readFile(path.join(OUT, 'timeline.json'), 'utf8'));
}

/** webm 의 실제 길이(초). 녹화 시간과 조금 다르다. */
export function probeDuration(file: string): Promise<number> {
  return new Promise((resolve, reject) => {
    const child = spawn('ffprobe', [
      '-v', 'error', '-select_streams', 'v:0', '-count_packets',
      '-show_entries', 'stream=nb_read_packets,r_frame_rate', '-of', 'json', file,
    ]);
    let out = '';
    child.stdout.on('data', (d) => (out += d));
    child.on('exit', (code) => {
      if (code !== 0) return reject(new Error('ffprobe 실패: ' + file));
      const s = JSON.parse(out).streams[0];
      const [n, d] = String(s.r_frame_rate).split('/').map(Number);
      resolve(Number(s.nb_read_packets) / (n / d));
    });
  });
}

async function probeAll(entries: Entry[]): Promise<number[]> {
  const out: number[] = [];
  for (const entry of entries) out.push(await probeDuration(path.join(SEGMENTS, entry.file)));
  return out;
}

/** 잰 길이로 합성본 기준 시작·끝을 놓는다. speed 가 1보다 크면 그만큼 빨리 재생하는 셈이라 길이와 큐 시각을 나눈다. */
function layout(entries: Entry[], probed: number[], speed: number): Timed[] {
  const out: Timed[] = [];
  let t = 0;
  entries.forEach((entry, i) => {
    const duration = Math.max(0.5, probed[i] - HEAD_TRIM) / speed;
    const start = t;
    const end = t + duration;
    const cues = entry.cues?.map((c) => ({ ...c, at: c.at / speed }));
    out.push({ ...entry, cues, duration, start, end });
    t = end - FADE;
  });
  return out;
}

/** 합성본 기준 장면별 시작·끝(초). 장면 사이 겹침(FADE)을 반영한다. */
export async function timedEntries(entries: Entry[]): Promise<Timed[]> {
  return layout(entries, await probeAll(entries), 1);
}

/** 목표 길이보다 길면 재생 속도를 올려(MAX_SPEED 까지) 맞춘 결과. compose(합성)와 docs(문서)가 같은 숫자를 쓴다. */
export function fitSpeed(rawTotal: number, target: number): number {
  if (!(rawTotal > target)) return 1;
  return Math.min(MAX_SPEED, rawTotal / target);
}

export async function fittedEntries(entries: Entry[], target = TARGET_SECONDS): Promise<Fitted> {
  const probed = await probeAll(entries);
  const raw = layout(entries, probed, 1);
  const rawTotal = raw.length ? raw[raw.length - 1].end : 0;
  const speed = fitSpeed(rawTotal, target);
  const timed = speed === 1 ? raw : layout(entries, probed, speed);
  const total = timed.length ? timed[timed.length - 1].end : 0;
  return { timed, speed, rawTotal, total };
}
