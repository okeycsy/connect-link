-- 기관 테스트 샘플. 전부 가상 데이터다. local, demo 프로필(과 테스트)에서만 읽는다.
-- 기관(R__1)·계정(R__2) 시드 뒤에 돈다. 날짜는 오늘 기준 상대값이라 시연 때 늘 진행 중·기간 지남·수행 완료가 함께 보인다.
-- progress_rate·completed_count·completed_at 은 서버 계산(TestProgress)과 같은 값을 손으로 넣었다: 요구사항 수행률은 유효한 결제원 미니 수행률, 없으면 정상 100 그 밖 0,
-- 기관·테스트 수행률은 그 평균(반올림), 완료는 정상 + 미니 수행률 100(결과 등록 뒤에 남긴 의견)이다. 결과를 바꾸면 이 숫자도 함께 고친다.
DELETE FROM test_item_result;
DELETE FROM test_plan_recipient;
DELETE FROM test_plan_target;
DELETE FROM test_item;
DELETE FROM test_plan;

INSERT INTO test_plan (business, sub_business, title, description, start_date, end_date, owner_email, owner_name, progress_rate, completed_at,
                       created_at, created_by, updated_at, updated_by)
SELECT p.business, p.sub_business, p.title, p.description,
       CURRENT_DATE + p.start_day * INTERVAL '1' DAY, CURRENT_DATE + p.end_day * INTERVAL '1' DAY,
       p.owner_email, p.owner_name, p.progress_rate,
       CASE WHEN p.completed_day IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CAST(CURRENT_DATE AS TIMESTAMP) + p.completed_day * INTERVAL '1' DAY + INTERVAL '15' HOUR END,
       CAST(CURRENT_DATE AS TIMESTAMP) + (p.start_day - 3) * INTERVAL '1' DAY + INTERVAL '10' HOUR, p.owner_email,
       CAST(CURRENT_DATE AS TIMESTAMP) + (p.start_day - 3) * INTERVAL '1' DAY + INTERVAL '10' HOUR, p.owner_email
FROM (VALUES
  ('ACCOUNT_INFO', '계좌정보 조회·해지', '계좌정보 통합조회 전문 개선 2차',
   '잔액·거래내역 조회 전문 개정과 해지 사유코드 추가를 기관별로 확인합니다. 테스트 서버 접속 정보와 전문 규격은 첨부 가이드를 봐 주세요.',
   -7, 7, 'hong.sungmin@kftc.example.com', '홍성민', 49, CAST(NULL AS INT)),
  ('ACCOUNT_INFO', '내 계좌 한눈에', '내 계좌 한눈에 연계 개선',
   '보유 계좌 목록 응답에 휴면 여부 항목을 더한 연계 개선 테스트입니다.',
   -40, -25, 'hong.sungmin@kftc.example.com', '홍성민', 100, -24),
  ('ACCOUNT_INFO', '오픈뱅킹 안심차단', '오픈뱅킹 안심차단 등록 전문 신설',
   '안심차단 등록·해제 전문을 새로 만들었습니다. 등록, 해제, 중복 등록 오류 세 가지를 확인합니다.',
   -20, -2, 'kim.ops@kftc.example.com', '김태호', 17, NULL),
  ('AUTO_TRANSFER', '자동이체 통합조회', '자동이체 통합조회 응답 항목 추가',
   '자동이체 통합조회 응답에 수납기관 연락처와 최근 출금일 항목을 더합니다.',
   -3, 10, 'hong.sungmin@kftc.example.com', '홍성민', 17, NULL),
  ('CMS', '출금이체', '출금이체 전문 연계 테스트',
   '출금이체 요청·결과 전문의 신규 연계 테스트입니다. 이체 한도 초과와 잔액 부족 응답을 꼭 확인해 주세요.',
   -5, 5, 'park.cms@kftc.example.com', '박하늘', 27, NULL)
) AS p (business, sub_business, title, description, start_day, end_day, owner_email, owner_name, progress_rate, completed_day);

-- 요구사항. 번호는 sort_order 순서로 001, 002 … 이다.
INSERT INTO test_item (test_plan_id, sort_order, title, description, created_at, created_by, updated_at, updated_by)
SELECT p.id, i.sort_order, i.title, i.description, p.created_at, p.created_by, p.created_at, p.created_by
FROM (VALUES
  ('계좌정보 통합조회 전문 개선 2차', 0, '잔액 조회 전문 응답 확인', '개정된 잔액 조회 전문(001)에 새 항목(가용잔액)이 채워져 오는지 확인합니다.'),
  ('계좌정보 통합조회 전문 개선 2차', 1, '거래내역 조회 페이지 넘김 확인', '거래내역 조회 전문(002)에서 100건이 넘을 때 다음 페이지 키로 이어 받는지 확인합니다.'),
  ('계좌정보 통합조회 전문 개선 2차', 2, '해지 요청 전문 처리 확인', '해지 요청 전문(003)의 새 해지 사유코드(07~09)를 모두 처리하는지 확인합니다.'),
  ('계좌정보 통합조회 전문 개선 2차', 3, '오류 응답코드 매핑 확인', '기관 오류를 표준 응답코드로 바꿔 보내는지 확인합니다.'),
  ('계좌정보 통합조회 전문 개선 2차', 4, '대량 조회 응답 시간 확인', CAST('계좌 50개 보유 고객 조회가 3초 안에 끝나는지 확인합니다.' AS VARCHAR(2000))),
  ('내 계좌 한눈에 연계 개선', 0, '보유 계좌 목록 휴면 여부 항목 확인', NULL),
  ('내 계좌 한눈에 연계 개선', 1, '휴면 계좌 상세 조회 차단 확인', NULL),
  ('내 계좌 한눈에 연계 개선', 2, '기존 항목 회귀 확인', '휴면 여부를 더해도 기존 항목 값이 그대로인지 확인합니다.'),
  ('오픈뱅킹 안심차단 등록 전문 신설', 0, '안심차단 등록 전문 처리', NULL),
  ('오픈뱅킹 안심차단 등록 전문 신설', 1, '안심차단 해제 전문 처리', NULL),
  ('오픈뱅킹 안심차단 등록 전문 신설', 2, '중복 등록 오류 응답 확인', '이미 등록한 고객을 다시 등록하면 응답코드 A21 을 보내는지 확인합니다.'),
  ('자동이체 통합조회 응답 항목 추가', 0, '수납기관 연락처 항목 확인', NULL),
  ('자동이체 통합조회 응답 항목 추가', 1, '최근 출금일 항목 확인', NULL),
  ('자동이체 통합조회 응답 항목 추가', 2, '출금 이력 없는 자동이체 처리', '최근 출금일이 없으면 빈 값으로 보내는지 확인합니다.'),
  ('자동이체 통합조회 응답 항목 추가', 3, '조회 응답 시간 확인', NULL),
  ('출금이체 전문 연계 테스트', 0, '출금이체 요청 전문 송수신', NULL),
  ('출금이체 전문 연계 테스트', 1, '이체 한도 초과 응답 확인', NULL),
  ('출금이체 전문 연계 테스트', 2, '잔액 부족 응답 확인', NULL)
) AS i (plan_title, sort_order, title, description)
JOIN test_plan p ON p.title = i.plan_title;

-- 대상 기관과 기관별 수행률·완료 요구사항 수·마지막 등록 일시(오늘 0시 기준 시간).
INSERT INTO test_plan_target (test_plan_id, organization_id, progress_rate, completed_count, last_reported_at, created_at, created_by, updated_at, updated_by)
SELECT p.id, o.id, t.progress_rate, t.completed_count,
       CASE WHEN t.last_hour IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CAST(CURRENT_DATE AS TIMESTAMP) + t.last_hour * INTERVAL '1' HOUR END,
       p.created_at, p.created_by, p.created_at, p.created_by
FROM (VALUES
  ('계좌정보 통합조회 전문 개선 2차', '004',  56,  2, -20),
  ('계좌정보 통합조회 전문 개선 2차', '088',  40,  1, -44),
  ('계좌정보 통합조회 전문 개선 2차', '240',   0,   0, CAST(NULL AS INT)),
  ('계좌정보 통합조회 전문 개선 2차', '050', 100, 4, -70),
  ('내 계좌 한눈에 연계 개선',        '004',  100, 3, -620),
  ('내 계좌 한눈에 연계 개선',        '088',  100, 3, -640),
  ('내 계좌 한눈에 연계 개선',        '048',  100, 3, -630),
  ('오픈뱅킹 안심차단 등록 전문 신설', '240',   33,  0, -200),
  ('오픈뱅킹 안심차단 등록 전문 신설', '050', 0,   0, -150),
  ('자동이체 통합조회 응답 항목 추가', '088',  50,  2, -30),
  ('자동이체 통합조회 응답 항목 추가', '240',   0,   0, CAST(NULL AS INT)),
  ('자동이체 통합조회 응답 항목 추가', '048',  0,   0, NULL),
  ('출금이체 전문 연계 테스트',        '088',  53,  0, -6),
  ('출금이체 전문 연계 테스트',        '071', 0,   0, NULL)
) AS t (plan_title, org_code, progress_rate, completed_count, last_hour)
JOIN test_plan p ON p.title = t.plan_title
JOIN organization o ON o.code = t.org_code;

-- 기관 결과와 결제원 의견. 시각은 오늘 0시 기준 시간(reported_hour, feedback_hour)이다.
-- 출금이체 002 는 결제원 의견(70%) 뒤에 신한은행이 결과를 다시 등록해 "다시 확인" 상태다.
INSERT INTO test_item_result (test_item_id, test_plan_target_id, result, note, reported_by_email, reported_by_name, reported_at,
                              feedback_rate, feedback_note, feedback_by_email, feedback_by_name, feedback_at,
                              created_at, created_by, updated_at, updated_by)
SELECT i.id, t.id, r.result, r.note, r.reporter_email, r.reporter_name,
       CAST(CURRENT_DATE AS TIMESTAMP) + r.reported_hour * INTERVAL '1' HOUR,
       r.feedback_rate, r.feedback_note, r.feedback_email, r.feedback_name,
       CASE WHEN r.feedback_hour IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CAST(CURRENT_DATE AS TIMESTAMP) + r.feedback_hour * INTERVAL '1' HOUR END,
       CAST(CURRENT_DATE AS TIMESTAMP) + r.reported_hour * INTERVAL '1' HOUR, r.reporter_email,
       CAST(CURRENT_DATE AS TIMESTAMP) + COALESCE(r.feedback_hour, r.reported_hour) * INTERVAL '1' HOUR, COALESCE(r.feedback_email, r.reporter_email)
FROM (VALUES
  -- 계좌정보 통합조회 전문 개선 2차: KB국민은행 56%(완료 2), 신한은행 40%(완료 1), 삼성증권 미등록, 저축은행중앙회 100%(완료 4)
  ('계좌정보 통합조회 전문 개선 2차', '004', 0, 'PASSED', CAST(NULL AS VARCHAR(2000)), 'kim.minjun@kb.example.com', '김민준', -50,
   100, CAST(NULL AS VARCHAR(2000)), 'hong.sungmin@kftc.example.com', '홍성민', -30),
  ('계좌정보 통합조회 전문 개선 2차', '004', 1, 'PASSED', NULL, 'kim.minjun@kb.example.com', '김민준', -50,
   100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -30),
  ('계좌정보 통합조회 전문 개선 2차', '004', 2, 'PASSED', '사유코드 07, 08 은 확인했습니다.', 'kim.minjun@kb.example.com', '김민준', -50,
   80, '003 전문에 대해 예외사항 발생: 해지 사유코드 09 를 처리하지 못해 재확인이 필요합니다.', 'hong.sungmin@kftc.example.com', '홍성민', -30),
  ('계좌정보 통합조회 전문 개선 2차', '004', 3, 'FAILED', '기관 오류 E31 을 표준 응답코드 대신 E99 로 내려보냅니다. 수정 배포 예정입니다.', 'kim.minjun@kb.example.com', '김민준', -20,
   CAST(NULL AS INT), NULL, CAST(NULL AS VARCHAR(200)), CAST(NULL AS VARCHAR(50)), CAST(NULL AS INT)),
  ('계좌정보 통합조회 전문 개선 2차', '004', 4, 'IN_PROGRESS', NULL, 'kim.minjun@kb.example.com', '김민준', -20,
   NULL, NULL, NULL, NULL, NULL),
  ('계좌정보 통합조회 전문 개선 2차', '088', 0, 'PASSED', NULL, 'park.jihoon@shinhan.example.com', '박지훈', -60,
   100, NULL, 'kim.ops@kftc.example.com', '김태호', -40),
  ('계좌정보 통합조회 전문 개선 2차', '088', 1, 'PASSED', NULL, 'park.jihoon@shinhan.example.com', '박지훈', -44,
   NULL, NULL, NULL, NULL, NULL),
  ('계좌정보 통합조회 전문 개선 2차', '088', 2, 'NOT_STARTED', '테스트 서버 반영 일정 조율 중입니다.', 'park.jihoon@shinhan.example.com', '박지훈', -44,
   NULL, NULL, NULL, NULL, NULL),
  ('계좌정보 통합조회 전문 개선 2차', '050', 0, 'PASSED', NULL, 'yoon.seoyeon@fsb.example.com', '윤서연', -90,
   100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -72),
  ('계좌정보 통합조회 전문 개선 2차', '050', 1, 'PASSED', NULL, 'yoon.seoyeon@fsb.example.com', '윤서연', -90,
   100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -72),
  ('계좌정보 통합조회 전문 개선 2차', '050', 2, 'PASSED', NULL, 'yoon.seoyeon@fsb.example.com', '윤서연', -90,
   100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -72),
  ('계좌정보 통합조회 전문 개선 2차', '050', 3, 'PASSED', NULL, 'yoon.seoyeon@fsb.example.com', '윤서연', -90,
   100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -72),
  ('계좌정보 통합조회 전문 개선 2차', '050', 4, 'PASSED', '평균 1.8초입니다.', 'yoon.seoyeon@fsb.example.com', '윤서연', -70,
   NULL, NULL, NULL, NULL, NULL),
  -- 내 계좌 한눈에 연계 개선: 세 기관 모두 완료(수행 완료)
  ('내 계좌 한눈에 연계 개선', '004', 0, 'PASSED', NULL, 'kim.minjun@kb.example.com', '김민준', -620, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  ('내 계좌 한눈에 연계 개선', '004', 1, 'PASSED', NULL, 'kim.minjun@kb.example.com', '김민준', -620, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  ('내 계좌 한눈에 연계 개선', '004', 2, 'PASSED', NULL, 'kim.minjun@kb.example.com', '김민준', -620, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  ('내 계좌 한눈에 연계 개선', '088', 0, 'PASSED', NULL, 'park.jihoon@shinhan.example.com', '박지훈', -640, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  ('내 계좌 한눈에 연계 개선', '088', 1, 'PASSED', NULL, 'park.jihoon@shinhan.example.com', '박지훈', -640, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  ('내 계좌 한눈에 연계 개선', '088', 2, 'PASSED', NULL, 'park.jihoon@shinhan.example.com', '박지훈', -640, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  ('내 계좌 한눈에 연계 개선', '048', 0, 'PASSED', NULL, 'jung.hyunwoo@cu.example.com', '정현우', -630, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  ('내 계좌 한눈에 연계 개선', '048', 1, 'PASSED', NULL, 'jung.hyunwoo@cu.example.com', '정현우', -630, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  ('내 계좌 한눈에 연계 개선', '048', 2, 'PASSED', NULL, 'jung.hyunwoo@cu.example.com', '정현우', -630, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -600),
  -- 오픈뱅킹 안심차단 등록 전문 신설(기간 지남): 삼성증권 33%, 저축은행중앙회 0%
  ('오픈뱅킹 안심차단 등록 전문 신설', '240', 0, 'PASSED', NULL, 'choi.yujin@samsung.example.com', '최유진', -200, NULL, NULL, NULL, NULL, NULL),
  ('오픈뱅킹 안심차단 등록 전문 신설', '050', 0, 'FAILED', '등록 전문의 고객 식별번호 길이가 맞지 않아 거절됩니다.', 'yoon.seoyeon@fsb.example.com', '윤서연', -150, NULL, NULL, NULL, NULL, NULL),
  ('오픈뱅킹 안심차단 등록 전문 신설', '050', 1, 'NOT_STARTED', NULL, 'yoon.seoyeon@fsb.example.com', '윤서연', -150, NULL, NULL, NULL, NULL, NULL),
  -- 자동이체 통합조회 응답 항목 추가: 신한은행 50%(완료 2), 삼성증권·신협은 그 업무 계정이 아직 없어 미등록
  ('자동이체 통합조회 응답 항목 추가', '088', 0, 'PASSED', NULL, 'lee.junho@shinhan.example.com', '이준호', -40, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -28),
  ('자동이체 통합조회 응답 항목 추가', '088', 1, 'FAILED', '최근 출금일이 YYYYMMDD 대신 YYMMDD 로 내려옵니다.', 'lee.junho@shinhan.example.com', '이준호', -30, NULL, NULL, NULL, NULL, NULL),
  ('자동이체 통합조회 응답 항목 추가', '088', 2, 'IN_PROGRESS', '출금 이력 없는 테스트 고객을 만드는 중입니다.', 'lee.junho@shinhan.example.com', '이준호', -30, NULL, NULL, NULL, NULL, NULL),
  ('자동이체 통합조회 응답 항목 추가', '088', 3, 'PASSED', NULL, 'lee.junho@shinhan.example.com', '이준호', -40, 100, NULL, 'hong.sungmin@kftc.example.com', '홍성민', -28),
  -- 출금이체 전문 연계 테스트: 신한은행 53%(002 는 결제원 의견 뒤에 다시 등록해 다시 확인 필요)
  ('출금이체 전문 연계 테스트', '088', 0, 'PASSED', NULL, 'lee.junho@shinhan.example.com', '이준호', -30, 60, '요청 전문은 정상이나 결과 전문 회신이 30초를 넘는 건이 있습니다.', 'park.cms@kftc.example.com', '박하늘', -20),
  ('출금이체 전문 연계 테스트', '088', 1, 'PASSED', '한도 초과 응답코드를 고쳐 다시 확인했습니다.', 'lee.junho@shinhan.example.com', '이준호', -6, 70, '한도 초과 응답코드가 표준과 다릅니다.', 'park.cms@kftc.example.com', '박하늘', -20),
  ('출금이체 전문 연계 테스트', '088', 2, 'IN_PROGRESS', NULL, 'lee.junho@shinhan.example.com', '이준호', -6, NULL, NULL, NULL, NULL, NULL)
) AS r (plan_title, org_code, sort_order, result, note, reporter_email, reporter_name, reported_hour,
        feedback_rate, feedback_note, feedback_email, feedback_name, feedback_hour)
JOIN test_plan p ON p.title = r.plan_title
JOIN test_item i ON i.test_plan_id = p.id AND i.sort_order = r.sort_order
JOIN organization o ON o.code = r.org_code
JOIN test_plan_target t ON t.test_plan_id = p.id AND t.organization_id = o.id;

-- 결과 등록 매터모스트 알림 대상. 만든 사람에 더해 몇 테스트에는 운영 담당자를 넣었다.
INSERT INTO test_plan_recipient (test_plan_id, account_id, email, name, created_at, created_by, updated_at, updated_by)
SELECT p.id, u.id, u.email, u.name, p.created_at, p.created_by, p.created_at, p.created_by
FROM (VALUES
  ('계좌정보 통합조회 전문 개선 2차', 'hong.sungmin@kftc.example.com'),
  ('계좌정보 통합조회 전문 개선 2차', 'kim.ops@kftc.example.com'),
  ('내 계좌 한눈에 연계 개선',        'hong.sungmin@kftc.example.com'),
  ('오픈뱅킹 안심차단 등록 전문 신설', 'kim.ops@kftc.example.com'),
  ('자동이체 통합조회 응답 항목 추가', 'hong.sungmin@kftc.example.com'),
  ('출금이체 전문 연계 테스트',        'park.cms@kftc.example.com')
) AS r (plan_title, email)
JOIN test_plan p ON p.title = r.plan_title
JOIN user_account u ON u.email = r.email;
