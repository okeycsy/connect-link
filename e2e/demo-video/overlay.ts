/**
 * 녹화 중인 화면에 끼워 넣는 연출 요소. 페이지가 열릴 때마다(addInitScript) 한 번 실행되어
 * 가짜 커서, 자막 띠, 확대(줌) 도우미를 `window.__demo` 로 노출한다.
 *
 * 앱 코드는 전혀 건드리지 않고 브라우저 안에서만 덧그린다. 실제 마우스 이동(Playwright mouse)을 따라
 * 커서가 움직이고, 클릭하면 물결 효과가 남는다.
 */
export const OVERLAY_SCRIPT = String.raw`
(() => {
  if (window.__demo) return;
  const CURSOR_SVG = '<svg width="28" height="36" viewBox="0 0 28 36" xmlns="http://www.w3.org/2000/svg">'
    + '<path d="M3 2 L3 27 L9.5 21.5 L13.5 31 L18 29 L14 19.5 L23 19.5 Z" fill="#111" stroke="#fff" stroke-width="2" stroke-linejoin="round"/></svg>';

  const state = { cursor: null, caption: null, who: null, captionTimer: null, ready: false };

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
      '#__demo-caption{position:fixed;left:50%;bottom:40px;transform:translate(-50%,16px);opacity:0;',
      'max-width:1400px;padding:16px 30px;border-radius:999px;background:rgba(29,23,21,.86);color:#fff;',
      'font-family:"Pretendard Variable",Pretendard,-apple-system,sans-serif;font-size:30px;font-weight:500;letter-spacing:-.01em;',
      'line-height:1.35;text-align:center;word-break:keep-all;pointer-events:none;z-index:2147483645;',
      'backdrop-filter:blur(10px);box-shadow:0 10px 30px rgba(0,0,0,.18);transition:opacity .35s ease,transform .35s cubic-bezier(.2,.8,.2,1);}',
      '#__demo-caption.show{opacity:1;transform:translate(-50%,0)}',
      '#__demo-caption b{color:#F6D6CC;font-weight:600}',
      'html.__demo-zoom #root{transition:transform .9s cubic-bezier(.2,.8,.2,1);will-change:transform}',
      '#__demo-who{position:fixed;top:7px;right:300px;z-index:2147483645;display:flex;align-items:center;gap:12px;',
      'padding:7px 20px 7px 10px;border-radius:999px;color:#fff;font-family:"Pretendard Variable",Pretendard,-apple-system,sans-serif;',
      'font-size:22px;font-weight:600;letter-spacing:-.01em;line-height:1.2;box-shadow:0 10px 28px rgba(0,0,0,.24);pointer-events:none;',
      'opacity:0;transform:translateY(-16px) scale(1.08);transition:opacity .4s ease,transform .55s cubic-bezier(.2,.8,.2,1)}',
      '#__demo-who.show{opacity:1;transform:none}',
      '#__demo-who.kftc{background:#6B3F35}#__demo-who.org{background:#2B5797}',
      '#__demo-who .tag{font-size:16px;font-weight:700;padding:4px 10px;border-radius:999px;background:rgba(255,255,255,.24);letter-spacing:.03em}',
    ].join('');
    document.head.appendChild(style);

    const cursor = document.createElement('div');
    cursor.id = '__demo-cursor';
    cursor.className = 'hidden';
    cursor.innerHTML = CURSOR_SVG;
    document.body.appendChild(cursor);
    state.cursor = cursor;

    const caption = document.createElement('div');
    caption.id = '__demo-caption';
    document.body.appendChild(caption);
    state.caption = caption;

    const who = document.createElement('div');
    who.id = '__demo-who';
    document.body.appendChild(who);
    state.who = who;
    try {
      const saved = JSON.parse(window.sessionStorage.getItem('__demo.who') || 'null');
      if (saved) api.who(saved.kind, saved.tag, saved.text);
    } catch (e) { /* 저장소를 못 읽으면 표시만 생략 */ }

    window.addEventListener('mousemove', (e) => {
      cursor.style.transform = 'translate(' + (e.clientX - 3) + 'px,' + (e.clientY - 2) + 'px)';
      cursor.classList.remove('hidden');
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

  const api = {
    ensure,
    /** 자막을 바꾼다. 비우면 숨긴다. <b> 로 강조할 수 있다. */
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
     * 화면의 한 점을 중심으로 앱(#root)을 부드럽게 확대한다. 좌표는 CSS 픽셀(뷰포트 기준). 자막·커서는 그대로다.
     * 가장자리 요소를 확대할 때 화면 밖으로 밀리지 않도록, 확대 뒤 보이는 창이 화면 안에 머물게 원점을 맞춘다.
     */
    zoom(x, y, scale) {
      ensure();
      const root = document.getElementById('root') || document.body;
      const W = window.innerWidth, H = window.innerHeight;
      const vw = W / scale, vh = H / scale;
      const left = Math.min(Math.max(x - vw / 2, 0), W - vw);
      const top = Math.min(Math.max(y - vh / 2, 0), H - vh);
      const ox = scale <= 1 ? 0 : (left * scale) / (scale - 1);
      const oy = scale <= 1 ? 0 : (top * scale) / (scale - 1);
      document.documentElement.classList.add('__demo-zoom');
      root.style.transformOrigin = ox + 'px ' + oy + 'px';
      root.style.transform = 'scale(' + scale + ')';
    },
    unzoom() {
      const root = document.getElementById('root') || document.body;
      root.style.transform = 'scale(1)';
      setTimeout(() => { document.documentElement.classList.remove('__demo-zoom'); }, 950);
    },
    hideCursor() { ensure(); state.cursor.classList.add('hidden'); },
    /** 지금 로그인한 사람을 오른쪽 위 알약으로 보인다. kind 는 'kftc'(결제원) 또는 'org'(참가기관). */
    who(kind, tag, text) {
      ensure();
      const el = state.who;
      const html = '<span class="tag"></span><span class="text"></span>';
      el.className = kind;
      el.innerHTML = html;
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
