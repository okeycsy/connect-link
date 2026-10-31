/**
 * 배경음(임시) 합성기. 외부 음원 없이 잔잔한 패드 + 가벼운 아르페지오를 만들어 out/bgm.wav 로 쓴다.
 * 정식 영상에서는 라이선스 있는 음원으로 바꿔 끼우면 된다(compose.ts --music=파일).
 *
 *   node e2e/demo-video/music.ts --seconds=160
 */
import { writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const RATE = 44100;

function arg(name: string, fallback: string): string {
  const found = process.argv.find((a) => a.startsWith(`--${name}=`));
  return found ? found.slice(name.length + 3) : fallback;
}

/** MIDI 번호 → 주파수. */
const hz = (midi: number) => 440 * Math.pow(2, (midi - 69) / 12);

// D 장조의 따뜻한 진행. 한 마디 6초, 네 마디가 한 바퀴.
const BAR = 6;
const CHORDS: number[][] = [
  [50, 57, 61, 64, 69], // Dmaj9  (D A C# E A)
  [47, 54, 57, 62, 66], // Bm7    (B F# A D F#)
  [43, 50, 54, 59, 66], // Gmaj9  (G D F# B F#)
  [45, 52, 57, 59, 64], // Asus/add9 (A E A B E)
];
// 아르페지오(가벼운 종소리 느낌)가 치는 음. 마디마다 코드 음을 위에서 고른다.
const ARP_STEP = 0.75;

function envPad(t: number, length: number) {
  const a = Math.min(1, t / 1.8);
  const r = Math.min(1, (length - t) / 1.8);
  return Math.max(0, Math.min(a, r));
}

function main() {
  const seconds = Number(arg('seconds', '160'));
  const n = Math.floor(seconds * RATE);
  const left = new Float32Array(n);
  const right = new Float32Array(n);

  const totalBars = Math.ceil(seconds / BAR);
  for (let bar = 0; bar < totalBars; bar++) {
    const chord = CHORDS[bar % CHORDS.length];
    const start = bar * BAR;
    const end = Math.min(seconds, start + BAR + 1.2); // 다음 마디와 살짝 겹친다.
    const s0 = Math.floor(start * RATE);
    const s1 = Math.floor(end * RATE);
    // 패드: 각 음에 배음 2개와 아주 느린 떨림, 좌우 살짝 다른 위상.
    chord.forEach((midi, i) => {
      const f = hz(midi);
      const detune = 1 + (i % 2 === 0 ? 0.0012 : -0.0012);
      const gain = (i === 0 ? 0.22 : 0.16) / chord.length;
      for (let s = s0; s < s1; s++) {
        const t = (s - s0) / RATE;
        const e = envPad(t, end - start);
        const w = 2 * Math.PI * t;
        const trem = 1 + 0.06 * Math.sin(w * 0.23 + i);
        const v =
          Math.sin(w * f) + 0.32 * Math.sin(w * f * 2 + 0.3) + 0.09 * Math.sin(w * f * 3 + 0.7);
        const vd = Math.sin(w * f * detune + 1.1) + 0.3 * Math.sin(w * f * detune * 2);
        left[s] += (v * 0.6 + vd * 0.4) * e * gain * trem;
        right[s] += (v * 0.4 + vd * 0.6) * e * gain * trem;
      }
    });
    // 아르페지오: 마디 안에서 0.75초마다 코드의 윗음을 하나씩 가볍게.
    const high = chord.slice(1).map((m) => m + 12);
    for (let k = 0; k * ARP_STEP < BAR; k++) {
      const at = start + k * ARP_STEP;
      if (at >= seconds) break;
      if (k % 4 === 3) continue; // 숨 쉬는 자리
      const midi = high[(k * 3) % high.length];
      const f = hz(midi);
      const a0 = Math.floor(at * RATE);
      const len = Math.floor(2.2 * RATE);
      const pan = 0.5 + 0.35 * Math.sin(k * 1.7);
      for (let s = a0; s < Math.min(n, a0 + len); s++) {
        const t = (s - a0) / RATE;
        const e = Math.exp(-t * 2.6) * Math.min(1, t / 0.012);
        const v = (Math.sin(2 * Math.PI * f * t) + 0.18 * Math.sin(2 * Math.PI * f * 2 * t)) * e * 0.085;
        left[s] += v * (1 - pan);
        right[s] += v * pan;
      }
    }
  }

  // 16비트 PCM WAV 로 쓴다.
  let peak = 0;
  for (let s = 0; s < n; s++) peak = Math.max(peak, Math.abs(left[s]), Math.abs(right[s]));
  const norm = peak > 0 ? 0.7 / peak : 1;
  const data = Buffer.alloc(n * 4);
  for (let s = 0; s < n; s++) {
    data.writeInt16LE(Math.round(Math.max(-1, Math.min(1, left[s] * norm)) * 32767), s * 4);
    data.writeInt16LE(Math.round(Math.max(-1, Math.min(1, right[s] * norm)) * 32767), s * 4 + 2);
  }
  const header = Buffer.alloc(44);
  header.write('RIFF', 0);
  header.writeUInt32LE(36 + data.length, 4);
  header.write('WAVE', 8);
  header.write('fmt ', 12);
  header.writeUInt32LE(16, 16);
  header.writeUInt16LE(1, 20);
  header.writeUInt16LE(2, 22);
  header.writeUInt32LE(RATE, 24);
  header.writeUInt32LE(RATE * 4, 28);
  header.writeUInt16LE(4, 32);
  header.writeUInt16LE(16, 34);
  header.write('data', 36);
  header.writeUInt32LE(data.length, 40);
  const out = path.join(here, 'out', 'bgm.wav');
  return writeFile(out, Buffer.concat([header, data])).then(() => console.log(`${seconds}초 → ${out}`));
}

main();
