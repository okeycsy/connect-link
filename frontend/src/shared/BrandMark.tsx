import { Image } from '@mantine/core';

import logo from '../assets/brand/logo.png';

interface Props {
  /** 표시 높이(px). 상단 바 40. 너비는 비율대로 따라간다. */
  height?: number;
  /** 화면 폭에 따라 높이를 바꿀 때 쓰는 클래스(로그인 제목). 주면 height 대신 클래스가 높이를 정한다. */
  className?: string;
}

/**
 * 서비스 로고(금융결제원 연결고리 CONTACT HUB). 파일은 `assets/brand/logo.png` 하나이고 빌드 결과에 들어가 외부에서 불러오지
 * 않는다. 상단 바, 로그인 제목, 회원가입·승인 대기 머리에서 쓴다. 파비콘은 고리 모양만 잘라 둔 `public/favicon.png` 다.
 */
export function BrandMark({ height = 40, className }: Props) {
  return (
    <Image
      src={logo}
      h={className ? undefined : height}
      w="auto"
      fit="contain"
      className={className}
      alt="금융결제원 연결고리"
    />
  );
}
