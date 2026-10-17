# CLAUDE.md — 연결고리 (connect-link)

이 파일은 이 저장소에서 일하는 Claude가 매번 같은 방식으로 같은 품질의 결과물을 내기 위한 규칙이다.
여기 적힌 내용이 다른 습관이나 일반적인 관행보다 우선한다. 규칙을 바꿔야 할 것 같으면 바꾸지 말고 사용자에게 먼저 묻는다.

## 1. 프로젝트 개요

- **무엇**: 결제원 담당자와 참가기관 담당자를 잇는 업무 포털의 프로토타입.
- **범위 업무(4개)**: 계좌통합관리, 자동이체통합관리, 대출이동, CMS.
- **기능 모듈(4개)**
  - 가. 참가기관 담당자 연락처 관리
  - 나. 참가기관 의견 회신 취합 관리
  - 다. 기관 테스트 수행 및 결과 관리
  - 라. 참가기관 일시중단 관리
  - 공통: 계정(가입, 이메일 인증, 가입 승인, 탈퇴), 알림(메일, 매터모스트)
- **목표 상태**: "동작만 하는 데모"가 아니라 **실제 산출물에 가까운 아키텍처, 데이터만 샘플**인 상태. 프로토타입 이후 이 코드를 바탕으로 바로 본 개발이 이어진다. 임시방편 코드나 화면 전용 가짜 로직을 넣지 않는다.
- **요구사항 원문**: 노션 "요구사항/기능 명세" 페이지(비공개, Notion 커넥터로 읽는다). 명세에 없는 동작은 추측해서 만들지 않는다. 13절을 따른다.

## 2. 절대 규칙

1. **공개 저장소다.** 실제 기관명, 실명, 전화번호, 이메일, 내부 시스템명·IP·호스트명, 업무 자료를 코드·샘플 데이터·커밋·이슈 어디에도 넣지 않는다. 샘플은 전부 가상 데이터로 만든다(예: "가나은행", "홍길동", 010-0000-0000, example.com).
2. **실행 중 외부 네트워크 호출 금지.** 망분리된 내부망에서 시연한다. CDN, 웹폰트, 외부 API, 분석·텔레메트리 스크립트를 쓰지 않는다. 화면에 필요한 모든 자원은 빌드 결과(app.jar)에 포함한다.
3. **별도 DB 설치 없이 실행된다.** 기본 DB는 H2 메모리 모드다. `java -jar app.jar` 하나로 서버와 화면이 함께 뜬다.
4. **Java 17 + Spring Boot 2.7에서 돌아야 한다.** 내부망 서버가 Java 17이다. 컴파일 대상은 Java 17이며, 17 이후 문법과 API를 쓰지 않는다. Spring Boot 3 이상 전용 기능도 쓰지 않는다. 패키지는 `javax.*`를 쓰고 `jakarta.*`는 쓰지 않으며, `ProblemDetail`, `RestClient`, `@HttpExchange`도 쓰지 않는다.
5. **기술 스택은 3절로 고정한다.** 새 라이브러리·프레임워크 추가, 메이저 버전 변경은 사용자 확인 후에만 한다.
6. **권한은 서버가 지킨다.** 권한이 없는 데이터는 API가 내려보내지 않는다. 화면에서 숨기는 것만으로 권한을 구현하지 않는다. 마스킹도 서버에서 한다.
7. **언어**: 사용자와의 대화, 화면 문구, 커밋 메시지, 주석은 한국어. 코드 식별자(클래스, 변수, API 경로, DB 컬럼)는 영어이며 4절 용어집을 따른다.

## 3. 기술 스택

정확한 버전은 빌드 파일(`build.gradle.kts`, `package.json`)이 기준이다. 버전을 올리는 작업은 따로 요청받았을 때만 한다.
서버 라이브러리 버전은 Spring Boot 2.7의 의존성 관리(BOM)에 맡긴다. 개별 버전을 직접 덮어쓰는 것은 보안 패치 목적일 때만, 사용자 확인 후에 한다.

| 구분 | 선택 | 비고 |
|---|---|---|
| 서버 언어 | Java 17 (toolchain 17) | Lombok 사용 안 함. DTO는 record |
| 서버 프레임워크 | Spring Boot 2.7.x (Web MVC, Security 5.7, Data JPA, Validation) | 2.x 최종 라인. Security 설정은 `SecurityFilterChain` 빈으로 하고 `WebSecurityConfigurerAdapter`는 쓰지 않는다 |
| DB | H2 (메모리 모드) | 운영 전환 시 접속 설정만 교체. 버전은 Spring Boot가 관리한다 |
| 스키마 관리 | Flyway (Spring Boot 관리 버전) | 이식성 있는 표준 SQL로 작성 |
| API 문서 | springdoc-openapi 1.x (`springdoc-openapi-ui`, OpenAPI 3, Swagger UI) | 프론트 타입 생성의 원천. 2.x 이상은 Boot 3 전용이라 쓰지 않는다 |
| 엑셀 | Apache POI | 서버에서 xlsx 생성 |
| 서버 테스트 | JUnit 5, Spring Boot Test, MockMvc | |
| 서버 포맷 | Spotless + google-java-format | |
| 빌드 | Gradle (Kotlin DSL) 멀티 프로젝트 | 프론트 빌드까지 Gradle이 묶는다 |
| 프론트 | React + TypeScript + Vite | |
| UI | Mantine (core, form, dates, notifications) | 다른 UI 라이브러리, CSS 프레임워크 금지 |
| 라우팅 | React Router | |
| 서버 상태 | TanStack Query | |
| API 클라이언트 | openapi-typescript + openapi-fetch | 손으로 API 타입을 쓰지 않는다 |
| 프론트 테스트 | Vitest + Testing Library, Playwright(E2E) | |
| 프론트 품질 | ESLint + Prettier | |
| CI | GitHub Actions | 빌드·테스트 후 app.jar를 아티팩트로 업로드 |

## 4. 용어집 (이름은 반드시 이것을 쓴다)

| 한국어 | 코드 이름 | 비고 |
|---|---|---|
| 기관 (상위 개념) | `Organization` | `type`: `OrganizationType` = `KFTC` / `FINANCE_ORGANIZATION` / `USE_ORGANIZATION` |
| 결제원 | `KFTC` (기관 구분) | |
| 참가기관 | `FINANCE_ORGANIZATION` (기관 구분), `FinanceOrganization` | 은행, 증권사 등 금융회사 |
| 이용기관 | `USE_ORGANIZATION` (기관 구분), `UseOrganization` | 금융회사가 아닌 일반 회사(주로 결제원의 고객). 이용기관 담당자의 권한과 기능은 명세에 정해지기 전까지 구현하지 않는다 |
| 계정 | `UserAccount` | 로그인 주체. 식별 키는 회사 이메일(소문자). 상태 `AccountStatus` = `PENDING_APPROVAL`(승인 대기), `ACTIVE`(사용 중), `REJECTED`(반려), `WITHDRAWN`(탈퇴). 담당 업무는 `UserAccountBusiness`, 전산 세부 직무는 `UserAccountItRole` |
| 회사 이메일 인증 | `EmailVerification` | 가입 때 숫자 6자리 코드 확인 한 건. 맞으면 가입 신청에 한 번 쓰는 인증 토큰을 준다. 설정 `app.account.verification.*`. 가입 신청은 `SignUpService` 가 계정을 승인 대기로 만든다 |
| 가입 승인 | `SignUpReviewService`, `SignUpReviewAccessPolicy` | 승인 대기 계정을 승인(`ACTIVE`)하거나 사유를 적어 반려(`REJECTED`)한다. 처리할 수 있는 사람은 관리자와 신청 업무를 맡은 결제원 담당자. API `/api/v1/sign-ups`(목록·건수·상세, `/{id}/approval`, `/{id}/rejection`), 화면 `/{업무}/admin/sign-ups` |
| 탈퇴 | `WithdrawalService`, `WithdrawalAccessPolicy`, `WithdrawalCheck`, `WithdrawalBlocker`, `AccountWithdrawnEvent` | 본인이 비밀번호를 다시 넣어 탈퇴한다(API `/api/v1/me/withdrawal`, 화면 내 정보). 결제원 담당자는 맡은 업무의 참가기관 담당자를 사유를 적어 탈퇴 처리한다(API `/api/v1/accounts/{id}/withdrawal`, 화면 가입 승인 상세). 계정은 `WITHDRAWN` 으로 남고 개인 정보는 지운다. 막는 규칙은 기능 모듈이 `WithdrawalCheck` 빈 하나로 더하고(예: 후임자 규칙), 탈퇴 뒤 자기 데이터는 `AccountWithdrawnEvent` 를 받아 지운다 |
| 담당자 연락처 | `Contact` | 계정이 없는 담당자(후임자 등)도 존재 가능 |
| 기관 업무 참여 | `OrganizationBusiness` | 기관이 참여하는 업무 영역 한 건. 레일별 조회 범위의 기준. 테이블 `organization_business` |
| 업무 | `Business` | `ACCOUNT_INFO`(계좌통합관리), `AUTO_TRANSFER`(자동이체통합관리), `LOAN_TRANSFER`(대출이동), `CMS` |
| 세부업무 | `subBusiness` | 자유 텍스트 |
| 업권 | `FinanceSector` | 참가기관의 금융 업권. `BANK`(은행), `SECURITIES`(증권사), `MICROFINANCE`(서민금융기관: 저축은행·상호금융 등), `INSURANCE`(보험사). 넷에 들지 않는 참가기관(카드사 등)은 비워 둔다. 이용기관에는 없다. 기관 기준정보 칼럼 `organization.sector`, 선택지 API `/api/v1/finance-sectors`. 의견 요청 대상을 업권별로 고를 때 쓴다 |
| 직무 | `JobRole` | `BUSINESS`(현업), `IT`(전산) |
| 전산 세부 직무 | `ItRole` (복수 선택) | `DEV`, `OPS`, `INFRA`, `EXTERNAL_LINK`(대외계), `CONTROL_ROOM`(상황실) |
| 주/부담당 | `ContactRank` | `PRIMARY`, `SECONDARY` |
| 최근 확인일 | `lastConfirmedAt` | 수정·확인 시 갱신 |
| 정기 변경(현행화) 주간 | `UpdateCampaign` | |
| 의견 요청 | `OpinionRequest` | 상태 `OpinionRequestStatus` = `DRAFT`(작성중), `OPEN`(회신중), `CLOSED`(마감). 회신중 요청을 취소하면 `CLOSED` + `cancelled`. 권한 `OpinionAccessPolicy`. API `/api/v1/opinion-requests`, 화면 `/{업무}/opinions` |
| 질문 | `OpinionQuestion` | 의견 요청에 넣는 질문 하나. 유형 `OpinionQuestionType` = `SINGLE_CHOICE`(객관식), `MULTIPLE_CHOICE`(체크박스), `TEXT`(주관식), 필수 여부. 선택지는 `OpinionQuestionOption`. 작성중일 때만 고친다 |
| 질문 답 | `OpinionReplyAnswer` | 기관 회신의 답 한 칸. 회신처럼 제출한 답(`SUBMITTED`)과 임시저장한 답(`DRAFT`)을 따로 둔다. 객관식은 고른 선택지마다, 주관식은 질문마다 한 행 |
| 대상 기관 | `OpinionTarget` | 요청의 대상 기관 한 곳. 회신 담당자는 `OpinionTargetRecipient`(고를 때의 이름·이메일·세부업무를 남긴다) |
| 리마인드 이력 | `OpinionReminder` | 미회신 기관에 보낸 리마인드 메일 한 번 |
| 회신 현황·취합표 | `OpinionCollationService` | 회신율, 유형별 기관, 기관별 취합표와 엑셀(`OpinionExcel`, 내부망 전용) |
| 기관 회신 | `OpinionReply` | 대상 기관 하나에 하나. 유형 `OpinionReplyType` = `AGREE`(동의), `CONDITIONAL`(조건부 동의), `DISAGREE`(이견). 상태 `OpinionReplyStatus` = `DRAFT`(임시저장), `SUBMITTED`(제출), `REVISION_REQUESTED`(수정 요청), `REVISING`(수정 중) |
| 기관 테스트(페이지) | `TestPlan` | 단위업무/개발 건별 |
| 테스트 요구사항 | `TestItem` | |
| 테스트 결과 | `TestResult` | `PASSED`(정상), `FAILED`(오류), `IN_PROGRESS`(수행중), `NOT_STARTED`(미수행) |
| 일시중단 | `Suspension` | 상태 `CONFIRMED`(확정), `PENDING_REVIEW`(검토 대기), `IN_REVIEW`(검토 중), `COMPLETED`(완료) |
| 알림 | `Notification` | 알림 센터 항목. 종류 `NotificationType` (`SIGNUP_REQUESTED`, `SIGNUP_APPROVED`, `CONTACT_CHANGE_REQUESTED`, `CONTACT_RECONFIRM_DUE`, `OPINION_REQUEST_OPENED`, `OPINION_REQUEST_CHANGED`, `OPINION_REPLY_DUE`, `OPINION_REPLY_REVISION`, `TEST_PLAN_OPENED`, `SUSPENSION_CHANGED`, `SYSTEM`) |
| 알림 채널 | `NotificationChannel` | `EMAIL`, `MATTERMOST` |
| 발송함 | `NotificationOutbox` | 프로토타입에서 실제 발송 대신 기록. 상태 `OutboxStatus` = `PENDING`(발송 대기), `SENT`, `FAILED` |
| 로그인 사용자 | `CurrentUser` | Service 권한 판단에 넘기는 값. 로그인할 때 계정에서 만들어 세션에 둔다. 업무 영역(레일) 조회 범위는 `BusinessAccessPolicy`(관리자는 전 업무, 그 밖에는 담당 업무). 테스트는 `TestUsers` |
| 내부망/인터넷망 | `NetworkZone` | `INTERNAL`, `EXTERNAL`. 판단은 `NetworkZoneResolver`(헤더 `X-Network-Zone` 은 local·demo 만, 운영은 `app.network.internal-cidrs`). 내부망 전용 위반은 403 `INTERNAL_ONLY` |
| 첨부파일 | `Attachment` | 대상은 `AttachmentOwnerType`(`OPINION_REQUEST`, `OPINION_REPLY`, `SUSPENSION`, `DEMO`) + `ownerId`. 파일 저장소 `FileStorage` |
| 변경 이력 | `ChangeHistory` | 대상은 `HistoryTargetType`(`ORGANIZATION`, `CONTACT`, `OPINION_REQUEST`, `OPINION_REPLY`, `TEST_PLAN`, `SUSPENSION`) + `targetId`. 종류 `ChangeAction` = `CREATED`/`UPDATED`/`DELETED`, 항목 `FieldChange`(field, label, before, after) |
| 전역 검색 묶음 | `SearchCategory` | `ORGANIZATION`, `CONTACT`, `OPINION_REQUEST`, `TEST_PLAN`, `SUSPENSION`. 기능 모듈은 `SearchProvider` 빈 하나로 자기 묶음을 채운다 |

용어집에 없는 개념이 필요하면 이름을 정해 이 표에 추가하는 것까지 같은 작업에 포함한다.

## 5. 저장소 구조

```
connect-link/
├── CLAUDE.md
├── README.md                 # 실행 방법, 데모 계정
├── settings.gradle.kts       # backend, frontend 포함
├── build.gradle.kts
├── backend/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/connectlink/
│       │   ├── ConnectLinkApplication.java
│       │   ├── common/        # 공통 설정, 오류 처리, 감사(auditing), 망 구분
│       │   ├── auth/          # 로그인, 세션, 보안 설정
│       │   ├── account/       # 가입, 이메일 인증, 가입 승인, 탈퇴
│       │   ├── organization/  # 기관(결제원·참가기관·이용기관), 업무 기준정보
│       │   ├── home/          # 홈 "지금 할 일": 카드 자리(HomeCardKey)와 기여자 계약(HomeCardContributor), 샘플 카드(home/sample)
│       │   ├── search/        # 전역 검색: 묶음(SearchCategory)과 제공자 계약(SearchProvider), 샘플 제공자(search/sample)
│       │   ├── contact/       # 가. 연락처 관리
│       │   ├── opinion/       # 나. 의견 회신 취합
│       │   ├── testplan/      # 다. 기관 테스트
│       │   ├── suspension/    # 라. 일시중단
│       │   └── notification/  # 알림 인터페이스와 발송함
│       ├── main/resources/
│       │   ├── application.yml, application-local.yml, application-demo.yml
│       │   ├── db/migration/  # Flyway 스키마 V1__*.sql …
│       │   └── db/seed/       # 샘플 데이터 (local, demo 프로필에서만)
│       └── test/java/com/connectlink/...
├── frontend/
│   ├── CLAUDE.md             # 디자인 시스템(토큰, 컴포넌트, 규칙)
│   ├── package.json
│   └── src/
│       ├── api/               # schema.d.ts(생성물), client.ts
│       ├── app/               # 라우터, 레이아웃(AppShell), 테마, 인증 상태
│       ├── features/
│       │   ├── account/ contact/ opinion/ testplan/ suspension/ notification/ attachment/ history/
│       │   │   └── (각각) pages/, components/, hooks.ts(Query 훅), *.test.tsx
│       ├── shared/            # 공통 컴포넌트(PageHeader, BrandMark, OrgMonogram), 포맷터(날짜, 전화번호), list/(목록 패턴), state/(빈·오류·권한 없음·내부망 전용 안내)
│       └── main.tsx
├── e2e/                      # Playwright 시나리오
├── docs/
│   └── decisions.md          # 명세에 없던 결정 기록 (13절)
└── .github/workflows/
```

새 기능은 이 구조 안에 넣는다. 모듈(폴더)을 새로 만들어야 하면 먼저 묻는다.

## 6. 서버 규칙

- **계층**: `Controller → Service → Repository`. Controller는 요청 검증과 DTO 변환만 하고, 업무 로직과 권한 판단은 Service에 둔다.
- **패키지는 기능별**: 모듈 안에 `XxxController`, `XxxService`, `XxxRepository`, `Xxx`(엔티티), `dto/`, `XxxAccessPolicy`를 둔다. 다른 모듈의 Repository를 직접 쓰지 않고 그 모듈의 Service를 쓴다.
- **엔티티는 API로 내보내지 않는다.** 요청·응답은 항상 record DTO다.
- **권한**: 모듈마다 `XxxAccessPolicy` 한 곳에서 조회·수정 범위를 판단한다(예: 결제원 운영·상황실·대외계는 전 업무 조회, 기관 담당자는 자기 기관 같은 업무만). 규칙 하나마다 허용과 거부 테스트를 하나씩 둔다.
- **망 구분**: 내부망 전용 기능(연락처 수정·삭제, 검색·필터, 엑셀 다운로드 등)은 `NetworkZoneResolver`로만 판단한다. 운영에서는 설정된 내부 IP 대역으로 판단하고, `demo` 프로필에서는 화면의 망 전환 토글이 보내는 헤더를 허용한다.
- **인증**: Spring Security 세션(쿠키) 방식, CSRF 켬(쿠키 토큰). 비밀번호는 BCrypt.
- **가입 흐름**: 가입 → 회사 이메일 인증 코드 발송(발송함에 기록) → 코드 확인 → 해당 업무 결제원 담당자 승인 → 승인 메일(발송함). 승인 전 계정은 로그인할 수 없다.
- **알림**: `NotificationSender` 인터페이스를 통해서만 보낸다. 프로토타입 구현은 `OutboxNotificationSender`(DB 발송함에 기록)다. 업무 코드에서 메일이나 웹훅을 직접 호출하지 않는다.
- **파일 첨부**: `attachment` 모듈의 `AttachmentService` 를 쓴다. 파일 내용은 `FileStorage` 인터페이스를 통해서만 저장한다(프로토타입 구현은 임시 디렉터리 `TempDirFileStorage`). 기능 모듈은 자기 `AttachmentOwnerType` 에 대한 `AttachmentOwnerAccess` 빈 하나로 보기·올리기 권한을 알려 준다. 빈이 없는 대상은 아무도 쓸 수 없다.
- **변경 이력**: 변경 전·후를 남겨야 하는 수정은 같은 트랜잭션 안에서 `history` 모듈의 `ChangeHistoryService.record` 를 부른다. 항목 비교는 `FieldChanges.compare()` 로 만들고, 바뀐 항목이 없는 수정은 남지 않는다. 기능 모듈은 자기 `HistoryTargetType` 에 대한 `HistoryTargetAccess` 빈 하나로 조회 권한과 마스킹(`redact`)을 정한다.
- **예약 작업**: 6개월 미확인 알림, 전 영업일 일시중단 취합 같은 정기 작업은 `@Scheduled`로 만들고, `demo` 프로필에서는 시연용 즉시 실행 API를 함께 둔다.
- **API 규약**
  - 경로 `/api/v1/{복수형-kebab-case}` (예: `/api/v1/opinion-requests/{id}/replies`).
  - JSON 필드는 camelCase. enum은 대문자 스네이크 문자열. ID는 `Long`.
  - 날짜·시간은 ISO-8601이며 서버 시간대는 `Asia/Seoul`.
  - 목록은 `page`(0부터), `size` 파라미터와 공통 `PageResponse<T>` 응답.
  - 오류는 공통 `ErrorResponse` record로 통일한다. 필드는 RFC 7807 형식(`type`, `title`, `status`, `detail`, `instance`)에 `code`와 필드별 검증 메시지 `fieldErrors`를 더한다. `@RestControllerAdvice` 한 곳에서 만든다.
  - 모든 Controller 메서드에 springdoc 어노테이션으로 요약(한국어)을 단다.
- **DB 규약**: 테이블·컬럼은 snake_case 단수형(`opinion_request`). 모든 테이블에 `created_at`, `created_by`, `updated_at`, `updated_by`(JPA Auditing). 스키마 변경은 새 Flyway 파일로만 하고 이미 커밋된 마이그레이션은 고치지 않는다. H2 전용 문법을 쓰지 않는다.
- **프로필**: `local`(개발, H2 콘솔 켬), `demo`(내부망 시연용 기본값, 샘플 데이터, 데모 로그인, 망 전환 토글, 발송함 화면). `app.jar`의 기본 프로필은 `demo`.

## 7. 프론트 규칙

- 화면은 `features/{모듈}/pages`에, 그 기능 전용 컴포넌트는 같은 모듈의 `components`에 둔다. 두 모듈 이상에서 쓰면 `shared`로 옮긴다.
- **API 호출은 `src/api/client.ts`(openapi-fetch)와 각 모듈의 `hooks.ts`(TanStack Query)로만** 한다. 컴포넌트에서 `fetch`를 직접 쓰지 않는다.
- `src/api/schema.d.ts`는 생성물이다. 손으로 고치지 않는다(8절).
- **디자인 시스템은 `frontend/CLAUDE.md`(B안 메신저형)를 따른다.** 색·글꼴·radius·앱 셸 치수·컴포넌트 모양과 금지 사항이 거기 있고, 화면 모양에 관해서는 그 문서가 우선한다.
- UI는 Mantine 컴포넌트와 Mantine 테마(`app/theme.ts`)만 쓴다. 인라인 스타일과 별도 CSS 파일은 Mantine으로 안 될 때만 최소로 쓴다.
- 글꼴은 Pretendard Variable 을 npm 패키지(`pretendard`)로 번들해서 쓴다. 외부에서 웹폰트를 불러오지 않는다. 글꼴은 `frontend/src/app/fonts.css` 의 `--cl-font-sans` 한 곳에서만 정하고, 바꾸는 방법은 `frontend/CLAUDE.md` 3절에 있다. 숫자 서체(Geist) 도입은 확인 대기.
- 화면 구성: Mantine `AppShell`, 왼쪽 레일(업무 영역: 계좌통합관리 / 자동이체통합관리 / 대출이동 / CMS) + 사이드바(기능: 홈 / 연락처 / 의견 요청 / 기관 테스트 / 일시중단 / 관리, 바로가기에 알림 센터), 상단 바에 전역 검색·알림·사용자 정보. 주소는 `/{업무 slug}/{기능}`. 디자인 토큰과 셸 치수는 `frontend/CLAUDE.md`, 화면 범위와 구조 결정은 `docs/design/common-screens.md`, 분류는 `docs/service-taxonomy.md`를 따른다. `demo` 프로필이면 상단에 데모 도구(계정 전환, 망 전환, 발송함)를 보인다.
- 날짜 표기 `YYYY-MM-DD`, 일시 `YYYY-MM-DD HH:mm`. 전화번호 표기와 마스킹 형식은 `shared/format.ts` 한 곳에서만 처리한다(마스킹 판단 자체는 서버가 한다).
- 데스크톱 우선(1280px 기준), 태블릿 폭에서 깨지지 않게 한다.
- 화면 문구는 한국어로 컴포넌트에 직접 쓴다. 다국어 처리는 하지 않는다.
- 폼은 `@mantine/form`, 검증 메시지는 서버 오류의 필드 메시지를 그대로 보여준다.

## 8. API 계약 흐름

1. 서버 Controller와 DTO를 먼저 만든다.
2. `npm --prefix frontend run gen:api`로 서버의 OpenAPI 문서에서 `schema.d.ts`를 다시 만든다.
3. 서버 변경과 `schema.d.ts` 변경을 같은 커밋(또는 같은 PR)에 넣는다.
4. CI는 생성물이 최신이 아니면 실패한다.

## 9. 명령어

(스캐폴딩 이후 기준. 명령어를 바꾸면 이 절도 함께 고친다.)

| 목적 | 명령 |
|---|---|
| 전체 빌드 + 테스트 + app.jar | `./gradlew build` → `backend/build/libs/app.jar` |
| 서버 실행(개발) | `./gradlew :backend:bootRun --args='--spring.profiles.active=local'` |
| 프론트 개발 서버 | `npm --prefix frontend run dev` (`/api`는 8080으로 프록시) |
| 서버 테스트 | `./gradlew :backend:test` |
| 서버 포맷 | `./gradlew spotlessApply` |
| 프론트 린트·테스트 | `npm --prefix frontend run lint`, `npm --prefix frontend test` |
| API 타입 생성 | `npm --prefix frontend run gen:api` |
| E2E | `npm --prefix e2e test` (app.jar를 demo 프로필로 띄워서 실행) |
| 시연 실행 | `java -jar app.jar` → 브라우저에서 `http://localhost:8080` |

## 10. 테스트와 완료 기준

작업은 아래를 모두 만족해야 완료다.

- `./gradlew build`가 통과한다(서버 테스트, 프론트 린트·테스트, 포맷 검사 포함).
- 새 API에는 MockMvc 테스트가 있다. 권한 규칙에는 허용과 거부 테스트가 모두 있다.
- 새 화면의 주요 흐름에는 Playwright 시나리오가 있다.
- 빌드 결과에 외부 URL을 불러오는 코드가 없다(CI가 검사한다).
- 샘플 데이터가 바뀌었으면 README의 데모 계정 표와 맞는다.
- 화면을 바꿨다면 Playwright로 찍은 캡처를 사용자에게 보여준다. 클라우드 세션은 github.io에 접속할 수 없고, 이 앱은 서버가 필요해서 Pages로 미리 볼 수 없다.

## 11. Git과 작업 단위

- `main`에 직접 push하지 않는다. 작업 브랜치에서 PR을 만들고 사용자가 병합한다.
- PR 하나에는 기능 하나(또는 한 모듈의 한 단계)만 담는다.
- 개발 사항이나 의미 있는 변경이 생길 때마다 커밋한다.
- 커밋 메시지 제목은 `<prefix>: <한국어 한 줄 요약>` 형식으로 쓴다. prefix는 작업 중인 Claude 프로젝트 이름에서 '_연결고리' 바로 앞의 단어이며, 프로젝트마다 다르다. 실제 prefix는 각 프로젝트의 지시사항에 적혀 있으니 그것을 따른다(예: `공통: 연락처 조회 API와 권한 테스트 추가`).
- 생성물(`schema.d.ts`)을 빼면 커밋에 빌드 결과물(`build/`, `dist/`, `node_modules/`)을 넣지 않는다.
- CI가 만든 `app.jar`는 GitHub Actions 실행 페이지의 아티팩트에서 내려받아 내부망으로 옮긴다.

## 12. 샘플 데이터

- 4개 업무, 결제원 1곳, 가상 참가기관 6곳 이상, 업무·직무 조합별 담당자를 고르게 넣어 권한 차이가 시연되도록 한다.
- 데모 계정은 역할별로 만든다(결제원 현업, 결제원 운영, 결제원 상황실, 참가기관 담당자 등). 비밀번호는 데모 공통값이며 README에 적는다. 운영 프로필에서는 데모 계정이 생기지 않는다.
- 이력, 미확인 6개월 경과, 회신중·마감 요청, 진행 중 테스트, 현재·예정 일시중단 같은 상태가 화면마다 하나 이상 보이게 한다.

## 13. 명세에 없거나 애매한 것

- 명세가 정하지 않은 동작은 **사용자에게 묻는다**. 묻기 어려운 작은 결정이면 합리적인 기본값으로 진행하고 `docs/decisions.md`에 날짜, 결정, 이유를 한 줄씩 남긴다.
- 같은 질문이 다시 나오면 `docs/decisions.md`를 먼저 확인한다.
- 명세에 남아 있는 미결 사항(예: 일시중단 취합 전달 양식, 결제원 담당자 업무분장 관리)은 임의로 구현하지 않는다.
