import { Image } from '@mantine/core';

import logo from '../assets/brand/logo.png';
import logoMascot from '../assets/brand/logo-mascot.png';

interface Props {
  /** 표시 높이(px). 상단 바 40. 너비는 비율대로 따라간다. */
  height?: number;
  /** 화면 폭에 따라 높이를 바꿀 때 쓰는 클래스(로그인 제목). 주면 height 대신 클래스가 높이를 정한다. */
  className?: string;
  /** 마스코트(고양이)까지 넣은 전체 로고. 넓은 자리(로그인 제목)에만 쓴다. */
  mascot?: boolean;
}

/**
 * 서비스 로고(금융결제원 연결이 CONTACT HUB). 파일은 고리와 글자만 있는 `assets/brand/logo.png` 와 마스코트까지 있는
 * `assets/brand/logo-mascot.png` 둘이고 빌드 결과에 들어가 외부에서 불러오지 않는다. 상단 바, 회원가입·승인 대기 머리는
 * 고리와 글자만, 로그인 제목은 마스코트까지 쓴다. 파비콘은 고리 모양만 잘라 둔 `public/favicon.png` 다.
 */
export function BrandMark({ height = 40, className, mascot = false }: Props) {
  return (
    <Image
      src={mascot ? logoMascot : logo}
      h={className ? undefined : height}
      w="auto"
      fit="contain"
      className={className}
      alt="금융결제원 연결이"
    />
  );
}
