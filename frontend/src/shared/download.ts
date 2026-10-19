/** Content-Disposition 의 filename*=UTF-8''… (한글 이름) 을 읽는다. */
export function fileNameOf(response: Response): string | undefined {
  const header = response.headers.get('Content-Disposition') ?? '';
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header)?.[1];
  if (encoded) return decodeURIComponent(encoded);
  return /filename="?([^";]+)"?/i.exec(header)?.[1];
}

/** 받은 파일을 브라우저 내려받기로 저장한다. 내부망 전용 엑셀처럼 망 전환 헤더를 실어 API 클라이언트로 받은 파일에 쓴다. */
export function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1_000);
}
