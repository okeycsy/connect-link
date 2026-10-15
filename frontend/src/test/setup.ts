import '@testing-library/jest-dom/vitest';

import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// globals 를 끈 설정이라 Testing Library 의 자동 정리가 안 붙는다. 테스트마다 렌더한 화면을 지운다.
afterEach(cleanup);

// Mantine 이 jsdom 에서 쓰는 브라우저 API 보강.
window.matchMedia =
  window.matchMedia ||
  ((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }));

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = window.ResizeObserver || ResizeObserverStub;

window.HTMLElement.prototype.scrollIntoView =
  window.HTMLElement.prototype.scrollIntoView || (() => {});
