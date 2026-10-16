-- 역할별 데모 계정. 전부 가상 데이터다. local, demo 프로필(과 테스트)에서만 읽으므로 운영에는 데모 계정이 생기지 않는다.
-- 비밀번호는 모두 데모 공통값(app.demo.account-password, README 의 데모 계정 표)이며 여기에는 BCrypt 해시만 둔다.
-- 기관 시드(R__1) 뒤에 돈다. 지우기는 기관 시드가 먼저 한다.
DELETE FROM user_account_it_role;
DELETE FROM user_account_business;
DELETE FROM user_account;

INSERT INTO user_account (email, password_hash, name, organization_id, status, admin, job_role, department, team, job_title, extension_phone, mobile_phone, created_at, created_by, updated_at, updated_by)
SELECT a.email, '$2a$10$zGUD2cvgSsbBBOlUXr3jE.F5xzRbA2kCey4pwdXvJsmdrx0eGzria', a.name, o.id, a.status, a.admin, a.job_role,
       a.department, a.team, a.job_title, a.extension_phone, a.mobile_phone, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('admin@kftc.example.com',        '데모 관리자', 'KFTC', 'ACTIVE',           TRUE,  'IT',       '업무지원부',   '포털운영팀',   '차장', '1000', '010-0000-1000'),
  ('hong.gildong@kftc.example.com', '홍길동',      'KFTC', 'ACTIVE',           FALSE, 'BUSINESS', '계좌서비스부', '계좌통합팀',   '과장', '1101', '010-0000-1101'),
  ('kim.ops@kftc.example.com',      '김운영',      'KFTC', 'ACTIVE',           FALSE, 'IT',       '정보시스템부', '운영팀',       '대리', '1201', CAST(NULL AS VARCHAR(30))),
  ('lee.control@kftc.example.com',  '이상황',      'KFTC', 'ACTIVE',           FALSE, 'IT',       '정보시스템부', '상황실',       '주임', '1301', '010-0000-1301'),
  ('park.cms@kftc.example.com',     '박하늘',      'KFTC', 'ACTIVE',           FALSE, 'BUSINESS', 'CMS사업부',    'CMS운영팀',    '과장', '1401', NULL),
  ('kim.gana@gana.example.com',     '김가나',      'GANA', 'ACTIVE',           FALSE, 'BUSINESS', '디지털금융부', '오픈뱅킹팀',   '대리', '2101', '010-0000-2101'),
  ('lee.dara@dara.example.com',     '이다라',      'DARA', 'ACTIVE',           FALSE, 'IT',       'IT운영부',     '대외계팀',     '과장', '3101', '010-0000-3101'),
  ('choi.saa@saa.example.com',      '최사아',      'SAA',  'ACTIVE',           FALSE, 'BUSINESS', '리테일영업부', '계좌관리팀',   '대리', '4101', NULL),
  ('jung.maba@maba.example.com',    '정마바',      'MABA', 'PENDING_APPROVAL', FALSE, 'IT',       '전산부',       '개발팀',       '사원', '5101', '010-0000-5101')
) AS a (email, name, org_code, status, admin, job_role, department, team, job_title, extension_phone, mobile_phone)
JOIN organization o ON o.code = a.org_code;

-- 담당 업무와 세부업무. 결제원 운영·상황실은 전 업무를 맡는다(명세: 전 업무 조회).
INSERT INTO user_account_business (account_id, business, sub_business, created_at, created_by, updated_at, updated_by)
SELECT u.id, b.business, b.sub_business, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('admin@kftc.example.com',        'ACCOUNT_INFO',  CAST(NULL AS VARCHAR(200))),
  ('admin@kftc.example.com',        'AUTO_TRANSFER', NULL),
  ('admin@kftc.example.com',        'LOAN_TRANSFER', NULL),
  ('admin@kftc.example.com',        'CMS',           NULL),
  ('hong.gildong@kftc.example.com', 'ACCOUNT_INFO',  '계좌정보 조회·해지'),
  ('hong.gildong@kftc.example.com', 'AUTO_TRANSFER', '자동이체 통합조회'),
  ('kim.ops@kftc.example.com',      'ACCOUNT_INFO',  NULL),
  ('kim.ops@kftc.example.com',      'AUTO_TRANSFER', NULL),
  ('kim.ops@kftc.example.com',      'LOAN_TRANSFER', NULL),
  ('kim.ops@kftc.example.com',      'CMS',           NULL),
  ('lee.control@kftc.example.com',  'ACCOUNT_INFO',  NULL),
  ('lee.control@kftc.example.com',  'AUTO_TRANSFER', NULL),
  ('lee.control@kftc.example.com',  'LOAN_TRANSFER', NULL),
  ('lee.control@kftc.example.com',  'CMS',           NULL),
  ('park.cms@kftc.example.com',     'CMS',           '출금이체'),
  ('kim.gana@gana.example.com',     'ACCOUNT_INFO',  '계좌정보 통합조회'),
  ('lee.dara@dara.example.com',     'AUTO_TRANSFER', NULL),
  ('lee.dara@dara.example.com',     'CMS',           '출금이체 전문 연계'),
  ('choi.saa@saa.example.com',      'ACCOUNT_INFO',  NULL),
  ('jung.maba@maba.example.com',    'LOAN_TRANSFER', '신규기관 주택담보대출')
) AS b (email, business, sub_business)
JOIN user_account u ON u.email = b.email;

-- 전산 세부 직무.
INSERT INTO user_account_it_role (account_id, it_role, created_at, created_by, updated_at, updated_by)
SELECT u.id, r.it_role, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('admin@kftc.example.com',       'OPS'),
  ('kim.ops@kftc.example.com',     'OPS'),
  ('lee.control@kftc.example.com', 'CONTROL_ROOM'),
  ('lee.dara@dara.example.com',    'DEV'),
  ('lee.dara@dara.example.com',    'EXTERNAL_LINK'),
  ('jung.maba@maba.example.com',   'DEV')
) AS r (email, it_role)
JOIN user_account u ON u.email = r.email;
