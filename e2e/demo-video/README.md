# 시연영상 녹화 스크립트

최종 심사용 시연영상(2분 50초 안팎, 1920×1080)을 실제 앱 화면에서 자동으로 찍어 만든다.
타이틀 카드(큰 글자 화면)와 앱 조작 장면을 Playwright 로 녹화하고 ffmpeg 로 이어 붙인다.
외부 자원은 쓰지 않는다(글꼴은 프론트의 Pretendard 패키지, 로고는 앱 자산).

영상은 **장(chapter)** 단위로 모듈화되어 있다. 장마다 파일 하나이고, 고친 장만 다시 찍어 전체를 다시 이어 붙인다.

| 파일 | 역할 |
| --- | --- |
| `scenes/01-intro.ts` … `06-outro.ts` | 장별 장면표. 카드 문구, 앱 조작 동선, 강조 설명, 내레이션 대본이 순서대로 들어 있다. 영상을 바꾸려면 해당 장 파일만 고친다 |
| `scenes/index.ts` | 장 순서(`CHAPTERS`)와 `--only=` 에 쓰는 id 풀이 |
| `scenes/types.ts` | 장면·장 타입, 데모 계정·샘플 데이터 상수, 시점 전환(`asKftc`, `asOrg`) |
| `record.ts` | 장면마다 새 브라우저 문맥을 열어 `out/segments/*.webm` 으로 녹화하고 `out/timeline.json` 을 남긴다. `--only=<장 id 나 장면 id>` 로 일부만 다시 찍으면 나머지 조각은 그대로 둔다 |
| `compose.ts` | 녹화 조각을 0.4초 크로스페이드로 이어 `out/demo.mp4` 를 만든다. `--music=`(배경음), `--voice=`(내레이션 음성), `--out=`, `--chapter=<장 id>`(그 장만 `out/demo-<장>.mp4` 로, 기능별 짧은 영상) |
| `docs.ts` | 녹화본 기준 시나리오 문서(`out/scenario.md`)와 내레이션 대본(`out/narration.md`)을 만든다. 장면표 앞글은 `scenario-intro.md` |
| `timeline.ts` | 조각 길이를 재서 합성본 기준 장면 시각을 계산한다(compose·docs 공용) |
| `music.ts` | 임시 배경음(합성 패드) `out/bgm.wav` 를 만든다. 정식 음원이 있으면 쓰지 않는다 |
| `overlay.ts` | 녹화 화면에 덧그리는 가짜 커서·클릭 물결·자막 띠·로그인 주체 알약·스포트라이트 강조. 앱 코드는 건드리지 않는다 |
| `helpers.ts` | 장면 스크립트가 쓰는 연출 도우미(`Director`): 천천히 움직이는 마우스, 타자, 자막, 강조(`focus`), 데모 로그인·세션 전환 |
| `cards.ts` | 타이틀 카드 HTML(애플 키노트식 큰 글자, 숫자 카운트업) |
| `server.ts` | 카드 HTML·글꼴·로고를 내려 주는 작은 정적 서버(녹화와 미리보기가 같이 쓴다) |
| `preview-cards.ts` | 녹화 없이 타이틀 카드만 `out/cards/{id}.png` 로 찍는다. 문구·글꼴을 고칠 때 먼저 본다 |
| `run.sh` | 전부 한 번에: app.jar 를 demo 프로필로 새로 띄우고 → 녹화 → 합성 → 문서 |

## 만들기

```bash
./gradlew :backend:bootJar -x test          # backend/build/libs/app.jar
e2e/demo-video/run.sh                        # out/demo.mp4 (무음) + out/scenario.md, out/narration.md
e2e/demo-video/run.sh --music=e2e/demo-video/out/bgm.wav   # 배경음 포함
```

- Node 22 이상(`.ts` 를 바로 실행한다), ffmpeg 6 이상, `e2e/` 의 `npm install`(Playwright) 이 필요하다.
- 장면이 데이터를 바꾸므로(회신 제출, 일시중단 확정 등) **녹화할 때마다 서버를 새로 띄운다**. `run.sh` 가 그렇게 한다.
- 03 일시중단 장은 중단 일시를 서울 기준 "내일"로 골라 전 영업일 취합에 들어가게 한다. 자정(KST) 전후에 걸쳐 찍으면 취합에 빠질 수 있으니 피한다.
- Chromium 경로가 기본과 다르면 `PLAYWRIGHT_CHROMIUM_PATH`, 서버 주소가 다르면 `DEMO_BASE_URL` 로 준다.
- 내레이션 음성 파일이 있으면 `node e2e/demo-video/compose.ts --voice=narration.m4a --music=out/bgm.wav` 로 입힌다. 대본은 `out/narration.md`.

## 장 하나만 고쳐서 다시 찍기 (애자일하게 고치는 법)

```bash
e2e/demo-video/run.sh --only=suspensions          # 03 일시중단 장만 다시 찍고 전체를 다시 이어 붙인다 (1~2분)
e2e/demo-video/run.sh --only=flow-branch,chat      # 장면 id 로도 된다
node e2e/demo-video/compose.ts --chapter=flow      # 01 장만 짧은 영상으로 → out/demo-flow.mp4
node e2e/demo-video/preview-cards.ts               # 카드 문구만 고쳤을 때 먼저 보기
```

장 id: `intro`(198시간 장표·소개·로그인), `flow`(01 연락처→회신), `tests`(02 기관 테스트), `suspensions`(03 일시중단), `chat`(04 소통 채널), `outro`(기대효과·마무리).
장 안의 장면들은 서로 이어지지만(기관 회신 뒤 집계) 장끼리는 독립이라 어느 장이든 따로 찍을 수 있다. 서버를 새로 띄우고 찍으므로 다른 장의 데이터와 섞이지 않는다.

## 장면을 고칠 때

- **강조는 `d.focus(요소, { note, scale, hold })`**: 자막이 가리키는 요소만 스포트라이트로 비추고(나머지는 어둡게), 화면에 다 들어오는 배율까지만 확대하고, 설명 말풍선을 요소 바로 옆에 붙인다. 요소 여러 개를 배열로 주면 그 묶음을 비춘다. `hold` 를 주면 그만큼 보여 주고 스스로 되돌리고, 안 주면 비춘 채로 클릭·타자를 이어 가다가 `d.unfocus()` 로 되돌린다. 비추는 동안 요소가 자라거나 움직여도 따라간다. 문구만 바꾸려면 `d.note('…')`.
- 최종본은 아래쪽 자막 띠를 쓰지 않는다. 모든 설명은 그 말이 가리키는 요소를 비추며 옆에 붙인다. 띠가 꼭 필요하면 `d.caption('…')`, 숨기기는 `d.caption(null)`.
- 장 카드는 `chapter: '01 연락처에서 회신까지'` 로 장 제목을 크게 두고 문장은 그 아래 작게 둔다. 긴 문장은 `titleSize` 로 줄이고, 숫자 카드의 "약" 같은 앞말은 `stat.prefix` 다.
- 시점 전환은 `asKftc(d)`(결제원 홍성민 과장, 갈색 알약) / `asOrg(d)`(KB국민은행 김민준 대리, 파란 알약). 로그인 화면을 보이려면 `d.loginAs('이름')`.
- 상태가 바뀌어야 하는 버튼(제출·확정·보내기)은 `d.clickUntil(버튼, 기대 요소)` 로 눌러 애니메이션 중에 빠진 클릭을 한 번 더 누른다.
- 요소는 e2e 테스트와 같은 `data-testid`·역할·이름으로 찾는다. 화면 문구가 바뀌면 e2e 와 함께 여기도 고친다.
- 앱 장면에는 `summary`(화면·동선 한 줄)를 꼭 쓴다. 시나리오 문서의 장면표가 여기서 나온다.
