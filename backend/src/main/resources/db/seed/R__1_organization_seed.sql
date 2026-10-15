-- 샘플 기관. 전부 가상 데이터다. local, demo 프로필에서만 읽는다.
-- Flyway 반복 마이그레이션(R__)이라 내용이 바뀌면 다시 실행된다. 메모리 DB라 매번 새로 넣는다.
-- 반복 마이그레이션은 설명(파일 이름) 순서로 돈다. 1 기관 → 2 계정 → 3 알림 순서이며, 계정이 기관을 참조하므로 계정부터 지운다.
DELETE FROM user_account_it_role;
DELETE FROM user_account_business;
DELETE FROM user_account;
DELETE FROM change_history;
DELETE FROM organization_business;
DELETE FROM organization;

INSERT INTO organization (code, name, type, email_domain, active, created_at, created_by, updated_at, updated_by) VALUES
  ('KFTC',   '금융결제원',     'KFTC',                 'kftc.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('GANA',   '가나은행',       'FINANCE_ORGANIZATION', 'gana.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('DARA',   '다라은행',       'FINANCE_ORGANIZATION', 'dara.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('MABA',   '마바저축은행',   'FINANCE_ORGANIZATION', 'maba.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('SAA',    '사아증권',       'FINANCE_ORGANIZATION', 'saa.example.com',     TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('JACHA',  '자차카드',       'FINANCE_ORGANIZATION', 'jacha.example.com',   TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('TAPA',   '타파보험',       'FINANCE_ORGANIZATION', 'tapa.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('CHAKA',  '차카저축은행',   'FINANCE_ORGANIZATION', 'chaka.example.com',   TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('PAHA',   '파하캐피탈',     'FINANCE_ORGANIZATION', 'paha.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('NADA',   '나다상호금융',   'FINANCE_ORGANIZATION', 'nada.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('HAHA',   '하하통신',       'USE_ORGANIZATION',     'haha.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('OLD',    '옛날은행',       'FINANCE_ORGANIZATION', 'old.example.com',     FALSE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed');

-- 업무 참여. 업무마다 참여 기관 수가 다르게 해서 레일을 바꾸면 목록이 달라지도록 한다.
INSERT INTO organization_business (organization_id, business, created_at, created_by, updated_at, updated_by)
SELECT o.id, p.business, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('KFTC',  'ACCOUNT_INFO'), ('KFTC',  'AUTO_TRANSFER'), ('KFTC',  'LOAN_TRANSFER'), ('KFTC',  'CMS'),
  ('GANA',  'ACCOUNT_INFO'), ('GANA',  'AUTO_TRANSFER'), ('GANA',  'LOAN_TRANSFER'), ('GANA',  'CMS'),
  ('DARA',  'ACCOUNT_INFO'), ('DARA',  'AUTO_TRANSFER'), ('DARA',  'CMS'),
  ('MABA',  'ACCOUNT_INFO'), ('MABA',  'LOAN_TRANSFER'),
  ('SAA',   'ACCOUNT_INFO'), ('SAA',   'AUTO_TRANSFER'),
  ('JACHA', 'AUTO_TRANSFER'), ('JACHA', 'CMS'),
  ('TAPA',  'LOAN_TRANSFER'), ('TAPA',  'CMS'),
  ('CHAKA', 'ACCOUNT_INFO'), ('CHAKA', 'AUTO_TRANSFER'),
  ('PAHA',  'LOAN_TRANSFER'),
  ('NADA',  'ACCOUNT_INFO'), ('NADA',  'AUTO_TRANSFER'), ('NADA',  'CMS'),
  ('HAHA',  'CMS'),
  ('OLD',   'ACCOUNT_INFO')
) AS p (code, business)
JOIN organization o ON o.code = p.code;

-- 기관 변경 이력 샘플. 기관 시드와 같은 파일에 두어 실행 순서(기관 → 이력)를 보장한다.
INSERT INTO change_history (target_type, target_id, business, action, summary, actor_name, changes, created_at, created_by, updated_at, updated_by)
SELECT 'ORGANIZATION', o.id, NULL, h.action, h.summary, h.actor_name, h.changes, h.changed_at, h.changed_by, h.changed_at, h.changed_by
FROM (VALUES
  ('DARA', 'CREATED', '다라은행 등록', '데모 관리자', 'admin@kftc.example.com', TIMESTAMP '2026-03-02 09:30:00',
   '[{"field":"code","label":"기관 코드","before":null,"after":"DARA"},{"field":"name","label":"기관명","before":null,"after":"다라은행"},{"field":"type","label":"기관 구분","before":null,"after":"참가기관"},{"field":"emailDomain","label":"이메일 도메인","before":null,"after":"dara.example.com"},{"field":"businesses","label":"참여 업무","before":null,"after":"계좌통합관리, 자동이체통합관리"}]'),
  ('DARA', 'UPDATED', '다라은행 수정', '데모 관리자', 'admin@kftc.example.com', TIMESTAMP '2026-07-14 14:05:00',
   '[{"field":"businesses","label":"참여 업무","before":"계좌통합관리, 자동이체통합관리","after":"계좌통합관리, 자동이체통합관리, CMS"}]'),
  ('OLD',  'UPDATED', '옛날은행 수정', '데모 관리자', 'admin@kftc.example.com', TIMESTAMP '2026-09-01 10:00:00',
   '[{"field":"active","label":"사용 여부","before":"예","after":"아니요"}]'),
  ('NADA', 'UPDATED', '나다상호금융 수정', '데모 관리자', 'admin@kftc.example.com', TIMESTAMP '2026-10-02 16:40:00',
   '[{"field":"name","label":"기관명","before":"나다신용협동조합","after":"나다상호금융"},{"field":"emailDomain","label":"이메일 도메인","before":"nada-cu.example.com","after":"nada.example.com"}]')
) AS h (code, action, summary, actor_name, changed_by, changed_at, changes)
JOIN organization o ON o.code = h.code;
