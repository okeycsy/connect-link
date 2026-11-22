/**
 * 녹화 중인 화면에 끼워 넣는 연출 요소. 페이지가 열릴 때마다(addInitScript) 한 번 실행되어
 * 가짜 커서, 자막 띠, 로그인 주체 알약, 스포트라이트(강조) 도우미를 `window.__demo` 로 노출한다.
 *
 * 앱 코드는 전혀 건드리지 않고 브라우저 안에서만 덧그린다. 실제 마우스 이동(Playwright mouse)을 따라
 * 커서가 움직이고, 클릭하면 물결 효과가 남는다.
 *
 * 강조(focus)는 세 가지를 한 번에 한다.
 *  1. 자막이 가리키는 요소(`data-demo-focus` 표시) 둘레만 밝게 남기고 나머지를 어둡게 가린다(스포트라이트).
 *  2. 요소가 화면 밖으로 잘리지 않는 배율까지만 앱(#root)을 확대한다.
 *  3. 설명(note)을 그 요소 바로 아래(자리가 없으면 위)에 붙인다. 아래쪽 자막 띠는 숨긴다.
 * 요소가 자라거나(섹션이 나타남) 움직여도 스포트라이트와 설명이 따라간다.
 */
export const OVERLAY_SCRIPT = String.raw`
(() => {
  if (window.__demo) return;
  const CURSOR_SVG = '<svg width="28" height="36" viewBox="0 0 28 36" xmlns="http://www.w3.org/2000/svg">'
    + '<path d="M3 2 L3 27 L9.5 21.5 L13.5 31 L18 29 L14 19.5 L23 19.5 Z" fill="#111" stroke="#fff" stroke-width="2" stroke-linejoin="round"/></svg>';
  const FONT = '"Pretendard Variable",Pretendard,-apple-system,sans-serif';

  const state = { cursor: null, caption: null, who: null, spot: null, note: null, ready: false, tracking: 0, zoomed: false };

  function ensure() {
    if (state.ready || !document.body) return;
    state.ready = true;
    const style = document.createElement('style');
    style.id = '__demo-style';
    style.textContent = [
      '#__demo-cursor{position:fixed;left:0;top:0;width:28px;height:36px;pointer-events:none;z-index:2147483647;',
      'transform:translate(-3px,-2px);filter:drop-shadow(0 2px 3px rgba(0,0,0,.35));transition:opacity .25s;}',
      '#__demo-cursor.hidden{opacity:0}',
      '.__demo-ripple{position:fixed;width:44px;height:44px;margin:-22px 0 0 -22px;border-radius:50%;',
      'border:3px solid rgba(138,90,80,.9);pointer-events:none;z-index:2147483646;animation:__demo-ripple .55s ease-out forwards;}',
      '@keyframes __demo-ripple{from{transform:scale(.35);opacity:.95}to{transform:scale(1.3);opacity:0}}',
      // 아래쪽 자막 띠(장면 설명)
      '#__demo-caption{position:fixed;left:50%;bottom:40px;transform:translate(-50%,16px);opacity:0;',
      'max-width:1400px;padding:16px 30px;border-radius:999px;background:rgba(29,23,21,.86);color:#fff;',
      'font-family:' + FONT + ';font-size:30px;font-weight:500;letter-spacing:-.01em;',
      'line-height:1.35;text-align:center;word-break:keep-all;pointer-events:none;z-index:2147483645;',
      'backdrop-filter:blur(10px);box-shadow:0 10px 30px rgba(0,0,0,.18);transition:opacity .35s ease,transform .35s cubic-bezier(.2,.8,.2,1);}',
      '#__demo-caption.show{opacity:1;transform:translate(-50%,0)}',
      '#__demo-caption b{color:#F6D6CC;font-weight:600}',
      'html.__demo-zoom #root{transition:transform .55s cubic-bezier(.2,.8,.2,1);will-change:transform}',
      // 스포트라이트: 요소 둘레만 남기고 어둡게
      '#__demo-spot{position:fixed;left:0;top:0;width:0;height:0;border-radius:16px;pointer-events:none;z-index:2147483640;',
      'box-shadow:0 0 0 3px #D27A62,0 0 0 9999px rgba(22,16,14,.52);opacity:0;transition:opacity .3s ease}',
      '#__demo-spot.show{opacity:1}',
      // 요소 옆에 붙는 설명
      '#__demo-note{position:fixed;left:0;top:0;max-width:1180px;padding:14px 26px;border-radius:18px;background:rgba(29,23,21,.94);color:#fff;',
      'font-family:' + FONT + ';font-size:29px;font-weight:500;letter-spacing:-.01em;line-height:1.38;text-align:center;word-break:keep-all;text-wrap:balance;',
      'pointer-events:none;z-index:2147483645;box-shadow:0 12px 32px rgba(0,0,0,.22);opacity:0;transform:translateY(10px);',
      'transition:opacity .35s ease,transform .35s cubic-bezier(.2,.8,.2,1)}',
      '#__demo-note.show{opacity:1;transform:none}',
      '#__demo-note b{color:#F6D6CC;font-weight:600}',
      '#__demo-note::before{content:"";position:absolute;width:18px;height:18px;background:rgba(29,23,21,.94);transform:rotate(45deg);',
      'left:calc(var(--ax,50%) - 9px);top:-8px;border-radius:3px}',
      '#__demo-note.above::before{top:auto;bottom:-8px}',
      '#__demo-note.side{max-width:640px;text-align:left}',
      '#__demo-note.side::before{left:-8px;top:calc(var(--ay,50%) - 9px)}',
      '#__demo-note.side.left-of::before{left:auto;right:-8px}',
      // 로그인 주체 알약
      '#__demo-who{position:fixed;top:7px;right:300px;z-index:2147483645;display:flex;align-items:center;gap:12px;',
      'padding:7px 20px 7px 10px;border-radius:999px;color:#fff;font-family:' + FONT + ';',
      'font-size:22px;font-weight:600;letter-spacing:-.01em;line-height:1.2;box-shadow:0 10px 28px rgba(0,0,0,.24);pointer-events:none;',
      'opacity:0;transform:translateY(-16px) scale(1.08);transition:opacity .4s ease,transform .55s cubic-bezier(.2,.8,.2,1)}',
      '#__demo-who.show{opacity:1;transform:none}',
      '#__demo-who.kftc{background:#6B3F35}#__demo-who.org{background:#2B5797}',
      '#__demo-who .tag{font-size:16px;font-weight:700;padding:4px 10px;border-radius:999px;background:rgba(255,255,255,.24);letter-spacing:.03em}',
    ].join('');
    document.head.appendChild(style);

    const make = (id) => { const el = document.createElement('div'); el.id = id; document.body.appendChild(el); return el; };
    state.cursor = make('__demo-cursor');
    state.cursor.className = 'hidden';
    state.cursor.innerHTML = CURSOR_SVG;
    state.spot = make('__demo-spot');
    state.caption = make('__demo-caption');
    state.note = make('__demo-note');
    state.who = make('__demo-who');
    try {
      const saved = JSON.parse(window.sessionStorage.getItem('__demo.who') || 'null');
      if (saved) api.who(saved.kind, saved.tag, saved.text);
    } catch (e) { /* 저장소를 못 읽으면 표시만 생략 */ }

    window.addEventListener('mousemove', (e) => {
      state.cursor.style.transform = 'translate(' + (e.clientX - 3) + 'px,' + (e.clientY - 2) + 'px)';
      state.cursor.classList.remove('hidden');
    }, true);
    window.addEventListener('mousedown', (e) => {
      const r = document.createElement('div');
      r.className = '__demo-ripple';
      r.style.left = e.clientX + 'px';
      r.style.top = e.clientY + 'px';
      document.body.appendChild(r);
      setTimeout(() => r.remove(), 600);
    }, true);
  }

  /** 강조 표시된 요소들의 화면상 합집합 사각형(여백 포함). 없으면 null. */
  function focusRect(pad) {
    const els = document.querySelectorAll('[data-demo-focus]');
    if (!els.length) return null;
    let l = Infinity, t = Infinity, r = -Infinity, b = -Infinity;
    els.forEach((el) => {
      const q = el.getBoundingClientRect();
      if (q.width === 0 && q.height === 0) return;
      l = Math.min(l, q.left); t = Math.min(t, q.top); r = Math.max(r, q.right); b = Math.max(b, q.bottom);
    });
    if (l === Infinity) return null;
    return { x: l - pad, y: t - pad, w: r - l + pad * 2, h: b - t + pad * 2 };
  }

  /** 설명 문구는 문장마다 한 줄. 마침표 뒤의 띄어쓰기를 줄바꿈으로 바꾼다(긴 문장만 단어 단위로 두 줄에 고르게 나뉜다). */
  function lines(html) {
    return (html || '').replace(/([.!?])\s+(?=\S)/g, '$1<br>');
  }

  function placeNote(rect) {
    const note = state.note;
    if (!note.innerHTML) return;
    const W = window.innerWidth, H = window.innerHeight;
    const nw = note.offsetWidth, nh = note.offsetHeight;
    const gap = 22;
    const below = rect.y + rect.h + gap + nh <= H - 20;
    const above = !below && rect.y - gap - nh >= 16;
    note.classList.remove('above', 'side');
    if (below || above) {
      const top = below ? rect.y + rect.h + gap : rect.y - gap - nh;
      const cx = rect.x + rect.w / 2;
      const left = Math.min(Math.max(cx - nw / 2, 24), Math.max(24, W - 24 - nw));
      if (above) note.classList.add('above');
      note.style.left = left + 'px';
      note.style.top = top + 'px';
      note.style.setProperty('--ax', Math.min(Math.max(cx - left, 28), nw - 28) + 'px');
      return;
    }
    // 위아래 모두 자리가 없으면(세로로 긴 요소) 오른쪽, 그것도 없으면 왼쪽에 붙인다.
    const right = rect.x + rect.w + gap + nw <= W - 24;
    const left = right ? rect.x + rect.w + gap : Math.max(24, rect.x - gap - nw);
    const cy = rect.y + rect.h / 2;
    const top = Math.min(Math.max(cy - nh / 2, 16), H - 16 - nh);
    note.classList.add('side');
    note.classList.toggle('left-of', !right);
    note.style.left = left + 'px';
    note.style.top = top + 'px';
    note.style.setProperty('--ay', Math.min(Math.max(cy - top, 24), nh - 24) + 'px');
  }

  function drawSpot(rect) {
    const s = state.spot.style;
    s.left = rect.x + 'px'; s.top = rect.y + 'px'; s.width = rect.w + 'px'; s.height = rect.h + 'px';
  }

  function track(pad) {
    const id = ++state.tracking;
    const step = () => {
      if (state.tracking !== id) return;
      const rect = focusRect(pad);
      if (rect) { drawSpot(rect); placeNote(rect); }
      requestAnimationFrame(step);
    };
    step();
  }

  function applyZoom(cx, cy, scale) {
    const root = document.getElementById('root') || document.body;
    const W = window.innerWidth, H = window.innerHeight;
    const vw = W / scale, vh = H / scale;
    const left = Math.min(Math.max(cx - vw / 2, 0), W - vw);
    const top = Math.min(Math.max(cy - vh / 2, 0), H - vh);
    const ox = scale <= 1 ? 0 : (left * scale) / (scale - 1);
    const oy = scale <= 1 ? 0 : (top * scale) / (scale - 1);
    document.documentElement.classList.add('__demo-zoom');
    root.style.transformOrigin = ox + 'px ' + oy + 'px';
    root.style.transform = 'scale(' + scale + ')';
    state.zoomed = scale > 1;
  }

  const api = {
    ensure,
    /** 아래쪽 자막을 바꾼다. 비우면 숨긴다. <b> 로 강조할 수 있다. */
    caption(html) {
      ensure();
      const el = state.caption;
      if (!html) { el.classList.remove('show'); return; }
      if (el.classList.contains('show') && el.innerHTML !== html) {
        el.classList.remove('show');
        setTimeout(() => { el.innerHTML = html; el.classList.add('show'); }, 220);
      } else {
        el.innerHTML = html;
        el.classList.add('show');
      }
    },
    /**
     * 강조: data-demo-focus 가 붙은 요소들을 스포트라이트로 비추고, 잘리지 않는 배율까지 확대하고, 설명을 옆에 붙인다.
     * 확대 배율(scale)은 상한이다. 요소가 화면에 다 들어오도록 줄인다. 아래쪽 자막은 숨긴다.
     */
    focus(opts) {
      ensure();
      const pad = opts.pad == null ? 12 : opts.pad;
      const rect = focusRect(pad);
      if (!rect) return false;
      const W = window.innerWidth, H = window.innerHeight;
      const scale = Math.max(1, Math.min(opts.scale || 1, (W - 140) / rect.w, (H - 300) / rect.h));
      api.caption(null);
      const note = state.note;
      note.innerHTML = lines(opts.note);
      note.classList.remove('show');
      drawSpot(rect);
      state.spot.classList.add('show');
      applyZoom(rect.x + rect.w / 2, rect.y + rect.h / 2, scale);
      track(pad);
      if (opts.note) setTimeout(() => note.classList.add('show'), 300);
      return true;
    },
    /** 강조 중에 설명 문구만 바꾼다. */
    note(html) {
      ensure();
      const note = state.note;
      if (!html) { note.classList.remove('show'); return; }
      note.classList.remove('show');
      setTimeout(() => { note.innerHTML = lines(html); note.classList.add('show'); }, 200);
    },
    /** 강조를 끝낸다: 스포트라이트·설명을 지우고 확대를 되돌린다. */
    unfocus() {
      ensure();
      state.tracking++;
      state.spot.classList.remove('show');
      state.note.classList.remove('show');
      document.querySelectorAll('[data-demo-focus]').forEach((el) => el.removeAttribute('data-demo-focus'));
      api.unzoom();
    },
    /**
     * 화면의 한 점을 중심으로 앱(#root)을 부드럽게 확대한다(스포트라이트 없이). 좌표는 CSS 픽셀(뷰포트 기준).
     * 가장자리 요소를 확대할 때 화면 밖으로 밀리지 않도록, 확대 뒤 보이는 창이 화면 안에 머물게 원점을 맞춘다.
     */
    zoom(x, y, scale) {
      ensure();
      applyZoom(x, y, scale);
    },
    unzoom() {
      const root = document.getElementById('root') || document.body;
      root.style.transform = 'scale(1)';
      state.zoomed = false;
      setTimeout(() => { if (!state.zoomed) document.documentElement.classList.remove('__demo-zoom'); }, 600);
    },
    hideCursor() { ensure(); state.cursor.classList.add('hidden'); },
    /** 지금 로그인한 사람을 머리 막대의 알약으로 보인다. kind 는 'kftc'(결제원) 또는 'org'(참가기관). */
    who(kind, tag, text) {
      ensure();
      const el = state.who;
      el.className = kind;
      el.innerHTML = '<span class="tag"></span><span class="text"></span>';
      el.querySelector('.tag').textContent = tag;
      el.querySelector('.text').textContent = text;
      requestAnimationFrame(() => el.classList.add('show'));
    },
  };
  window.__demo = api;
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', ensure);
  else ensure();
})();
`;
