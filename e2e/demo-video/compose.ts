/**
 * 녹화한 장면(out/segments/*.webm)을 ffmpeg 로 이어 붙여 out/demo.mp4 를 만든다.
 * 장면 사이는 0.4초 크로스페이드, 첫 장면은 페이드인, 마지막은 페이드아웃.
 *
 *   node e2e/demo-video/compose.ts                      # 무음
 *   node e2e/demo-video/compose.ts --music=bgm.m4a       # 배경음악을 깔고 끝에서 서서히 줄인다
 *   node e2e/demo-video/compose.ts --voice=narration.m4a # 내레이션 음성을 입힌다
 *   node e2e/demo-video/compose.ts --chapter=suspensions # 그 장만 따로 out/demo-suspensions.mp4 로(기능별 짧은 영상)
 *   node e2e/demo-video/compose.ts --target=175          # 이 길이(초)보다 길면 재생 속도를 조금 올려 맞춘다(기본 TARGET_SECONDS, 상한 MAX_SPEED)
 *
 * ffmpeg 가 PATH 에 있어야 한다.
 */
import path from 'node:path';
import { spawn } from 'node:child_process';

import { CHAPTERS } from './scenes/index.ts';
import { FADE, HEAD_TRIM, MAX_SPEED, OUT, SEGMENTS, TARGET_SECONDS, fittedEntries, readTimeline } from './timeline.ts';

const FPS = 30;

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

async function main() {
  let timeline = await readTimeline();
  const chapter = arg('chapter');
  if (chapter) {
    const found = CHAPTERS.find((c) => c.id === chapter);
    if (!found) throw new Error(`모르는 장 id: ${chapter} (${CHAPTERS.map((c) => c.id).join(', ')})`);
    const ids = new Set(found.scenes.map((s) => s.id));
    timeline = timeline.filter((t) => ids.has(t.id));
  }
  if (timeline.length === 0) throw new Error('녹화된 장면이 없습니다. record.ts 를 먼저 실행하세요.');
  const music = arg('music');
  const voice = arg('voice');
  const output = arg('out') ?? path.join(OUT, chapter ? `demo-${chapter}.mp4` : 'demo.mp4');

  // 실제 webm 길이(녹화 시간과 조금 다르다)를 재서 크로스페이드 위치를 정한다. 장 하나만 뽑을 때는 길이를 맞추지 않는다.
  const target = chapter ? Infinity : Number(arg('target') ?? TARGET_SECONDS);
  const { timed, speed, rawTotal } = await fittedEntries(timeline, target);
  if (speed > 1) {
    console.log(`길이 맞춤: 녹화 ${rawTotal.toFixed(1)}초 → 목표 ${target}초, 재생 ${speed.toFixed(3)}배`);
    if (rawTotal / target > MAX_SPEED) console.warn(`경고: 상한 ${MAX_SPEED}배로도 목표에 못 미칩니다. 장면을 줄이세요.`);
  }
  const durations = timed.map((t) => t.duration);

  const inputs: string[] = [];
  for (const entry of timeline) inputs.push('-i', path.join(SEGMENTS, entry.file));

  // 각 입력을 같은 fps·크기·포맷으로 맞추고, xfade 로 차례로 잇는다.
  const filters: string[] = [];
  timeline.forEach((_, i) => {
    filters.push(`[${i}:v]trim=start=${HEAD_TRIM},setpts=(PTS-STARTPTS)/${speed.toFixed(4)},fps=${FPS},scale=1920:1080:flags=lanczos,format=yuv420p[v${i}]`);
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
