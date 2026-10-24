import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { OrgLogo } from './OrgLogo';

describe('OrgLogo', () => {
  it('로고 파일이 있는 기관은 원형 그림을 그린다', () => {
    const { container } = render(<OrgLogo code="004" />);
    const img = container.querySelector('img');
    expect(img).not.toBeNull();
    expect(img?.getAttribute('src')).toContain('004');
  });

  it('로고 파일이 없는 기관은 아무것도 그리지 않는다', () => {
    const { container } = render(<OrgLogo code="103" />);
    expect(container).toBeEmptyDOMElement();
  });
});
