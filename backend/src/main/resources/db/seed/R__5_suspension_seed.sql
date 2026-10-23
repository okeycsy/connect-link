-- 일시중단 샘플. 전부 가상 데이터다. local, demo 프로필(과 테스트)에서만 읽는다.
-- 기관(R__1)·계정(R__2) 시드 뒤에 돈다. 시각은 오늘 0시 기준 분(start_min, end_min)이라 시연 때 늘 진행 중·예정·지난 건이 함께 보인다.
-- 진행 중(확정), 내일 시작(확정, 전 영업일 취합에 들어감), 예정(검토 대기·검토 중), 지난(완료)을 업무마다 섞었다.
-- 한 줄이 한 건이다. business 는 대상 업무(기준정보, R__5_0) 이름이다. 둘째 대상 업무(business2)와 둘째 담당자(contact2_*)가 있는 건은
-- 여러 대상 업무를 멈추거나 담당자가 여럿이다. 관련 레일 업무(suspension_business)는 고른 대상 업무의 관련 업무를 모은 것이다.
-- 한 줄을 일시중단·업무·담당자 세 테이블로 나누려고 잠깐 쓰는 표(suspension_seed)에 먼저 넣는다.
DELETE FROM suspension_digest;
DELETE FROM suspension_reviewer;
DELETE FROM suspension_contact;
DELETE FROM suspension_target_work;
DELETE FROM suspension_business;
DELETE FROM suspension;

CREATE TABLE suspension_seed (
    business            VARCHAR(100)   NOT NULL,
    business2           VARCHAR(100),
    org_code            VARCHAR(20)    NOT NULL,
    start_min           INT            NOT NULL,
    end_min             INT            NOT NULL,
    reason              VARCHAR(2000)  NOT NULL,
    kftc_action         VARCHAR(2000),
    contact_name        VARCHAR(50)    NOT NULL,
    contact_phone       VARCHAR(30)    NOT NULL,
    contact_email       VARCHAR(200),
    contact2_name       VARCHAR(50),
    contact2_phone      VARCHAR(30),
    contact2_email      VARCHAR(200),
    extra_note          VARCHAR(2000),
    status              VARCHAR(20)    NOT NULL,
    registrant_email    VARCHAR(200)   NOT NULL,
    registrant_name     VARCHAR(50)    NOT NULL,
    registrant_kftc     BOOLEAN        NOT NULL,
    handover            VARCHAR(2000),
    confirmed_by_email  VARCHAR(200),
    confirmed_by_name   VARCHAR(50)
);

INSERT INTO suspension_seed
SELECT s.business, CAST(NULL AS VARCHAR(100)), s.org_code, s.start_min, s.end_min, s.reason, s.kftc_action,
       s.contact_name, s.contact_phone, s.contact_email, CAST(NULL AS VARCHAR(50)), CAST(NULL AS VARCHAR(30)), CAST(NULL AS VARCHAR(200)),
       s.extra_note, s.status, s.registrant_email, s.registrant_name, s.registrant_kftc, s.handover, s.confirmed_by_email, s.confirmed_by_name
FROM (VALUES
  -- 계좌통합관리
  ('계좌통합관리', '004', 30, 1800,
   '계정계 시스템 증설 작업으로 계좌정보 통합조회 응답이 중단됩니다.', '작업 시간 동안 KB국민은행 조회 요청을 차단하고 고객 안내 문구를 띄웁니다.',
   '김민준', '010-0000-2101', 'kim.minjun@kb.example.com', CAST(NULL AS VARCHAR(2000)),
   'CONFIRMED', 'kim.minjun@kb.example.com', '김민준', FALSE,
   '운영팀이 00:30 에 KB국민은행 조회를 차단했습니다. 재개 연락이 오면 차단을 풀고 상황실에 알립니다.', 'kim.ops@kftc.example.com', '김태호'),
  ('계좌통합관리', '088', 2820, 3000,
   '인터넷뱅킹 서버 교체로 내 계좌 한눈에 연계가 끊깁니다.', '신한은행 연계 차단, 오류 문의는 신한은행 담당자에게 안내',
   '박지훈', '010-0000-3103', NULL, '작업이 길어지면 연장 공문을 다시 올리겠습니다.',
   'PENDING_REVIEW', 'park.jihoon@shinhan.example.com', '박지훈', FALSE, CAST(NULL AS VARCHAR(2000)), CAST(NULL AS VARCHAR(200)), CAST(NULL AS VARCHAR(50))),
  ('계좌통합관리', '240', 5640, 5730,
   'DB 정기 점검으로 안심차단 등록·해제가 중단됩니다.', '삼성증권 안심차단 요청을 대기열에 쌓았다가 재개 뒤 다시 보냅니다.',
   '최유진', '010-0000-4101', 'choi.yujin@samsung.example.com', NULL,
   'IN_REVIEW', 'choi.yujin@samsung.example.com', '최유진', FALSE, NULL, NULL, NULL),
  ('계좌통합관리', '050', 8700, 8940,
   '차세대 시스템 전환 리허설로 계좌정보 일괄해지가 중단됩니다.', '저축은행중앙회 해지 요청 차단, 리허설 끝나면 저축은행중앙회이 재개 연락',
   '윤서연', '010-0000-6101', 'yoon.seoyeon@fsb.example.com', '결제원 요청으로 등록합니다(기관 공문 접수).',
   'CONFIRMED', 'hong.sungmin@kftc.example.com', '홍성민', TRUE,
   '개발팀이 해지 전문 차단 설정을 미리 반영해 둡니다. 운영팀은 리허설 시작·종료 시각을 상황실에 공유합니다.', 'hong.sungmin@kftc.example.com', '홍성민'),
  ('계좌통합관리', '103', -13020, -12780,
   '전산센터 이전으로 계좌정보 통합조회가 중단되었습니다.', 'SBI저축은행 조회 차단',
   '정다은', '010-0000-5101', NULL, NULL,
   'COMPLETED', 'hong.sungmin@kftc.example.com', '홍성민', TRUE, '운영팀이 차단과 해제를 마쳤습니다.', 'kim.ops@kftc.example.com', '김태호'),
  ('계좌통합관리', '004', -56220, -56040,
   '재해복구 전환 훈련으로 조회가 중단되었습니다.', 'KB국민은행 조회 차단',
   '김민준', '010-0000-2101', 'kim.minjun@kb.example.com', NULL,
   'COMPLETED', 'kim.minjun@kb.example.com', '김민준', FALSE, '상황실이 훈련 시작·종료를 확인했습니다.', 'lee.control@kftc.example.com', '이수진'),
  -- 자동이체통합관리
  ('자동이체통합관리', '071', 1560, 1680,
   '우체국금융 전산 정기 점검으로 자동이체 조회가 중단됩니다.', '우체국 조회 요청 차단',
   '윤하은', '010-0000-8101', NULL, NULL,
   'CONFIRMED', 'hong.sungmin@kftc.example.com', '홍성민', TRUE, '운영팀이 02:00 에 차단하고 04:00 에 풉니다.', 'kim.ops@kftc.example.com', '김태호'),
  ('자동이체통합관리', '088', 4200, 4380,
   '대외계 회선 교체로 자동이체 해지 전문과 CMS 출금 전문 송수신이 중단됩니다.', '신한은행 해지·출금 전문 보류',
   '이준호', '010-0000-3101', 'lee.junho@shinhan.example.com', NULL,
   'PENDING_REVIEW', 'lee.junho@shinhan.example.com', '이준호', FALSE, NULL, NULL, NULL),
  ('자동이체통합관리', '240', -27960, -27780,
   '고객정보 시스템 점검으로 조회가 중단되었습니다.', '삼성증권 조회 차단',
   '최유진', '010-0000-4101', NULL, NULL,
   'COMPLETED', 'hong.sungmin@kftc.example.com', '홍성민', TRUE, '차단과 해제를 마쳤습니다.', 'kim.ops@kftc.example.com', '김태호'),
  -- CMS
  ('CMS', '088', 1560, 1680,
   '코어뱅킹 정기 점검으로 CMS 출금이체가 중단됩니다.', '신한은행 출금 요청을 작업 뒤 일괄 재처리',
   '이준호', '010-0000-3101', 'lee.junho@shinhan.example.com', '공문 붙임 참고 바랍니다.',
   'CONFIRMED', 'lee.junho@shinhan.example.com', '이준호', FALSE, 'CMS운영팀이 재처리 일정을 잡았습니다. 상황실은 04:00 정상화 여부를 확인합니다.', 'park.cms@kftc.example.com', '박하늘'),
  ('CMS', '020', 7080, 7320,
   '수납 시스템 교체로 CMS 출금이 중단됩니다.', '우리은행 출금 요청 보류',
   '정재원', '010-0000-9101', NULL, NULL,
   'PENDING_REVIEW', 'park.cms@kftc.example.com', '박하늘', TRUE, NULL, NULL, NULL),
  ('CMS', '071', -19620, -19500,
   '출금 시스템 작업으로 출금이 중단되었습니다.', '우체국 출금 보류',
   '윤하은', '010-0000-8101', NULL, NULL,
   'COMPLETED', 'park.cms@kftc.example.com', '박하늘', TRUE, '재처리를 마쳤습니다.', 'park.cms@kftc.example.com', '박하늘'),
  -- 대출이동
  ('대출이동', '090', 4260, 4440,
   '대출 원장 시스템 점검으로 대출이동 조회가 중단됩니다.', '카카오뱅크 대출이동 조회 차단',
   '오성훈', '010-0000-1201', NULL, NULL,
   'IN_REVIEW', 'kim.ops@kftc.example.com', '김태호', TRUE, NULL, NULL, NULL),
  ('대출이동', '020', -35940, -35760,
   '여신 시스템 교체로 대출이동이 중단되었습니다.', '우리은행 대출이동 차단',
   '정재원', '010-0000-9101', NULL, NULL,
   'COMPLETED', 'kim.ops@kftc.example.com', '김태호', TRUE, '차단과 해제를 마쳤습니다.', 'kim.ops@kftc.example.com', '김태호'),
  -- 레일과 무관한 대상 업무만 고른 건: 결제원 운영·상황실·대외계가 맡는다.
  ('타행환공동망', '004', 2940, 3060,
   '대외계 회선 이중화 작업으로 타행환과 전자금융공동망 거래가 잠시 끊깁니다.', '작업 시간 동안 KB국민은행 공동망 거래를 우회해 주세요.',
   '김민준', '010-0000-2101', 'kim.minjun@kb.example.com', NULL,
   'PENDING_REVIEW', 'kim.minjun@kb.example.com', '김민준', FALSE, NULL, NULL, NULL)
) AS s (business, org_code, start_min, end_min, reason, kftc_action, contact_name, contact_phone, contact_email, extra_note,
        status, registrant_email, registrant_name, registrant_kftc, handover, confirmed_by_email, confirmed_by_name);

-- 대외계 회선 교체는 자동이체와 CMS 를 함께 멈추고, 담당자가 둘이다.
UPDATE suspension_seed
SET business2 = 'CMS', contact2_name = '박지훈', contact2_phone = '010-0000-3103', contact2_email = 'park.jihoon@shinhan.example.com'
WHERE org_code = '088' AND end_min = 4380;
UPDATE suspension_seed SET business2 = '전자금융공동망' WHERE org_code = '004' AND end_min = 3060;
-- 계정계 증설은 담당자가 둘이다(현업, 전산).
UPDATE suspension_seed
SET contact2_name = '한예린', contact2_phone = '010-0000-2102', contact2_email = NULL
WHERE org_code = '004' AND end_min = 1800;

INSERT INTO suspension (organization_id, start_at, end_at, reason, kftc_action, extra_note,
                        status, registrant_email, registrant_name, registrant_kftc, handover, confirmed_at, confirmed_by_email, confirmed_by_name, completed_at,
                        created_at, created_by, updated_at, updated_by)
SELECT o.id,
       CAST(CURRENT_DATE AS TIMESTAMP) + s.start_min * INTERVAL '1' MINUTE,
       CAST(CURRENT_DATE AS TIMESTAMP) + s.end_min * INTERVAL '1' MINUTE,
       s.reason, s.kftc_action, s.extra_note,
       s.status, s.registrant_email, s.registrant_name, s.registrant_kftc, s.handover,
       CASE WHEN s.confirmed_by_email IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CAST(CURRENT_DATE AS TIMESTAMP) + (s.start_min - 1440) * INTERVAL '1' MINUTE END,
       s.confirmed_by_email, s.confirmed_by_name,
       CASE WHEN s.status = 'COMPLETED' THEN CAST(CURRENT_DATE AS TIMESTAMP) + (s.end_min + 1) * INTERVAL '1' MINUTE ELSE CAST(NULL AS TIMESTAMP) END,
       CAST(CURRENT_DATE AS TIMESTAMP) + (s.start_min - 4320) * INTERVAL '1' MINUTE, s.registrant_email,
       CAST(CURRENT_DATE AS TIMESTAMP) + (s.start_min - 2880) * INTERVAL '1' MINUTE, s.registrant_email
FROM suspension_seed s
JOIN organization o ON o.code = s.org_code;

-- 한 기관의 같은 종료 시각 건은 하나뿐이라 (기관, 종료 시각)으로 이어 붙인다.
INSERT INTO suspension_target_work (suspension_id, target_work_id, name, created_at, created_by, updated_at, updated_by)
SELECT su.id, w.id, w.name, su.created_at, su.created_by, su.updated_at, su.updated_by
FROM (SELECT org_code, end_min, business AS work FROM suspension_seed
      UNION ALL
      SELECT org_code, end_min, business2 FROM suspension_seed WHERE business2 IS NOT NULL) b
JOIN target_work w ON w.name = b.work
JOIN organization o ON o.code = b.org_code
JOIN suspension su ON su.organization_id = o.id
                  AND su.end_at = CAST(CURRENT_DATE AS TIMESTAMP) + b.end_min * INTERVAL '1' MINUTE;

INSERT INTO suspension_business (suspension_id, business, created_at, created_by, updated_at, updated_by)
SELECT DISTINCT stw.suspension_id, twb.business, su.created_at, su.created_by, su.updated_at, su.updated_by
FROM suspension_target_work stw
JOIN target_work_business twb ON twb.target_work_id = stw.target_work_id
JOIN suspension su ON su.id = stw.suspension_id;

INSERT INTO suspension_contact (suspension_id, sort_order, name, phone, email, created_at, created_by, updated_at, updated_by)
SELECT su.id, c.sort_order, c.name, c.phone, c.email, su.created_at, su.created_by, su.updated_at, su.updated_by
FROM (SELECT org_code, end_min, 0 AS sort_order, contact_name AS name, contact_phone AS phone, contact_email AS email FROM suspension_seed
      UNION ALL
      SELECT org_code, end_min, 1, contact2_name, contact2_phone, contact2_email FROM suspension_seed WHERE contact2_name IS NOT NULL) c
JOIN organization o ON o.code = c.org_code
JOIN suspension su ON su.organization_id = o.id
                  AND su.end_at = CAST(CURRENT_DATE AS TIMESTAMP) + c.end_min * INTERVAL '1' MINUTE;

DROP TABLE suspension_seed;

-- 확인을 시작한 결제원 담당자. 확정한 사람은 확인자에도 들어 있다.
INSERT INTO suspension_reviewer (suspension_id, email, name, role_label, started_at, created_at, created_by, updated_at, updated_by)
SELECT su.id, r.email, r.name, r.role_label, su.created_at + INTERVAL '1' HOUR, su.created_at + INTERVAL '1' HOUR, r.email, su.created_at + INTERVAL '1' HOUR, r.email
FROM (VALUES
  ('004',  'ACCOUNT_INFO',  1800,   'hong.sungmin@kftc.example.com', '홍성민', '현업'),
  ('004',  'ACCOUNT_INFO',  1800,   'kim.ops@kftc.example.com',      '김태호', '전산(운영)'),
  ('240',   'ACCOUNT_INFO',  5730,   'kim.ops@kftc.example.com',      '김태호', '전산(운영)'),
  ('050', 'ACCOUNT_INFO',  8940,   'hong.sungmin@kftc.example.com', '홍성민', '현업'),
  ('103',  'ACCOUNT_INFO',  -12780, 'kim.ops@kftc.example.com',      '김태호', '전산(운영)'),
  ('004',  'ACCOUNT_INFO',  -56040, 'lee.control@kftc.example.com',  '이수진', '전산(상황실)'),
  ('071', 'AUTO_TRANSFER', 1680,   'kim.ops@kftc.example.com',      '김태호', '전산(운영)'),
  ('240',   'AUTO_TRANSFER', -27780, 'kim.ops@kftc.example.com',      '김태호', '전산(운영)'),
  ('088',  'CMS',           1680,   'park.cms@kftc.example.com',     '박하늘', '현업'),
  ('071', 'CMS',           -19500, 'park.cms@kftc.example.com',     '박하늘', '현업'),
  ('090',  'LOAN_TRANSFER', 4440,   'lee.control@kftc.example.com',  '이수진', '전산(상황실)'),
  ('020',  'LOAN_TRANSFER', -35760, 'kim.ops@kftc.example.com',      '김태호', '전산(운영)')
) AS r (org_code, business, end_min, email, name, role_label)
JOIN organization o ON o.code = r.org_code
JOIN suspension su ON su.organization_id = o.id
                  AND su.end_at = CAST(CURRENT_DATE AS TIMESTAMP) + r.end_min * INTERVAL '1' MINUTE;

-- 글쓴이 직급은 샘플 계정의 직급을 그대로 쓴다.
UPDATE suspension
SET registrant_job_title = (SELECT a.job_title FROM user_account a WHERE a.email = suspension.registrant_email),
    confirmed_by_job_title = (SELECT a.job_title FROM user_account a WHERE a.email = suspension.confirmed_by_email);
