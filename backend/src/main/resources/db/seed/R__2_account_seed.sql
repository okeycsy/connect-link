-- 역할별 데모 계정과 샘플 가입 신청(승인 대기·반려). 전부 가상 데이터다. local, demo 프로필(과 테스트)에서만 읽으므로 운영에는 데모 계정이 생기지 않는다.
-- 비밀번호는 모두 데모 공통값(app.demo.account-password, README 의 데모 계정 표)이며 여기에는 BCrypt 해시만 둔다.
-- 기관 시드(R__1) 뒤에 돈다. 지우기는 기관 시드가 먼저 한다.
DELETE FROM user_account_it_role;
DELETE FROM user_account_business;
DELETE FROM user_account;

INSERT INTO user_account (email, password_hash, name, organization_id, status, admin, job_role, department, team, job_title, extension_phone, mobile_phone,
                          applied_at, reviewed_at, reviewer_email, reviewer_name, rejection_reason, created_at, created_by, updated_at, updated_by)
SELECT a.email, '$2a$10$zGUD2cvgSsbBBOlUXr3jE.F5xzRbA2kCey4pwdXvJsmdrx0eGzria', a.name, o.id, a.status, a.admin, a.job_role,
       a.department, a.team, a.job_title, a.extension_phone, a.mobile_phone,
       a.applied_at, a.reviewed_at, a.reviewer_email, a.reviewer_name, a.rejection_reason, a.applied_at, 'seed', COALESCE(a.reviewed_at, a.applied_at), 'seed'
FROM (VALUES
  ('admin@kftc.example.com',        '데모 관리자', 'KFTC', 'ACTIVE',           TRUE,  'IT',       '업무지원부',   '포털운영팀',   '차장', '1000', '010-0000-1000',
   CURRENT_TIMESTAMP - INTERVAL '90' DAY, CAST(NULL AS TIMESTAMP), CAST(NULL AS VARCHAR(200)), CAST(NULL AS VARCHAR(50)), CAST(NULL AS VARCHAR(500))),
  ('hong.sungmin@kftc.example.com', '홍성민',      'KFTC', 'ACTIVE',           FALSE, 'BUSINESS', '계좌서비스부', '계좌통합팀',   '과장', '1101', '010-0000-1101',
   CURRENT_TIMESTAMP - INTERVAL '90' DAY, NULL, NULL, NULL, NULL),
  ('kim.ops@kftc.example.com',      '김태호',      'KFTC', 'ACTIVE',           FALSE, 'IT',       '정보시스템부', '운영팀',       '대리', '1201', CAST(NULL AS VARCHAR(30)),
   CURRENT_TIMESTAMP - INTERVAL '90' DAY, NULL, NULL, NULL, NULL),
  ('lee.control@kftc.example.com',  '이수진',      'KFTC', 'ACTIVE',           FALSE, 'IT',       '정보시스템부', '상황실',       '주임', '1301', '010-0000-1301',
   CURRENT_TIMESTAMP - INTERVAL '90' DAY, NULL, NULL, NULL, NULL),
  ('park.cms@kftc.example.com',     '박하늘',      'KFTC', 'ACTIVE',           FALSE, 'BUSINESS', 'CMS사업부',    'CMS운영팀',    '과장', '1401', NULL,
   CURRENT_TIMESTAMP - INTERVAL '90' DAY, NULL, NULL, NULL, NULL),
  ('moon.seojun@kftc.example.com',  '문서준',      'KFTC', 'ACTIVE',           FALSE, 'BUSINESS', '계좌서비스부', '자동이체팀',   '과장', '1601', '010-0000-1601',
   CURRENT_TIMESTAMP - INTERVAL '90' DAY, NULL, NULL, NULL, NULL),
  -- 가입 승인을 거친 기관 담당자(관리 > 가입 승인의 "승인" 칩에 보인다).
  ('kim.minjun@kb.example.com',     '김민준',      '004', 'ACTIVE',           FALSE, 'BUSINESS', '디지털금융부', '오픈뱅킹팀',   '대리', '2101', '010-0000-2101',
   CURRENT_TIMESTAMP - INTERVAL '21' DAY, CURRENT_TIMESTAMP - INTERVAL '20' DAY, 'hong.sungmin@kftc.example.com', '홍성민', NULL),
  ('lee.junho@shinhan.example.com',     '이준호',      '088', 'ACTIVE',           FALSE, 'IT',       'IT운영부',     '대외계팀',     '과장', '3101', '010-0000-3101',
   CURRENT_TIMESTAMP - INTERVAL '40' DAY, CURRENT_TIMESTAMP - INTERVAL '39' DAY, 'park.cms@kftc.example.com', '박하늘', NULL),
  ('choi.yujin@samsung.example.com',      '최유진',      '240',  'ACTIVE',           FALSE, 'BUSINESS', '리테일영업부', '계좌관리팀',   '대리', '4101', NULL,
   CURRENT_TIMESTAMP - INTERVAL '6' DAY, CURRENT_TIMESTAMP - INTERVAL '5' DAY, 'admin@kftc.example.com', '데모 관리자', NULL),
  -- 계좌통합관리 기관 담당자(세부업무가 서로 달라 의견 요청 대상을 세부업무로 묶어 고르는 시연에 쓴다).
  ('park.jihoon@shinhan.example.com',    '박지훈',      '088', 'ACTIVE',           FALSE, 'BUSINESS', '디지털채널부', '계좌서비스팀', '과장', '3103', NULL,
   CURRENT_TIMESTAMP - INTERVAL '60' DAY, NULL, NULL, NULL, NULL),
  ('yoon.seoyeon@fsb.example.com',  '윤서연',      '050', 'ACTIVE',          FALSE, 'BUSINESS', '수신업무부',   '전자금융팀',   '대리', '6101', NULL,
   CURRENT_TIMESTAMP - INTERVAL '60' DAY, NULL, NULL, NULL, NULL),
  ('jung.hyunwoo@cu.example.com',    '정현우',      '048', 'ACTIVE',           FALSE, 'IT',       '전산정보부',   '채널개발팀',   '대리', '7101', NULL,
   CURRENT_TIMESTAMP - INTERVAL '60' DAY, NULL, NULL, NULL, NULL),
  -- 승인 대기 신청. 오승현은 회사 이메일 도메인이 기관과 다르다.
  ('jung.daeun@woori.example.com',  '정다은',      '020', 'PENDING_APPROVAL', FALSE, 'IT',       '전산부',       '개발팀',       '주임', '5101', '010-0000-5101',
   CURRENT_TIMESTAMP - INTERVAL '1' HOUR, NULL, NULL, NULL, NULL),
  ('han.yerin@kb.example.com',     '한예린',      '004', 'PENDING_APPROVAL', FALSE, 'BUSINESS', '디지털금융부', '오픈뱅킹팀',   '주임', '2102', NULL,
   CURRENT_TIMESTAMP - INTERVAL '1' DAY, NULL, NULL, NULL, NULL),
  ('oh.seunghyun@mail.example.com',      '오승현',      '088', 'PENDING_APPROVAL', FALSE, 'IT',       'IT운영부',     '개발팀',       '대리', '3102', '010-0000-3102',
   CURRENT_TIMESTAMP - INTERVAL '3' HOUR, NULL, NULL, NULL, NULL),
  -- 반려된 신청. 같은 이메일로 다시 가입 신청할 수 있다.
  ('seo.minjae@samsung.example.com',       '서민재',      '240',  'REJECTED',         FALSE, 'BUSINESS', '리테일영업부', '계좌관리팀',   '주임', '4102', NULL,
   CURRENT_TIMESTAMP - INTERVAL '4' DAY, CURRENT_TIMESTAMP - INTERVAL '3' DAY, 'hong.sungmin@kftc.example.com', '홍성민',
   '삼성증권 계좌통합관리 담당자 명단에서 확인되지 않습니다. 부서장 확인 뒤 다시 신청해 주세요.')
) AS a (email, name, org_code, status, admin, job_role, department, team, job_title, extension_phone, mobile_phone,
        applied_at, reviewed_at, reviewer_email, reviewer_name, rejection_reason)
JOIN organization o ON o.code = a.org_code;

-- 담당 업무와 세부업무. 결제원 운영·상황실은 전 업무를 맡는다(명세: 전 업무 조회). 결제원 현업·개발 담당자는 보통 업무 하나만 맡아 하나만 둔다.
-- 참가기관 담당자는 여러 업무를 맡기도 한다.
INSERT INTO user_account_business (account_id, business, sub_business, created_at, created_by, updated_at, updated_by)
SELECT u.id, b.business, b.sub_business, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('admin@kftc.example.com',        'ACCOUNT_INFO',  CAST(NULL AS VARCHAR(200))),
  ('admin@kftc.example.com',        'AUTO_TRANSFER', NULL),
  ('admin@kftc.example.com',        'LOAN_TRANSFER', NULL),
  ('admin@kftc.example.com',        'CMS',           NULL),
  ('hong.sungmin@kftc.example.com', 'ACCOUNT_INFO',  '본인계좌 일괄지급정지'),
  ('kim.ops@kftc.example.com',      'ACCOUNT_INFO',  NULL),
  ('kim.ops@kftc.example.com',      'AUTO_TRANSFER', NULL),
  ('kim.ops@kftc.example.com',      'LOAN_TRANSFER', NULL),
  ('kim.ops@kftc.example.com',      'CMS',           NULL),
  ('lee.control@kftc.example.com',  'ACCOUNT_INFO',  NULL),
  ('lee.control@kftc.example.com',  'AUTO_TRANSFER', NULL),
  ('lee.control@kftc.example.com',  'LOAN_TRANSFER', NULL),
  ('lee.control@kftc.example.com',  'CMS',           NULL),
  ('park.cms@kftc.example.com',     'CMS',           '출금이체'),
  ('moon.seojun@kftc.example.com',  'AUTO_TRANSFER', '자동이체 통합조회'),
  ('kim.minjun@kb.example.com',     'ACCOUNT_INFO',  '본인계좌 일괄지급정지'),
  ('lee.junho@shinhan.example.com',     'AUTO_TRANSFER', NULL),
  ('lee.junho@shinhan.example.com',     'CMS',           '출금이체 전문 연계'),
  ('choi.yujin@samsung.example.com',      'ACCOUNT_INFO',  '오픈뱅킹 안심차단'),
  ('park.jihoon@shinhan.example.com',    'ACCOUNT_INFO',  '내 계좌 한눈에'),
  ('yoon.seoyeon@fsb.example.com',  'ACCOUNT_INFO',  '오픈뱅킹 안심차단'),
  ('jung.hyunwoo@cu.example.com',    'ACCOUNT_INFO',  '내 계좌 한눈에'),
  ('jung.daeun@woori.example.com',    'LOAN_TRANSFER', '신규기관 주택담보대출'),
  ('han.yerin@kb.example.com',     'ACCOUNT_INFO',  '본인계좌 일괄지급정지'),
  ('oh.seunghyun@mail.example.com',      'ACCOUNT_INFO',  NULL),
  ('oh.seunghyun@mail.example.com',      'AUTO_TRANSFER', '자동이체 전문 개발'),
  ('seo.minjae@samsung.example.com',       'ACCOUNT_INFO',  NULL)
) AS b (email, business, sub_business)
JOIN user_account u ON u.email = b.email;

-- 전산 세부 직무.
INSERT INTO user_account_it_role (account_id, it_role, created_at, created_by, updated_at, updated_by)
SELECT u.id, r.it_role, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('admin@kftc.example.com',       'OPS'),
  ('kim.ops@kftc.example.com',     'OPS'),
  ('lee.control@kftc.example.com', 'CONTROL_ROOM'),
  ('lee.junho@shinhan.example.com',    'DEV'),
  ('lee.junho@shinhan.example.com',    'EXTERNAL_LINK'),
  ('jung.daeun@woori.example.com',   'DEV'),
  ('oh.seunghyun@mail.example.com',     'DEV'),
  ('jung.hyunwoo@cu.example.com',   'DEV')
) AS r (email, it_role)
JOIN user_account u ON u.email = r.email;
