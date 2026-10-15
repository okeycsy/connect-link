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
| `/swagger-ui.html` | API 문서 |
| `/api/v1/health` | 서버 상태 |

데모 계정은 계정 모듈이 붙으면 이 표에 적는다.

### 외부 미리보기 (Render)

내부망 시연과 별개로, 여러 기기에서 같은 주소로 보려고 [Render](https://render.com) 무료 웹 서비스에 올린다. `render.yaml` 과 `Dockerfile` 이 그 설정이다. 올라가는 데이터는 샘플(가상)뿐이다.

1. Render 에 GitHub 계정으로 가입한다.
2. Dashboard → New → **Blueprint** → 이 저장소(`okeycsy/connect-link`)를 고르고 Apply 한다. `render.yaml` 을 읽어 서비스 `connect-link` 가 만들어진다.
3. 첫 빌드(5~10분)가 끝나면 `https://connect-link-xxxx.onrender.com` 같은 주소가 생긴다. 이후 `main` 에 머지될 때마다 자동으로 다시 배포된다.

무료 등급은 15분 동안 요청이 없으면 잠들고, 다음 접속에 1분 정도 걸린다.

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
