-- 가. 담당자 연락처 샘플. 전부 가상 사람·가상 번호(010-0000-xxxx)·가상 이메일(*.example.com)이다. local, demo 프로필(과 테스트)에서만 읽는다.
-- 기관(R__1)·계정(R__2) 시드 뒤에 돈다. 계정이 있는 담당자는 계정 시드와 이름·부서·번호를 맞췄고, 나머지는 계정이 없는 담당자(후임자 등)다.
-- 최근 확인일은 오늘 기준 며칠 전(days_ago)이라 시연 때 늘 "6개월 경과" 담당자가 업무마다 보인다(김민준 본인 행도 경과라 로그인 확인 알림이 뜬다).
-- 계좌통합관리에는 진행 중인 정기 변경 주간(완료·미완료·정보 변경 기관이 섞임)과 지난 주간, CMS 에는 예정 주간이 있다.
DELETE FROM update_campaign_organization;
DELETE FROM update_campaign;
DELETE FROM contact_it_role;
DELETE FROM contact;

INSERT INTO contact (organization_id, business, sub_business, name, department, team, job_title, job_role, contact_rank,
                     extension_phone, mobile_phone, email, last_confirmed_at, last_confirmed_by_name,
                     created_at, created_by, updated_at, updated_by)
SELECT o.id, c.business, c.sub_business, c.name, c.department, c.team, c.job_title, c.job_role, c.contact_rank,
       c.extension_phone, c.mobile_phone, c.email,
       CURRENT_TIMESTAMP - c.days_ago * INTERVAL '1' DAY, c.confirmed_by,
       CURRENT_TIMESTAMP - (c.days_ago + 30) * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - c.days_ago * INTERVAL '1' DAY, 'seed'
FROM (VALUES
  -- 계좌통합관리
  ('KFTC', 'ACCOUNT_INFO', '계좌정보 조회·해지',      '홍성민', '계좌서비스부', '계좌통합팀', '과장', 'BUSINESS', 'PRIMARY',   '1101', '010-0000-1101', 'hong.sungmin@kftc.example.com', 20,  '홍성민'),
  ('KFTC', 'ACCOUNT_INFO', CAST(NULL AS VARCHAR(200)), '김태호', '정보시스템부', '운영팀',     '대리', 'IT',       'PRIMARY',   '1201', CAST(NULL AS VARCHAR(30)), 'kim.ops@kftc.example.com', 40, '김태호'),
  ('004',  'ACCOUNT_INFO', '계좌정보 통합조회',       '김민준', '디지털금융부', '오픈뱅킹팀', '대리', 'BUSINESS', 'PRIMARY',   '2101', '010-0000-2101', 'kim.minjun@kb.example.com',       200, '김민준'),
  ('004',  'ACCOUNT_INFO', '계좌정보 통합조회',       '남궁민', '전산부',       '채널개발팀', '과장', 'IT',       'PRIMARY',   '2201', '010-0000-2201', 'namgung.min@kb.example.com',    30,  '김민준'),
  ('088',  'ACCOUNT_INFO', '내 계좌 한눈에',          '박지훈', '디지털채널부', '계좌서비스팀', '과장', 'BUSINESS', 'PRIMARY', '3103', NULL,            'park.jihoon@shinhan.example.com', 2,   '박지훈'),
  ('088',  'ACCOUNT_INFO', '내 계좌 한눈에',          '신서준', '디지털채널부', '계좌서비스팀', '대리', 'BUSINESS', 'SECONDARY', '3104', '010-0000-3104', 'shin.seojun@shinhan.example.com', 2,   '박지훈'),
  ('103',  'ACCOUNT_INFO', NULL,                      '문하린', '수신부',       '전자금융팀', '차장', 'BUSINESS', 'PRIMARY',   '5201', '010-0000-5201', 'moon.harin@sbi.example.com',    250, '문하린'),
  ('240',  'ACCOUNT_INFO', '오픈뱅킹 안심차단',       '최유진', '리테일영업부', '계좌관리팀', '대리', 'BUSINESS', 'PRIMARY',   '4101', NULL,            'choi.yujin@samsung.example.com',  1,   '최유진'),
  ('050',  'ACCOUNT_INFO', '오픈뱅킹 안심차단',       '윤서연', '수신업무부',   '전자금융팀', '대리', 'BUSINESS', 'PRIMARY',   '6101', NULL,            'yoon.seoyeon@fsb.example.com',    60,  '윤서연'),
  ('048',  'ACCOUNT_INFO', '내 계좌 한눈에',          '정현우', '전산정보부',   '채널개발팀', '대리', 'IT',       'PRIMARY',   '7101', NULL,            'jung.hyunwoo@cu.example.com',      1,   '정현우'),
  ('048',  'ACCOUNT_INFO', '내 계좌 한눈에',          '류지안', '수신부',       '전자금융팀', '과장', 'BUSINESS', 'PRIMARY',   '7201', '010-0000-7201', 'ryu.jian@cu.example.com',       190, '류지안'),
  -- 자동이체통합관리
  ('KFTC', 'AUTO_TRANSFER', '자동이체 통합조회',      '홍성민', '계좌서비스부', '계좌통합팀', '과장', 'BUSINESS', 'PRIMARY',   '1101', '010-0000-1101', 'hong.sungmin@kftc.example.com', 20,  '홍성민'),
  ('KFTC', 'AUTO_TRANSFER', NULL,                     '김태호', '정보시스템부', '운영팀',     '대리', 'IT',       'PRIMARY',   '1201', NULL,            'kim.ops@kftc.example.com',      40,  '김태호'),
  ('004',  'AUTO_TRANSFER', '자동이체 통합조회',      '장서윤', '디지털금융부', '수신결제팀', '차장', 'BUSINESS', 'PRIMARY',   '2301', '010-0000-2301', 'jang.seoyun@kb.example.com',    100, '장서윤'),
  ('088',  'AUTO_TRANSFER', NULL,                     '이준호', 'IT운영부',     '대외계팀',   '과장', 'IT',       'PRIMARY',   '3101', '010-0000-3101', 'lee.junho@shinhan.example.com',  15,  '이준호'),
  ('240',  'AUTO_TRANSFER', '자동이체 해지',          '임채원', '리테일영업부', '계좌관리팀', '과장', 'BUSINESS', 'PRIMARY',   '4201', '010-0000-4201', 'lim.chaewon@samsung.example.com', 210, '임채원'),
  ('071',  'AUTO_TRANSFER', '카드 자동납부',          '하윤슬', '결제사업부',   '자동납부팀', '대리', 'BUSINESS', 'PRIMARY',   '8101', '010-0000-8101', 'ha.yunseul@epost.example.com',  50,  '하윤슬'),
  ('050',  'AUTO_TRANSFER', NULL,                     '탁지유', '수신업무부',   '전자금융팀', '주임', 'BUSINESS', 'PRIMARY',   '6201', NULL,            'tak.jiyu@fsb.example.com',      80,  '탁지유'),
  ('048',  'AUTO_TRANSFER', NULL,                     '엄도경', '전산정보부',   '결제개발팀', '대리', 'IT',       'PRIMARY',   '7301', '010-0000-7301', 'eom.dokyung@cu.example.com',    30,  '엄도경'),
  -- 대출이동
  ('KFTC', 'LOAN_TRANSFER', '대환대출 인프라',        '진대호', '대출서비스부', '대출이동팀', '차장', 'BUSINESS', 'PRIMARY',   '1501', '010-0000-1501', 'jin.daeho@kftc.example.com',    60,  '진대호'),
  ('KFTC', 'LOAN_TRANSFER', NULL,                     '김태호', '정보시스템부', '운영팀',     '대리', 'IT',       'PRIMARY',   '1201', NULL,            'kim.ops@kftc.example.com',      40,  '김태호'),
  ('004',  'LOAN_TRANSFER', '신용대출 이동',          '왕보람', '여신부',       '대출상품팀', '과장', 'BUSINESS', 'PRIMARY',   '2401', '010-0000-2401', 'wang.boram@kb.example.com',     120, '왕보람'),
  ('103',  'LOAN_TRANSFER', '신규기관 주택담보대출',  '표은우', '여신부',       '대출심사팀', '대리', 'BUSINESS', 'PRIMARY',   '5301', '010-0000-5301', 'pyo.eunwoo@sbi.example.com',    185, '표은우'),
  ('020',  'LOAN_TRANSFER', NULL,                     '차민결', '대출지원부',   '약관대출팀', '대리', 'BUSINESS', 'PRIMARY',   '9101', '010-0000-9101', 'cha.mingyeol@woori.example.com', 20,  '차민결'),
  ('090',  'LOAN_TRANSFER', NULL,                     '석다온', '리테일금융부', '대출운영팀', '과장', 'BUSINESS', 'PRIMARY',   '9201', '010-0000-9201', 'seok.daon@kakaobank.example.com',    70,  '석다온'),
  -- CMS
  ('KFTC', 'CMS',           '출금이체',               '박하늘', 'CMS사업부',    'CMS운영팀',  '과장', 'BUSINESS', 'PRIMARY',   '1401', NULL,            'park.cms@kftc.example.com',     15,  '박하늘'),
  ('KFTC', 'CMS',           NULL,                     '김태호', '정보시스템부', '운영팀',     '대리', 'IT',       'PRIMARY',   '1201', NULL,            'kim.ops@kftc.example.com',      40,  '김태호'),
  ('004',  'CMS',           '출금이체',               '국해솔', '기업금융부',   '자금관리팀', '대리', 'BUSINESS', 'PRIMARY',   '2501', '010-0000-2501', 'kook.haesol@kb.example.com',    90,  '국해솔'),
  ('088',  'CMS',           '출금이체 전문 연계',     '이준호', 'IT운영부',     '대외계팀',   '과장', 'IT',       'PRIMARY',   '3101', '010-0000-3101', 'lee.junho@shinhan.example.com',  15,  '이준호'),
  ('071',  'CMS',           NULL,                     '하윤슬', '결제사업부',   '자동납부팀', '대리', 'BUSINESS', 'PRIMARY',   '8101', '010-0000-8101', 'ha.yunseul@epost.example.com',  50,  '하윤슬'),
  ('020',  'CMS',           '대출이자 출금',            '차민결', '대출지원부',   '약관대출팀', '대리', 'BUSINESS', 'PRIMARY',   '9101', '010-0000-9101', 'cha.mingyeol@woori.example.com', 200, '차민결'),
  ('048',  'CMS',           NULL,                     '엄도경', '전산정보부',   '결제개발팀', '대리', 'IT',       'PRIMARY',   '7301', '010-0000-7301', 'eom.dokyung@cu.example.com',    30,  '엄도경')
) AS c (org_code, business, sub_business, name, department, team, job_title, job_role, contact_rank,
        extension_phone, mobile_phone, email, days_ago, confirmed_by)
JOIN organization o ON o.code = c.org_code;

-- 전산 세부 직무.
INSERT INTO contact_it_role (contact_id, it_role, created_at, created_by, updated_at, updated_by)
SELECT c.id, r.it_role, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('kim.ops@kftc.example.com',     'OPS'),
  ('namgung.min@kb.example.com',   'DEV'),
  ('jung.hyunwoo@cu.example.com',     'DEV'),
  ('lee.junho@shinhan.example.com', 'DEV'),
  ('lee.junho@shinhan.example.com', 'EXTERNAL_LINK'),
  ('eom.dokyung@cu.example.com',   'DEV')
) AS r (email, it_role)
JOIN contact c ON c.email = r.email;

-- 변경 이력. KB국민은행 계좌통합관리 전산 담당자가 백도윤에서 남궁민으로 바뀌었다(이전 담당자는 한 명까지만, 휴대폰·이메일은 가려서 보인다).
-- 더 오래된 등록 이력은 있지만 화면에는 가장 최근 한 건만 보인다.
INSERT INTO change_history (target_type, target_id, business, action, summary, actor_name, changes, created_at, created_by, updated_at, updated_by)
SELECT 'CONTACT', c.id, c.business, h.action, h.summary, h.actor_name, h.changes,
       CURRENT_TIMESTAMP - h.days_ago * INTERVAL '1' DAY, h.changed_by, CURRENT_TIMESTAMP - h.days_ago * INTERVAL '1' DAY, h.changed_by
FROM (VALUES
  ('namgung.min@kb.example.com', 'ACCOUNT_INFO', 'CREATED', 'KB국민은행 백도윤 담당자 등록', '홍성민', 'hong.sungmin@kftc.example.com', 400,
   '[{"field":"name","label":"이름","before":null,"after":"백도윤"},{"field":"department","label":"부서","before":null,"after":"전산부"},{"field":"extensionPhone","label":"내선번호","before":null,"after":"2299"},{"field":"mobilePhone","label":"휴대폰번호","before":null,"after":"010-0000-2299"}]'),
  ('namgung.min@kb.example.com', 'ACCOUNT_INFO', 'UPDATED', 'KB국민은행 남궁민 담당자 수정', '김민준', 'kim.minjun@kb.example.com', 30,
   '[{"field":"name","label":"이름","before":"백도윤","after":"남궁민"},{"field":"team","label":"팀","before":"인프라팀","after":"채널개발팀"},{"field":"jobTitle","label":"직급","before":"대리","after":"과장"},{"field":"extensionPhone","label":"내선번호","before":"2299","after":"2201"},{"field":"mobilePhone","label":"휴대폰번호","before":"010-0000-2299","after":"010-0000-2201"},{"field":"email","label":"회사 이메일","before":"baek.doyun@kb.example.com","after":"namgung.min@kb.example.com"}]'),
  ('park.jihoon@shinhan.example.com', 'ACCOUNT_INFO', 'UPDATED', '신한은행 박지훈 담당자 수정', '박지훈', 'park.jihoon@shinhan.example.com', 2,
   '[{"field":"extensionPhone","label":"내선번호","before":"3199","after":"3103"}]')
) AS h (email, business, action, summary, actor_name, changed_by, days_ago, changes)
JOIN contact c ON c.email = h.email AND c.business = h.business;

-- 정기 변경 주간. 계좌통합관리는 사흘 전 시작해 나흘 뒤 끝나는 진행 중 주간과 반년 전 지난 주간, CMS 는 열흘 뒤 시작하는 예정 주간.
INSERT INTO update_campaign (business, title, start_date, end_date, registrant_name, created_at, created_by, updated_at, updated_by)
VALUES
  ('ACCOUNT_INFO', '하반기 담당자 정기 변경', CAST(CURRENT_DATE - 3 * INTERVAL '1' DAY AS DATE), CAST(CURRENT_DATE + 4 * INTERVAL '1' DAY AS DATE), '홍성민',
   CURRENT_TIMESTAMP - INTERVAL '5' DAY, 'hong.sungmin@kftc.example.com', CURRENT_TIMESTAMP - INTERVAL '5' DAY, 'hong.sungmin@kftc.example.com'),
  ('ACCOUNT_INFO', '상반기 담당자 정기 변경', CAST(CURRENT_DATE - 185 * INTERVAL '1' DAY AS DATE), CAST(CURRENT_DATE - 178 * INTERVAL '1' DAY AS DATE), '홍성민',
   CURRENT_TIMESTAMP - INTERVAL '190' DAY, 'hong.sungmin@kftc.example.com', CURRENT_TIMESTAMP - INTERVAL '190' DAY, 'hong.sungmin@kftc.example.com'),
  ('CMS',          '연말 담당자 정기 변경',   CAST(CURRENT_DATE + 10 * INTERVAL '1' DAY AS DATE), CAST(CURRENT_DATE + 16 * INTERVAL '1' DAY AS DATE), '박하늘',
   CURRENT_TIMESTAMP - INTERVAL '1' DAY, 'park.cms@kftc.example.com', CURRENT_TIMESTAMP - INTERVAL '1' DAY, 'park.cms@kftc.example.com');

-- 기관별 현행화. 진행 중 주간: 신한은행(정보 변경도 있음)·삼성증권 완료, 신협은 정보만 바꾸고 미완료. 지난 주간: 다섯 기관 완료.
INSERT INTO update_campaign_organization (campaign_id, organization_id, completed_at, completed_by_name, changed_at, created_at, created_by, updated_at, updated_by)
SELECT u.id, o.id,
       CASE WHEN r.completed_days_ago IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CURRENT_TIMESTAMP - r.completed_days_ago * INTERVAL '1' DAY END,
       r.completed_by,
       CASE WHEN r.changed_days_ago IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CURRENT_TIMESTAMP - r.changed_days_ago * INTERVAL '1' DAY END,
       CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('하반기 담당자 정기 변경', '088', 2,                  '박지훈',                     2),
  ('하반기 담당자 정기 변경', '240', 1,                  '최유진',                     CAST(NULL AS INTEGER)),
  ('하반기 담당자 정기 변경', '048', CAST(NULL AS INTEGER), CAST(NULL AS VARCHAR(100)), 1),
  ('상반기 담당자 정기 변경', '004', 183,                '김민준',                     184),
  ('상반기 담당자 정기 변경', '088', 182,                '박지훈',                     NULL),
  ('상반기 담당자 정기 변경', '240', 180,                '최유진',                     NULL),
  ('상반기 담당자 정기 변경', '050', 179,                '윤서연',                     179),
  ('상반기 담당자 정기 변경', '048', 181,                '정현우',                     NULL)
) AS r (title, org_code, completed_days_ago, completed_by, changed_days_ago)
JOIN update_campaign u ON u.title = r.title
JOIN organization o ON o.code = r.org_code;
