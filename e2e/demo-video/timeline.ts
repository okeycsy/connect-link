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

export type Entry = { id: string; file: string; kind: string; seconds: number };
export type Timed = Entry & { duration: number; start: number; end: number };

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

/** 합성본 기준 장면별 시작·끝(초). 장면 사이 겹침(FADE)을 반영한다. */
export async function timedEntries(entries: Entry[]): Promise<Timed[]> {
  const out: Timed[] = [];
  let t = 0;
  for (const entry of entries) {
    const duration = Math.max(0.5, (await probeDuration(path.join(SEGMENTS, entry.file))) - HEAD_TRIM);
    const start = t;
    const end = t + duration;
    out.push({ ...entry, duration, start, end });
    t = end - FADE;
  }
  return out;
}
