-- 샘플 기관. 전부 가상 데이터다. local, demo 프로필에서만 읽는다.
-- Flyway 반복 마이그레이션(R__)이라 내용이 바뀌면 다시 실행된다. 메모리 DB라 매번 새로 넣는다.
DELETE FROM organization;

INSERT INTO organization (code, name, type, email_domain, active, created_at, created_by, updated_at, updated_by) VALUES
  ('KFTC',   '금융결제원',     'KFTC',                 'kftc.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('GANA',   '가나은행',       'FINANCE_ORGANIZATION', 'gana.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('DARA',   '다라은행',       'FINANCE_ORGANIZATION', 'dara.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('MABA',   '마바저축은행',   'FINANCE_ORGANIZATION', 'maba.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('SAA',    '사아증권',       'FINANCE_ORGANIZATION', 'saa.example.com',     TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('JACHA',  '자차카드',       'FINANCE_ORGANIZATION', 'jacha.example.com',   TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('TAPA',   '타파보험',       'FINANCE_ORGANIZATION', 'tapa.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('HAHA',   '하하통신',       'USE_ORGANIZATION',     'haha.example.com',    TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'),
  ('OLD',    '옛날은행',       'FINANCE_ORGANIZATION', 'old.example.com',     FALSE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed');
