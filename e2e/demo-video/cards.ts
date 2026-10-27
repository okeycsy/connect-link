/**
 * 타이틀 카드(장면 사이의 큰 글자 화면) HTML. 녹화 서버가 /cards/{id} 로 내려 주고 브라우저가 그대로 녹화한다.
 * 글꼴은 앱과 같은 Pretendard 를 /fonts/ 에서, 로고는 앱 자산을 /brand/ 에서 읽는다(외부 URL 없음).
 *
 * 애플 키노트 문법: 한 화면에 한 문장, 여백, 큰 글자, 짧게 떠올랐다가 사라진다.
 */
export type CardSpec = {
  /** 'light' 는 흰 바탕(앱과 이어질 때), 'dark' 는 검은 바탕(문제 제기·마무리). */
  tone?: 'light' | 'dark';
  /** 작은 머리글(예: "01  담당자 연락처"). */
  kicker?: string;
  /** 큰 제목. `\n` 으로 줄을 바꾼다. 숫자 카드(stat)면 비워도 된다. */
  title?: string;
  /** 제목 아래 설명. */
  subtitle?: string;
  /** 제목 대신 커다란 숫자 하나를 보일 때. value 가 숫자면 0 에서 올라오고 천 단위 쉼표를 붙인다. */
  stat?: { value: number | string; unit?: string; label: string };
  /** 제목 위에 보일 그림(앱 로고). 경로는 /brand/… */
  image?: { src: string; width: number };
  /** 글자가 나타나는 시차(ms). */
  stagger?: number;
};

const esc = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const lines = (s: string, from: number) =>
  esc(s)
    .split('\n')
    .map((l, i) => `<span class="line" style="animation-delay:${from + i * 120}ms">${l}</span>`)
    .join('');

export function cardHtml(spec: CardSpec): string {
  const dark = spec.tone === 'dark';
  const stagger = spec.stagger ?? 160;
  let step = 0;
  const next = () => stagger * step++;
  const parts: string[] = [];
  if (spec.image) {
    parts.push(
      `<img class="rise" src="${esc(spec.image.src)}" width="${spec.image.width}" style="animation-delay:${next()}ms" alt="">`,
    );
  }
  if (spec.kicker) parts.push(`<div class="kicker rise" style="animation-delay:${next()}ms">${esc(spec.kicker)}</div>`);
  if (spec.stat) {
    const numeric = typeof spec.stat.value === 'number';
    const shown = numeric ? (spec.stat.value as number).toLocaleString('ko-KR') : String(spec.stat.value);
    parts.push(
      `<div class="stat rise" style="animation-delay:${next()}ms"><span class="value"${numeric ? ` data-count="${spec.stat.value}"` : ''}>${esc(shown)}</span>${spec.stat.unit ? `<span class="unit">${esc(spec.stat.unit)}</span>` : ''}</div>`,
      `<div class="stat-label rise" style="animation-delay:${next()}ms">${esc(spec.stat.label)}</div>`,
    );
  }
  if (spec.title) parts.push(`<h1 class="title">${lines(spec.title, next())}</h1>`);
  if (spec.subtitle) parts.push(`<div class="subtitle rise" style="animation-delay:${next()}ms">${esc(spec.subtitle)}</div>`);

  return `<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><title>card</title>
<style>
@font-face{font-family:"Pretendard";font-weight:400;src:url(/fonts/Pretendard-Regular.otf) format("opentype")}
@font-face{font-family:"Pretendard";font-weight:500;src:url(/fonts/Pretendard-Medium.otf) format("opentype")}
@font-face{font-family:"Pretendard";font-weight:600;src:url(/fonts/Pretendard-SemiBold.otf) format("opentype")}
@font-face{font-family:"Pretendard";font-weight:700;src:url(/fonts/Pretendard-Bold.otf) format("opentype")}
@font-face{font-family:"Pretendard";font-weight:800;src:url(/fonts/Pretendard-ExtraBold.otf) format("opentype")}
html,body{margin:0;height:100%;overflow:hidden}
body{display:flex;align-items:center;justify-content:center;font-family:"Pretendard",-apple-system,sans-serif;
  background:${dark ? '#121010' : '#FFFFFF'};color:${dark ? '#FFFFFF' : '#1D1715'};letter-spacing:-.02em;word-break:keep-all}
.blob{position:absolute;border-radius:50%;filter:blur(90px);opacity:.75;pointer-events:none}
.b1{width:720px;height:720px;left:-220px;top:-260px;background:#F6D6CC}
.b2{width:640px;height:640px;right:-200px;bottom:-240px;background:#CFE0F3}
.b3{width:420px;height:420px;right:18%;top:-200px;background:#F7EFB8}
.wrap{position:relative;text-align:center;max-width:1500px;padding:0 60px;display:flex;flex-direction:column;align-items:center}
img{display:block;margin:0 auto 44px}
.kicker{font-size:28px;font-weight:600;color:${dark ? '#F6D6CC' : '#8A5A50'};margin-bottom:30px;letter-spacing:.04em}
.title{font-size:84px;font-weight:700;line-height:1.24;margin:0;letter-spacing:-.03em}
.title .line{display:block}
.subtitle{font-size:34px;font-weight:400;color:${dark ? 'rgba(255,255,255,.72)' : '#62656D'};margin-top:34px;line-height:1.45;letter-spacing:-.01em}
.stat{display:flex;align-items:baseline;justify-content:center;gap:10px}
.stat .value{font-size:210px;font-weight:800;line-height:1;letter-spacing:-.05em;font-variant-numeric:tabular-nums;color:${dark ? '#fff' : '#8A5A50'}}
.stat .unit{font-size:88px;font-weight:700}
.stat-label{font-size:38px;font-weight:500;color:${dark ? 'rgba(255,255,255,.72)' : '#3E4048'};margin-top:22px;letter-spacing:-.01em}
.rise,.line{opacity:0;transform:translateY(28px);animation:rise 820ms cubic-bezier(.2,.8,.2,1) forwards}
@keyframes rise{to{opacity:1;transform:translateY(0)}}
</style></head>
<body>
${dark ? '' : '<div class="blob b1"></div><div class="blob b2"></div><div class="blob b3"></div>'}
<div class="wrap">
${parts.join('\n')}
</div>
<script>
// 숫자는 0 에서 올라온다.
const v = document.querySelector('[data-count]');
if (v) {
  const target = Number(v.dataset.count); const dur = 1400; let t0 = null;
  const tick = (t) => { if (t0 === null) t0 = t; const p = Math.min(1, (t - t0) / dur); const e = 1 - Math.pow(1 - p, 3);
    v.textContent = Math.round(target * e).toLocaleString('ko-KR'); if (p < 1) requestAnimationFrame(tick); };
  setTimeout(() => requestAnimationFrame(tick), ${stagger + 150});
}
</script>
</body></html>`;
}
