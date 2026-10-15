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
| `/login` | 로그인 (계정 모듈 연결 전) |
| `/{업무}/notifications` | 알림 센터 |
| `/{업무}/me` | 내 정보 (읽기 전용. 연락처 수정·탈퇴는 계정·연락처 모듈 후) |
| `/{업무}/demo/outbox` | 발송함 (데모 도구) |
| `/{업무}/demo/states` | 상태 화면 모음 (데모 도구: 빈 화면·오류·권한 없음·주소 없음·내부망 전용) |
| `/{업무}/demo/attachments` | 첨부파일 (데모 도구: 공통 첨부 부품으로 올리기·내려받기·지우기) |
| `/{업무}/admin` | 관리 허브 (기준정보, 가입 승인·정기 변경 주간·알림 대상은 준비 중) |
| `/{업무}/admin/organizations` | 기관·업무 기준정보 (목록 + 오른쪽 상세 패널, 변경 이력 탭) |
| `/swagger-ui.html` | API 문서 |
| `/api/v1/health` | 서버 상태 |

`demo`·`local` 프로필에서는 로그인 없이 데모 관리자(`admin@kftc.example.com`, 결제원, 관리자)로 접속된다. 공통 화면을 어드민 전제로 먼저 만들기로 한 결정이며, 역할별 데모 계정은 계정 모듈이 붙으면 이 표에 적는다. 상단 바의 **데모** 메뉴에서 발송함(메일·매터모스트 기록), 상태 화면 모음, 첨부파일, 망 전환(내부망 ↔ 인터넷망, 서버가 `X-Network-Zone` 헤더를 믿는 것은 `demo`·`local` 뿐)을 쓸 수 있다. 전역 검색은 상단 검색 상자나 **Ctrl K** 로 연다.

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
