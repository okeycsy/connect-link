/**
 * 녹화한 장면(out/segments/*.webm)을 ffmpeg 로 이어 붙여 out/demo.mp4 를 만든다.
 * 장면 사이는 0.4초 크로스페이드, 첫 장면은 페이드인, 마지막은 페이드아웃.
 *
 *   node e2e/demo-video/compose.ts                 # 무음
 *   node e2e/demo-video/compose.ts --music=bgm.m4a  # 배경음악을 깔고 끝에서 서서히 줄인다
 *   node e2e/demo-video/compose.ts --voice=narration.m4a  # 내레이션 음성을 입힌다
 *
 * ffmpeg 가 PATH 에 있어야 한다.
 */
import { spawn } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.join(here, 'out');
const SEGMENTS = path.join(OUT, 'segments');
const FADE = 0.4;
const FPS = 30;
/** 장면마다 녹화 머리에 남는 빈 화면(브라우저 문맥을 여는 시간, 초). 잘라 낸다. */
const HEAD_TRIM = 0.35;

type Entry = { id: string; file: string; kind: string; seconds: number };

function arg(name: string): string | undefined {
  const found = process.argv.find((a) => a.startsWith(`--${name}=`));
  return found?.slice(name.length + 3);
}

function run(cmd: string, args: string[]) {
  return new Promise<void>((resolve, reject) => {
    const child = spawn(cmd, args, { stdio: ['ignore', 'inherit', 'inherit'] });
    child.on('exit', (code) => (code === 0 ? resolve() : reject(new Error(`${cmd} 종료 코드 ${code}`))));
  });
}

async function probeDuration(file: string): Promise<number> {
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

async function main() {
  const timeline: Entry[] = JSON.parse(await readFile(path.join(OUT, 'timeline.json'), 'utf8'));
  if (timeline.length === 0) throw new Error('녹화된 장면이 없습니다. record.ts 를 먼저 실행하세요.');
  const music = arg('music');
  const voice = arg('voice');
  const output = arg('out') ?? path.join(OUT, 'demo.mp4');

  // 실제 webm 길이(녹화 시간과 조금 다르다)를 재서 크로스페이드 위치를 정한다.
  const durations: number[] = [];
  for (const entry of timeline) durations.push(Math.max(0.5, (await probeDuration(path.join(SEGMENTS, entry.file))) - HEAD_TRIM));

  const inputs: string[] = [];
  for (const entry of timeline) inputs.push('-i', path.join(SEGMENTS, entry.file));

  // 각 입력을 같은 fps·크기·포맷으로 맞추고, xfade 로 차례로 잇는다.
  const filters: string[] = [];
  timeline.forEach((_, i) => {
    filters.push(`[${i}:v]trim=start=${HEAD_TRIM},fps=${FPS},scale=1920:1080:flags=lanczos,format=yuv420p,setpts=PTS-STARTPTS[v${i}]`);
  });
  let last = 'v0';
  let offset = durations[0];
  for (let i = 1; i < timeline.length; i++) {
    const out = i === timeline.length - 1 ? 'vjoined' : `x${i}`;
    const at = Math.max(0, offset - FADE);
    filters.push(`[${last}][v${i}]xfade=transition=fade:duration=${FADE}:offset=${at.toFixed(3)}[${out}]`);
    last = out;
    offset = at + durations[i];
  }
  if (timeline.length === 1) {
    filters.push(`[v0]null[vjoined]`);
    offset = durations[0];
  }
  const total = offset;
  filters.push(`[vjoined]fade=t=in:st=0:d=0.6,fade=t=out:st=${(total - 0.8).toFixed(3)}:d=0.8[vout]`);

  const maps = ['-map', '[vout]'];
  let audioIndex = timeline.length;
  const audioParts: string[] = [];
  if (music) {
    inputs.push('-stream_loop', '-1', '-i', music);
    filters.push(
      `[${audioIndex}:a]atrim=0:${total.toFixed(3)},asetpts=PTS-STARTPTS,volume=${voice ? 0.18 : 0.35},afade=t=in:st=0:d=1.5,afade=t=out:st=${(total - 3).toFixed(3)}:d=3[am]`,
    );
    audioParts.push('[am]');
    audioIndex++;
  }
  if (voice) {
    inputs.push('-i', voice);
    filters.push(`[${audioIndex}:a]apad,atrim=0:${total.toFixed(3)},asetpts=PTS-STARTPTS[av]`);
    audioParts.push('[av]');
    audioIndex++;
  }
  if (audioParts.length === 1) {
    filters.push(`${audioParts[0]}anull[aout]`);
    maps.push('-map', '[aout]');
  } else if (audioParts.length === 2) {
    filters.push(`${audioParts.join('')}amix=inputs=2:duration=first:dropout_transition=0:normalize=0[aout]`);
    maps.push('-map', '[aout]');
  }

  const args = [
    '-y', '-hide_banner', '-loglevel', 'error', '-stats',
    ...inputs,
    '-filter_complex', filters.join(';'),
    ...maps,
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', '-pix_fmt', 'yuv420p', '-r', String(FPS),
    '-movflags', '+faststart',
  ];
  if (audioParts.length > 0) args.push('-c:a', 'aac', '-b:a', '192k');
  else args.push('-an');
  args.push(output);

  console.log(`장면 ${timeline.length}개, 총 ${total.toFixed(1)}초 → ${output}`);
  await run('ffmpeg', args);
  console.log('완료');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
