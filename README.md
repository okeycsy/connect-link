# 연결고리 (connect-link)

결제원 담당자와 참가기관 담당자를 잇는 업무 포털의 프로토타입. 네 업무 영역(계좌통합관리, 자동이체통합관리, 대출이동, CMS)에서 연락처, 의견 요청, 기관 테스트, 일시중단을 한곳에서 다룬다.

- 작업 규칙과 기술 스택: [CLAUDE.md](./CLAUDE.md)
- 서비스 최상위 분류: [docs/service-taxonomy.md](./docs/service-taxonomy.md)
- 공통 화면 기획: [docs/design/common-screens.md](./docs/design/common-screens.md)
- 명세에 없던 결정: [docs/decisions.md](./docs/decisions.md)

모든 샘플 데이터는 가상이다. 공개 저장소이므로 실제 기관·사람·연락처를 넣지 않는다.

## 실행 (시연)

빌드된 `app.jar` 하나로 서버와 화면이 함께 뜬다. 별도 DB 설치가 없고(H2 메모리), 실행 중 외부 네트워크를 쓰지 않는다. Java 17 이상이 필요하다.

```bash
java -jar app.jar
# 브라우저에서 http://localhost:8080
```

기본 프로필은 `demo`(샘플 데이터 포함)다. `app.jar` 는 GitHub Actions 실행 페이지의 아티팩트 `app.jar` 에서 내려받는다.

| 주소 | 내용 |
| --- | --- |
| `/` | 화면 (홈 → `/account-info/home`) |
| `/login` | 로그인 (회사 이메일 + 비밀번호. `demo` 프로필은 역할별 데모 계정 목록도 보인다) |
| `/sign-up` | 회원가입 4단계 (회사 이메일 인증 → 기관과 업무 → 직무 → 담당자 정보). 신청하면 `/sign-up/pending`(승인 대기) |
| `/{업무}/notifications` | 알림 센터 |
| `/{업무}/me` | 내 정보와 탈퇴(비밀번호를 다시 넣는다. 데모 관리자는 하나뿐이라 탈퇴가 막힌다). 연락처 수정은 연락처 모듈 후 |
| `/{업무}/demo/outbox` | 발송함 (데모 도구) |
| `/{업무}/demo/states` | 상태 화면 모음 (데모 도구: 빈 화면·오류·권한 없음·주소 없음·내부망 전용) |
| `/{업무}/demo/attachments` | 첨부파일 (데모 도구: 공통 첨부 부품으로 올리기·내려받기·지우기) |
| `/{업무}/opinions` | 의견 요청 (상태별 목록 → 상세. 결제원 담당자는 새 요청·고치기, 회신 현황·취합표(엑셀은 내부망)·리마인드 이력·변경 이력 탭. 기관 담당자는 요청을 보고 회신을 임시저장·제출·수정 요청) |
| `/{업무}/admin` | 관리 허브 (가입 승인, 기준정보. 정기 변경 주간·알림 대상은 준비 중). 관리자와 결제원 담당자가 본다 |
| `/{업무}/admin/sign-ups` | 가입 승인 (그 업무를 신청한 승인 대기·승인·반려 목록 + 오른쪽 상세 패널에서 승인·반려, 승인한 참가기관 담당자는 탈퇴 처리) |
| `/{업무}/admin/organizations` | 기관·업무 기준정보 (목록 + 오른쪽 상세 패널, 변경 이력 탭). 관리자만 본다 |
| `/swagger-ui.html` | API 문서 |
| `/api/v1/health` | 서버 상태 |

### 데모 계정

`demo`·`local` 프로필에는 역할별 데모 계정이 들어 있다. 비밀번호는 모두 **`demo1234!`** 이다. 로그인 화면의 "데모 계정으로 들어가기" 에서 고르면 같은 로그인 절차를 거쳐 들어가고, 로그인한 뒤에는 상단 **데모** 메뉴의 **계정 전환** 으로 바꾼다. 운영 프로필에는 데모 계정이 생기지 않는다.

| 이메일 | 이름 | 소속 · 역할 | 담당 업무 | 비고 |
| --- | --- | --- | --- | --- |
| `admin@kftc.example.com` | 데모 관리자 | 결제원 · 전산(운영) | 전 업무 | 관리자. 관리 메뉴 전체(기준정보 포함)가 보이고 모든 업무의 가입을 승인한다 |
| `hong.gildong@kftc.example.com` | 홍길동 | 결제원 · 현업 | 계좌통합관리, 자동이체통합관리 | 맡은 업무의 가입을 승인한다. 계좌통합관리에 승인 대기 2건이 있다. 계좌통합관리·자동이체통합관리 의견 요청 담당(회신중·마감·작성중 요청이 있다) |
| `kim.ops@kftc.example.com` | 김운영 | 결제원 · 전산(운영) | 전 업무 | |
| `lee.control@kftc.example.com` | 이상황 | 결제원 · 전산(상황실) | 전 업무 | |
| `park.cms@kftc.example.com` | 박하늘 | 결제원 · 현업 | CMS | CMS 의견 요청 담당 |
| `kim.gana@gana.example.com` | 김가나 | 가나은행(참가기관) · 현업 | 계좌통합관리 | "수수료 체계 개편" 의견 요청에 아직 회신하지 않았다 |
| `lee.dara@dara.example.com` | 이다라 | 다라은행(참가기관) · 전산(개발, 대외계) | 자동이체통합관리, CMS | |
| `choi.saa@saa.example.com` | 최사아 | 사아증권(참가기관) · 현업 | 계좌통합관리(오픈뱅킹 안심차단) | 제출한 회신을 수정 요청해 승인을 기다린다 |
| `park.dara@dara.example.com` | 박다라 | 다라은행(참가기관) · 현업 | 계좌통합관리(내 계좌 한눈에) | 의견 요청 대상을 세부업무·업권으로 고를 때 보인다 |
| `yoon.chaka@chaka.example.com` | 윤차카 | 차카저축은행(참가기관) · 현업 | 계좌통합관리(오픈뱅킹 안심차단) | 〃 |
| `jung.nada@nada.example.com` | 정나다 | 나다상호금융(참가기관) · 전산(개발) | 계좌통합관리(내 계좌 한눈에) | 〃 |
| `jung.maba@maba.example.com` | 정마바 | 마바저축은행(참가기관) · 전산(개발) | 대출이동 | 가입 승인 대기. 로그인하면 승인 전 안내가 나온다 |

결제원 계정은 관리 메뉴가 보이고 맡은 업무의 가입을 승인한다(전 업무를 맡은 김운영·이상황도 승인할 수 있다). 참가기관 계정에는 관리 메뉴가 없다. 정마바 말고도 데모 계정 목록에 없는 샘플 가입 신청이 있다(비밀번호는 같은 `demo1234!`).

| 이메일 | 이름 | 소속 · 역할 | 신청 업무 | 상태 |
| --- | --- | --- | --- | --- |
| `han.gana@gana.example.com` | 한가나 | 가나은행 · 현업 | 계좌통합관리 | 승인 대기 |
| `oh.dara@mail.example.com` | 오다라 | 다라은행 · 전산(개발) | 계좌통합관리, 자동이체통합관리 | 승인 대기. 이메일 도메인이 기관과 달라 "도메인 다름" 이 붙는다 |
| `seo.saa@saa.example.com` | 서사아 | 사아증권 · 현업 | 계좌통합관리 | 반려(홍길동). 같은 이메일로 다시 가입 신청할 수 있다 |

왼쪽 레일에는 담당 업무만 보이고(관리자는 전 업무), 담당이 아닌 업무 영역은 서버도 403 으로 막는다. 상단 바의 **데모** 메뉴에서 계정 전환, 발송함(메일·매터모스트 기록), 상태 화면 모음, 첨부파일, 망 전환(내부망 ↔ 인터넷망, 서버가 `X-Network-Zone` 헤더를 믿는 것은 `demo`·`local` 뿐)을 쓸 수 있다. 전역 검색은 상단 검색 상자나 **Ctrl K** 로 연다.

가입은 로그인 화면의 **회원가입** 에서 시연한다. 아무 `…@gana.example.com` 같은 가상 이메일로 시작하면 되고, 인증 코드 메일은 실제로 가지 않고 가입 화면 아래 **받은 메일함**(데모)에 보인다. 신청한 계정은 승인 대기라 아직 로그인할 수 없다. 승인은 **홍길동**(계좌통합관리·자동이체통합관리)이나 데모 관리자로 들어가 **관리 → 가입 승인** 에서 한다. 승인 메일과 반려 메일(사유 포함)도 받은 메일함과 발송함에 남는다. 승인 목록의 참가기관 담당자는 상세에서 사유를 적어 **탈퇴 처리** 할 수 있고, 탈퇴 메일(사유 포함)이 발송함에 남는다.

### 외부 미리보기 (Render)

내부망 시연과 별개로, 여러 기기에서 같은 주소로 보려고 [Render](https://render.com) 무료 웹 서비스에 올려 두었다. 올라가는 데이터는 샘플(가상)뿐이다.

- 미리보기 주소: **https://connect-link-xmwv.onrender.com** (`demo` 프로필)
- `main` 에 머지될 때마다 자동으로 다시 배포된다(빌드 5~10분).
- 무료 등급은 15분 동안 요청이 없으면 잠들고, 다음 접속에 1분 정도 걸린다.

설정은 `render.yaml` 과 `Dockerfile` 이다. 다시 만들어야 하면 Render 에 GitHub 계정으로 가입한 뒤 Dashboard → New → **Blueprint** → 이 저장소(`okeycsy/connect-link`)를 골라 Apply 한다.

## 개발

필요: JDK 17 이상, Node 22.

| 목적 | 명령 |
| --- | --- |
| 전체 빌드 + 테스트 + app.jar | `./gradlew build` → `backend/build/libs/app.jar` |
| 서버 실행(개발) | `./gradlew :backend:bootRun --args='--spring.profiles.active=local'` |
| 프론트 개발 서버 | `npm --prefix frontend run dev` (`/api` 는 8080 으로 프록시) |
| 서버 테스트 | `./gradlew :backend:test` |
| 서버 포맷 | `./gradlew :backend:spotlessApply` |
| 프론트 린트·테스트 | `npm --prefix frontend run lint`, `npm --prefix frontend test` |
| API 타입 생성 | `./gradlew :backend:test` 후 `npm --prefix frontend run gen:api` |
| E2E | `./gradlew build` 후 `npm --prefix e2e ci && npm --prefix e2e test` |

서버 API 를 바꾸면 `gen:api` 로 `frontend/src/api/schema.d.ts` 를 다시 만들어 같은 PR 에 넣는다. CI 가 생성물이 최신인지 검사한다.

## 구조

```
backend/   Spring Boot 2.7 (Java 17) · REST API · JPA + H2 · Flyway · springdoc
frontend/  React + TypeScript + Vite · Mantine · TanStack Query · openapi-fetch
e2e/       Playwright (app.jar 를 demo 프로필로 띄워 실행)
docs/      기획·설계 문서
```

주소 구조는 `/{업무 영역}/{기능}` 이다. 왼쪽 레일이 업무 영역(조회 범위)을, 보조 메뉴가 기능을 고른다.
