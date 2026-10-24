/**
 * 참가기관 로고(원형). 파일은 `assets/org-logos/{3자리 금융기관 공동코드}.svg|png` 이고 빌드에 묶여 외부에서 불러오지 않는다.
 * 파일이 없는 기관은 아무것도 그리지 않는다(SHY 요청 2026-10-24: 없는 로고는 자리도 두지 않는다).
 * 원형이 아닌 원본은 기관 색 원 안에 심볼을 흰색으로 넣어 다듬었다(docs/decisions.md 2026-10-24).
 */
const LOGOS: Record<string, string> = Object.fromEntries(
  Object.entries(
    import.meta.glob<string>('../assets/org-logos/*.{svg,png}', {
      eager: true,
      query: '?url',
      import: 'default',
    }),
  ).map(([path, url]) => [path.replace(/^.*\/([^/]+)\.(svg|png)$/, '$1'), url]),
);

/** 이 기관 코드의 로고 주소. 없으면 undefined. */
function orgLogoUrl(code: string | null | undefined): string | undefined {
  return code ? LOGOS[code] : undefined;
}

interface Props {
  code: string | null | undefined;
  size?: number;
}

/** 기관명 옆에 두는 원형 로고. 이름은 옆 글자가 말하므로 그림은 화면 낭독기에서 숨긴다. */
export function OrgLogo({ code, size = 20 }: Props) {
  const url = orgLogoUrl(code);
  if (!url) {
    return null;
  }
  return (
    <img
      src={url}
      alt=""
      aria-hidden
      width={size}
      height={size}
      style={{ borderRadius: '50%', flex: 'none', display: 'block' }}
    />
  );
}
