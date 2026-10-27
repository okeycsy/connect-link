# 시연영상 녹화 스크립트

최종 심사용 시연영상(2분 50초 안팎, 1920×1080)을 실제 앱 화면에서 자동으로 찍어 만든다.
타이틀 카드(큰 글자 화면)와 앱 조작 장면을 Playwright 로 녹화하고 ffmpeg 로 이어 붙인다.
외부 자원은 쓰지 않는다(글꼴은 프론트의 Pretendard 패키지, 로고는 앱 자산).

| 파일 | 역할 |
| --- | --- |
| `scenes.ts` | 장면표. 카드 문구, 앱 조작 동선, 자막, 내레이션 대본이 순서대로 들어 있다. 영상을 바꾸려면 이 파일만 고친다 |
| `record.ts` | 장면마다 새 브라우저 문맥을 열어 `out/segments/*.webm` 으로 녹화하고 `out/timeline.json` 을 남긴다. `--only=id1,id2` 로 일부만 다시 찍는다 |
| `compose.ts` | 녹화 조각을 0.4초 크로스페이드로 이어 `out/demo.mp4` 를 만든다. `--music=`(배경음), `--voice=`(내레이션 음성), `--out=` |
| `music.ts` | 임시 배경음(합성 패드) `out/bgm.wav` 를 만든다. 정식 음원이 있으면 쓰지 않는다 |
| `overlay.ts` | 녹화 화면에 덧그리는 가짜 커서·클릭 물결·자막 띠·확대(줌). 앱 코드는 건드리지 않는다 |
| `helpers.ts` | 장면 스크립트가 쓰는 연출 도우미(`Director`): 천천히 움직이는 마우스, 타자, 자막, 확대, 데모 로그인·세션 전환 |
| `cards.ts` | 타이틀 카드 HTML(애플 키노트식 큰 글자, 숫자 카운트업) |
| `server.ts` | 카드 HTML·글꼴·로고를 내려 주는 작은 정적 서버(녹화와 미리보기가 같이 쓴다) |
| `preview-cards.ts` | 녹화 없이 타이틀 카드만 `out/cards/{id}.png` 로 찍는다. 문구·글꼴을 고칠 때 먼저 본다 |
| `run.sh` | 전부 한 번에: app.jar 를 demo 프로필로 새로 띄우고 → 녹화 → 합성 |

## 만들기

```bash
./gradlew :backend:bootJar -x test          # backend/build/libs/app.jar
e2e/demo-video/run.sh                        # out/demo.mp4 (무음)
e2e/demo-video/run.sh --music=e2e/demo-video/out/bgm.wav   # 배경음 포함
```

- Node 22 이상(`.ts` 를 바로 실행한다), ffmpeg 6 이상, `e2e/` 의 `npm install`(Playwright) 이 필요하다.
- 장면이 데이터를 바꾸므로(회신 제출, 일시중단 확정 등) **녹화할 때마다 서버를 새로 띄운다**. `run.sh` 가 그렇게 한다. `record.ts --only=` 로 일부만 다시 찍을 때도 서버를 먼저 새로 띄운다.
- Chromium 경로가 기본과 다르면 `PLAYWRIGHT_CHROMIUM_PATH`, 서버 주소가 다르면 `DEMO_BASE_URL` 로 준다.
- 카드 문구만 고쳤으면 `node e2e/demo-video/preview-cards.ts` 로 먼저 확인한 뒤 전체를 다시 찍는다.
- 내레이션 음성 파일이 있으면 `node e2e/demo-video/compose.ts --voice=narration.m4a --music=out/bgm.wav` 로 입힌다. 대본은 `scenes.ts` 의 `narration` 이다.

## 장면을 고칠 때

- 자막은 `d.caption('… <b>강조</b> …')`, 숨기기는 `d.caption(null)`.
- 확대는 `d.zoomTo(요소, 배율)` 또는 `d.zoomBetween(요소A, 요소B, 배율)`, 되돌리기는 `d.unzoom()`. 확대 전에 요소가 화면 가운데 가까이 오도록 `d.centerOn(요소)` 을 쓴다.
- 다른 사람 시점으로 넘어갈 때는 `d.switchSession(이메일)`(화면 전환 없이 로그인 API 로 바꾼다), 로그인 화면을 보이려면 `d.loginAs('이름')`.
- 요소는 e2e 테스트와 같은 `data-testid`·역할·이름으로 찾는다. 화면 문구가 바뀌면 e2e 와 함께 여기도 고친다.
