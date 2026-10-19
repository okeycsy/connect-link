-- 샘플 기관. local, demo 프로필에서만 읽는다.
-- 참가기관은 공개된 금융기관 공동코드(3자리)와 실제 이름을 쓴다(CLAUDE.md 2절 1번). 이메일 도메인은 가상(example.com)이다.
-- 공개 코드를 확인하지 못한 카드·보험·캐피탈은 넣지 않는다. 이용기관은 금융회사가 아니라 가상 이름과 영문 코드를 둔다.
-- Flyway 반복 마이그레이션(R__)이라 내용이 바뀌면 다시 실행된다. 메모리 DB라 매번 새로 넣는다.
-- 반복 마이그레이션은 설명(파일 이름) 순서로 돈다. 1 기관 → 2 계정 → 3 알림 순서이며, 계정이 기관을 참조하므로 계정부터 지운다.
DELETE FROM user_account_it_role;
DELETE FROM user_account_business;
DELETE FROM user_account;
DELETE FROM change_history;
DELETE FROM organization_business;
DELETE FROM organization;

INSERT INTO organization (code, name, type, email_domain, active, created_at, created_by, updated_at, updated_by) VALUES
  ('KFTC',  '금융결제원',     'KFTC',                 'kftc.example.com',      TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('004',   'KB국민은행',    'FINANCE_ORGANIZATION', 'kb.example.com',        TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('088',   '신한은행',      'FINANCE_ORGANIZATION', 'shinhan.example.com',   TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('103',   'SBI저축은행',   'FINANCE_ORGANIZATION', 'sbi.example.com',       TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('240',   '삼성증권',      'FINANCE_ORGANIZATION', 'samsung.example.com',   TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('071',   '우체국',       'FINANCE_ORGANIZATION', 'epost.example.com',     TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('020',   '우리은행',      'FINANCE_ORGANIZATION', 'woori.example.com',     TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('050',   '저축은행중앙회',   'FINANCE_ORGANIZATION', 'fsb.example.com',       TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('090',   '카카오뱅크',     'FINANCE_ORGANIZATION', 'kakaobank.example.com', TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('048',   '신협',        'FINANCE_ORGANIZATION', 'cu.example.com',        TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('HAHA',  '하하통신',      'USE_ORGANIZATION',     'haha.example.com',      TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('005',   '외환은행',      'FINANCE_ORGANIZATION', 'keb.example.com',       FALSE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed');

-- 업권. 의견 요청에서 대상 기관을 업권별로 고를 때 쓴다(참가기관만).
UPDATE organization SET sector = CASE code
  WHEN '004'  THEN 'BANK'
  WHEN '088'  THEN 'BANK'
  WHEN '005'  THEN 'BANK'
  WHEN '103'  THEN 'MICROFINANCE'
  WHEN '050'  THEN 'MICROFINANCE'
  WHEN '240'  THEN 'SECURITIES'
  WHEN '020'  THEN 'BANK'
  WHEN '090'  THEN 'BANK'
  WHEN '048'  THEN 'MICROFINANCE'
END
WHERE type = 'FINANCE_ORGANIZATION';

-- 업무 참여. 업무마다 참여 기관 수가 다르게 해서 레일을 바꾸면 목록이 달라지도록 한다.
INSERT INTO organization_business (organization_id, business, created_at, created_by, updated_at, updated_by)
SELECT o.id, p.business, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('KFTC',    'ACCOUNT_INFO'), ('KFTC',  'AUTO_TRANSFER'), ('KFTC',  'LOAN_TRANSFER'), ('KFTC',  'CMS'),
  ('004',     'ACCOUNT_INFO'), ('004',  'AUTO_TRANSFER'), ('004',  'LOAN_TRANSFER'), ('004',  'CMS'),
  ('088',     'ACCOUNT_INFO'), ('088',  'AUTO_TRANSFER'), ('088',  'CMS'),
  ('103',     'ACCOUNT_INFO'), ('103',  'LOAN_TRANSFER'),
  ('240',     'ACCOUNT_INFO'), ('240',   'AUTO_TRANSFER'),
  ('071',   'AUTO_TRANSFER'), ('071', 'CMS'),
  ('020',    'LOAN_TRANSFER'), ('020',  'CMS'),
  ('050',     'ACCOUNT_INFO'), ('050', 'AUTO_TRANSFER'),
  ('090',    'LOAN_TRANSFER'),
  ('048',     'ACCOUNT_INFO'), ('048',  'AUTO_TRANSFER'), ('048',  'CMS'),
  ('HAHA',    'CMS'),
  ('005',     'ACCOUNT_INFO')
) AS p (code, business)
JOIN organization o ON o.code = p.code;

-- 기관 변경 이력 샘플. 기관 시드와 같은 파일에 두어 실행 순서(기관 → 이력)를 보장한다.
INSERT INTO change_history (target_type, target_id, business, action, summary, actor_name, changes, created_at, created_by, updated_at, updated_by)
SELECT 'ORGANIZATION', o.id, NULL, h.action, h.summary, h.actor_name, h.changes, h.changed_at, h.changed_by, h.changed_at, h.changed_by
FROM (VALUES
  ('088',     'CREATED', '신한은행 등록', '데모 관리자', 'admin@kftc.example.com', TIMESTAMP '2026-03-02 09:30:00',
   '[{"field":"code","label":"기관 코드","before":null,"after":"088"},{"field":"name","label":"기관명","before":null,"after":"신한은행"},{"field":"type","label":"기관 구분","before":null,"after":"참가기관"},{"field":"emailDomain","label":"이메일 도메인","before":null,"after":"shinhan.example.com"},{"field":"businesses","label":"참여 업무","before":null,"after":"계좌통합관리, 자동이체통합관리"}]'),
  ('088',     'UPDATED', '신한은행 수정', '데모 관리자', 'admin@kftc.example.com', TIMESTAMP '2026-07-14 14:05:00',
   '[{"field":"businesses","label":"참여 업무","before":"계좌통합관리, 자동이체통합관리","after":"계좌통합관리, 자동이체통합관리, CMS"}]'),
  ('005',     'UPDATED', '외환은행 수정', '데모 관리자', 'admin@kftc.example.com', TIMESTAMP '2026-09-01 10:00:00',
   '[{"field":"active","label":"사용 여부","before":"예","after":"아니요"}]'),
  ('048',     'UPDATED', '신협 수정', '데모 관리자', 'admin@kftc.example.com', TIMESTAMP '2026-10-02 16:40:00',
   '[{"field":"name","label":"기관명","before":"나다신용협동조합","after":"신협"},{"field":"emailDomain","label":"이메일 도메인","before":"nada-cu.example.com","after":"cu.example.com"}]')
) AS h (code, action, summary, actor_name, changed_by, changed_at, changes)
JOIN organization o ON o.code = h.code;
