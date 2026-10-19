-- 일시중단 샘플. 전부 가상 데이터다. local, demo 프로필(과 테스트)에서만 읽는다.
-- 기관(R__1)·계정(R__2) 시드 뒤에 돈다. 시각은 오늘 0시 기준 분(start_min, end_min)이라 시연 때 늘 진행 중·예정·지난 건이 함께 보인다.
-- 진행 중(확정), 내일 시작(확정, 전 영업일 취합에 들어감), 예정(검토 대기·검토 중), 지난(완료)을 업무마다 섞었다.
DELETE FROM suspension_digest;
DELETE FROM suspension_reviewer;
DELETE FROM suspension;

INSERT INTO suspension (business, sub_business, organization_id, start_at, end_at, reason, kftc_action, contact_name, contact_phone, contact_email, extra_note,
                        status, registrant_email, registrant_name, registrant_kftc, handover, confirmed_at, confirmed_by_email, confirmed_by_name, completed_at,
                        created_at, created_by, updated_at, updated_by)
SELECT s.business, s.sub_business, o.id,
       CAST(CURRENT_DATE AS TIMESTAMP) + s.start_min * INTERVAL '1' MINUTE,
       CAST(CURRENT_DATE AS TIMESTAMP) + s.end_min * INTERVAL '1' MINUTE,
       s.reason, s.kftc_action, s.contact_name, s.contact_phone, s.contact_email, s.extra_note,
       s.status, s.registrant_email, s.registrant_name, s.registrant_kftc, s.handover,
       CASE WHEN s.confirmed_by_email IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CAST(CURRENT_DATE AS TIMESTAMP) + (s.start_min - 1440) * INTERVAL '1' MINUTE END,
       s.confirmed_by_email, s.confirmed_by_name,
       CASE WHEN s.status = 'COMPLETED' THEN CAST(CURRENT_DATE AS TIMESTAMP) + (s.end_min + 1) * INTERVAL '1' MINUTE ELSE CAST(NULL AS TIMESTAMP) END,
       CAST(CURRENT_DATE AS TIMESTAMP) + (s.start_min - 4320) * INTERVAL '1' MINUTE, s.registrant_email,
       CAST(CURRENT_DATE AS TIMESTAMP) + (s.start_min - 2880) * INTERVAL '1' MINUTE, s.registrant_email
FROM (VALUES
  -- 계좌통합관리
  ('ACCOUNT_INFO', '계좌정보 통합조회', '004', 30, 1800,
   '계정계 시스템 증설 작업으로 계좌정보 통합조회 응답이 중단됩니다.', '작업 시간 동안 KB국민은행 조회 요청을 차단하고 고객 안내 문구를 띄웁니다.',
   '김가나', '010-0000-2101', 'kim.gana@kb.example.com', CAST(NULL AS VARCHAR(2000)),
   'CONFIRMED', 'kim.gana@kb.example.com', '김가나', FALSE,
   '운영팀이 00:30 에 KB국민은행 조회를 차단했습니다. 재개 연락이 오면 차단을 풀고 상황실에 알립니다.', 'kim.ops@kftc.example.com', '김운영'),
  ('ACCOUNT_INFO', '내 계좌 한눈에', '088', 2820, 3000,
   '인터넷뱅킹 서버 교체로 내 계좌 한눈에 연계가 끊깁니다.', '신한은행 연계 차단, 오류 문의는 신한은행 담당자에게 안내',
   '박다라', '010-0000-3103', NULL, '작업이 길어지면 연장 공문을 다시 올리겠습니다.',
   'PENDING_REVIEW', 'park.dara@shinhan.example.com', '박다라', FALSE, CAST(NULL AS VARCHAR(2000)), CAST(NULL AS VARCHAR(200)), CAST(NULL AS VARCHAR(50))),
  ('ACCOUNT_INFO', '오픈뱅킹 안심차단', '240', 5640, 5730,
   'DB 정기 점검으로 안심차단 등록·해제가 중단됩니다.', '삼성증권 안심차단 요청을 대기열에 쌓았다가 재개 뒤 다시 보냅니다.',
   '최사아', '010-0000-4101', 'choi.saa@samsung.example.com', NULL,
   'IN_REVIEW', 'choi.saa@samsung.example.com', '최사아', FALSE, NULL, NULL, NULL),
  ('ACCOUNT_INFO', '계좌정보 일괄해지', '050', 8700, 8940,
   '차세대 시스템 전환 리허설로 계좌정보 일괄해지가 중단됩니다.', '저축은행중앙회 해지 요청 차단, 리허설 끝나면 저축은행중앙회이 재개 연락',
   '윤차카', '010-0000-6101', 'yoon.chaka@fsb.example.com', '결제원 요청으로 등록합니다(기관 공문 접수).',
   'CONFIRMED', 'hong.gildong@kftc.example.com', '홍길동', TRUE,
   '개발팀이 해지 전문 차단 설정을 미리 반영해 둡니다. 운영팀은 리허설 시작·종료 시각을 상황실에 공유합니다.', 'hong.gildong@kftc.example.com', '홍길동'),
  ('ACCOUNT_INFO', '계좌정보 통합조회', '103', -13020, -12780,
   '전산센터 이전으로 계좌정보 통합조회가 중단되었습니다.', 'SBI저축은행 조회 차단',
   '정마바', '010-0000-5101', NULL, NULL,
   'COMPLETED', 'hong.gildong@kftc.example.com', '홍길동', TRUE, '운영팀이 차단과 해제를 마쳤습니다.', 'kim.ops@kftc.example.com', '김운영'),
  ('ACCOUNT_INFO', '계좌정보 통합조회', '004', -56220, -56040,
   '재해복구 전환 훈련으로 조회가 중단되었습니다.', 'KB국민은행 조회 차단',
   '김가나', '010-0000-2101', 'kim.gana@kb.example.com', NULL,
   'COMPLETED', 'kim.gana@kb.example.com', '김가나', FALSE, '상황실이 훈련 시작·종료를 확인했습니다.', 'lee.control@kftc.example.com', '이상황'),
  -- 자동이체통합관리
  ('AUTO_TRANSFER', '자동이체 통합조회', 'JACHA', 1560, 1680,
   '카드 승인 시스템 정기 점검으로 자동이체 조회가 중단됩니다.', '자차카드 조회 요청 차단',
   '윤자차', '010-0000-8101', NULL, NULL,
   'CONFIRMED', 'hong.gildong@kftc.example.com', '홍길동', TRUE, '운영팀이 02:00 에 차단하고 04:00 에 풉니다.', 'kim.ops@kftc.example.com', '김운영'),
  ('AUTO_TRANSFER', '자동이체 해지', '088', 4200, 4380,
   '대외계 회선 교체로 자동이체 해지 전문 송수신이 중단됩니다.', '신한은행 해지 전문 보류',
   '이다라', '010-0000-3101', 'lee.dara@shinhan.example.com', NULL,
   'PENDING_REVIEW', 'lee.dara@shinhan.example.com', '이다라', FALSE, NULL, NULL, NULL),
  ('AUTO_TRANSFER', '자동이체 통합조회', '240', -27960, -27780,
   '고객정보 시스템 점검으로 조회가 중단되었습니다.', '삼성증권 조회 차단',
   '최사아', '010-0000-4101', NULL, NULL,
   'COMPLETED', 'hong.gildong@kftc.example.com', '홍길동', TRUE, '차단과 해제를 마쳤습니다.', 'kim.ops@kftc.example.com', '김운영'),
  -- CMS
  ('CMS', '출금이체 전문 연계', '088', 1560, 1680,
   '코어뱅킹 정기 점검으로 CMS 출금이체가 중단됩니다.', '신한은행 출금 요청을 작업 뒤 일괄 재처리',
   '이다라', '010-0000-3101', 'lee.dara@shinhan.example.com', '공문 붙임 참고 바랍니다.',
   'CONFIRMED', 'lee.dara@shinhan.example.com', '이다라', FALSE, 'CMS운영팀이 재처리 일정을 잡았습니다. 상황실은 04:00 정상화 여부를 확인합니다.', 'park.cms@kftc.example.com', '박하늘'),
  ('CMS', '출금이체', 'TAPA', 7080, 7320,
   '보험료 수납 시스템 교체로 CMS 출금이 중단됩니다.', '타파보험 출금 요청 보류',
   '정타파', '010-0000-9101', NULL, NULL,
   'PENDING_REVIEW', 'park.cms@kftc.example.com', '박하늘', TRUE, NULL, NULL, NULL),
  ('CMS', '출금이체', 'JACHA', -19620, -19500,
   '카드 결제 시스템 작업으로 출금이 중단되었습니다.', '자차카드 출금 보류',
   '윤자차', '010-0000-8101', NULL, NULL,
   'COMPLETED', 'park.cms@kftc.example.com', '박하늘', TRUE, '재처리를 마쳤습니다.', 'park.cms@kftc.example.com', '박하늘'),
  -- 대출이동
  ('LOAN_TRANSFER', '신규기관 주택담보대출', 'PAHA', 4260, 4440,
   '대출 원장 시스템 점검으로 대출이동 조회가 중단됩니다.', '파하캐피탈 대출이동 조회 차단',
   '오파하', '010-0000-1201', NULL, NULL,
   'IN_REVIEW', 'kim.ops@kftc.example.com', '김운영', TRUE, NULL, NULL, NULL),
  ('LOAN_TRANSFER', '신용대출', 'TAPA', -35940, -35760,
   '약관대출 시스템 교체로 대출이동이 중단되었습니다.', '타파보험 대출이동 차단',
   '정타파', '010-0000-9101', NULL, NULL,
   'COMPLETED', 'kim.ops@kftc.example.com', '김운영', TRUE, '차단과 해제를 마쳤습니다.', 'kim.ops@kftc.example.com', '김운영')
) AS s (business, sub_business, org_code, start_min, end_min, reason, kftc_action, contact_name, contact_phone, contact_email, extra_note,
        status, registrant_email, registrant_name, registrant_kftc, handover, confirmed_by_email, confirmed_by_name)
JOIN organization o ON o.code = s.org_code;

-- 확인을 시작한 결제원 담당자. 확정한 사람은 확인자에도 들어 있다.
INSERT INTO suspension_reviewer (suspension_id, email, name, role_label, started_at, created_at, created_by, updated_at, updated_by)
SELECT su.id, r.email, r.name, r.role_label, su.created_at + INTERVAL '1' HOUR, su.created_at + INTERVAL '1' HOUR, r.email, su.created_at + INTERVAL '1' HOUR, r.email
FROM (VALUES
  ('004',  'ACCOUNT_INFO',  1800,   'hong.gildong@kftc.example.com', '홍길동', '현업'),
  ('004',  'ACCOUNT_INFO',  1800,   'kim.ops@kftc.example.com',      '김운영', '전산(운영)'),
  ('240',   'ACCOUNT_INFO',  5730,   'kim.ops@kftc.example.com',      '김운영', '전산(운영)'),
  ('050', 'ACCOUNT_INFO',  8940,   'hong.gildong@kftc.example.com', '홍길동', '현업'),
  ('103',  'ACCOUNT_INFO',  -12780, 'kim.ops@kftc.example.com',      '김운영', '전산(운영)'),
  ('004',  'ACCOUNT_INFO',  -56040, 'lee.control@kftc.example.com',  '이상황', '전산(상황실)'),
  ('JACHA', 'AUTO_TRANSFER', 1680,   'kim.ops@kftc.example.com',      '김운영', '전산(운영)'),
  ('240',   'AUTO_TRANSFER', -27780, 'kim.ops@kftc.example.com',      '김운영', '전산(운영)'),
  ('088',  'CMS',           1680,   'park.cms@kftc.example.com',     '박하늘', '현업'),
  ('JACHA', 'CMS',           -19500, 'park.cms@kftc.example.com',     '박하늘', '현업'),
  ('PAHA',  'LOAN_TRANSFER', 4440,   'lee.control@kftc.example.com',  '이상황', '전산(상황실)'),
  ('TAPA',  'LOAN_TRANSFER', -35760, 'kim.ops@kftc.example.com',      '김운영', '전산(운영)')
) AS r (org_code, business, end_min, email, name, role_label)
JOIN organization o ON o.code = r.org_code
JOIN suspension su ON su.organization_id = o.id AND su.business = r.business
                  AND su.end_at = CAST(CURRENT_DATE AS TIMESTAMP) + r.end_min * INTERVAL '1' MINUTE;
