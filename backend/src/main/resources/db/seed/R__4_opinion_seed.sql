-- 의견 요청 샘플. 전부 가상 데이터다. local, demo 프로필(과 테스트)에서만 읽는다.
-- 기관(R__1)·계정(R__2) 시드 뒤에 돈다. 날짜는 오늘 기준 상대값이라 시연 때 늘 임박·초과·마감이 함께 보인다.
-- 회신 담당자는 계정 시드의 기관 담당자이고, 계정이 없는 기관의 회신은 그때 쓴 사람 이름(가상)만 남긴다.
DELETE FROM attachment WHERE owner_type = 'OPINION_REQUEST';
DELETE FROM opinion_reminder;
DELETE FROM opinion_reply_answer;
DELETE FROM opinion_reply;
DELETE FROM opinion_target_recipient;
DELETE FROM opinion_target;
DELETE FROM opinion_question_option;
DELETE FROM opinion_question;
DELETE FROM opinion_section;
DELETE FROM opinion_request;

INSERT INTO opinion_request (business, title, content, due_date, status, owner_email, owner_name, opened_at, closed_at, cancelled, cancel_reason,
                             created_at, created_by, updated_at, updated_by)
SELECT r.business, r.title, r.content, CAST(CURRENT_DATE + r.due_days * INTERVAL '1' DAY AS DATE), r.status, r.owner_email, r.owner_name,
       CASE WHEN r.opened_days IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CURRENT_TIMESTAMP - r.opened_days * INTERVAL '1' DAY END,
       CASE WHEN r.closed_days IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CURRENT_TIMESTAMP - r.closed_days * INTERVAL '1' DAY END,
       r.cancelled, r.cancel_reason,
       CURRENT_TIMESTAMP - r.created_days * INTERVAL '1' DAY, r.owner_email, CURRENT_TIMESTAMP - COALESCE(r.closed_days, r.opened_days, r.created_days) * INTERVAL '1' DAY, r.owner_email
FROM (VALUES
  ('ACCOUNT_INFO', '계좌통합관리 수수료 체계 개편 의견 요청',
   '내년 1월부터 계좌통합관리 조회 수수료를 건별에서 구간별 정액으로 바꾸려고 합니다. 붙임 개편안을 보시고 기관 의견을 회신해 주세요. 적용 시기, 영향받는 업무, 조회 건수 질문에 답해 주세요.',
   3, 'OPEN', 'hong.sungmin@kftc.example.com', '홍성민', 7, CAST(NULL AS INT), 8, FALSE, CAST(NULL AS VARCHAR(500))),
  ('ACCOUNT_INFO', '4분기 계좌통합관리업무 회비 사전확인',
   '3분기(7~9월) 계좌정보 통합조회 실적으로 산정한 4분기 회비를 미리 알려 드립니다. 붙임 엑셀에서 기관 실적과 총 회비를 확인하고 의견없음 또는 확인 필요로 회신해 주세요. 확인이 필요하면 문의사항을 함께 적어 주세요.',
   7, 'OPEN', 'hong.sungmin@kftc.example.com', '홍성민', 3, NULL, 4, FALSE, NULL),
  ('ACCOUNT_INFO', '오픈뱅킹 연계 전문 항목 추가 검토',
   '계좌정보 통합조회 응답 전문에 계좌 별칭 항목(30바이트)을 더하는 안을 검토하고 있습니다. 기관 시스템 반영 가능 여부와 필요한 개발 기간을 알려 주세요.',
   12, 'OPEN', 'hong.sungmin@kftc.example.com', '홍성민', 2, NULL, 3, FALSE, NULL),
  ('ACCOUNT_INFO', '계좌정보 일괄해지 화면 문구 개선',
   '일괄해지 화면의 안내 문구를 고객이 이해하기 쉬운 표현으로 바꾸려고 합니다. 붙임 문구안에 대한 의견을 주세요.',
   -25, 'CLOSED', 'hong.sungmin@kftc.example.com', '홍성민', 40, 20, 41, FALSE, NULL),
  ('ACCOUNT_INFO', '계좌통합관리 수수료 고지 방식 변경',
   '수수료 고지서를 우편에서 전자 고지로 바꾸는 안에 대한 의견을 요청합니다.',
   -2, 'CLOSED', 'hong.sungmin@kftc.example.com', '홍성민', 15, 10, 16, TRUE,
   '수수료 체계 개편 의견 요청과 함께 다루기로 해서 이 요청은 취소합니다.'),
  ('ACCOUNT_INFO', '야간 배치 시간 조정 의견 요청',
   '야간 정산 배치 시작 시각을 23시에서 23시 30분으로 늦추는 안입니다. 기관 영향 범위를 확인해 주세요.',
   20, 'DRAFT', 'hong.sungmin@kftc.example.com', '홍성민', NULL, NULL, 1, FALSE, NULL),
  ('AUTO_TRANSFER', '자동이체 통합조회 해지 절차 개선',
   '자동이체 통합조회에서 해지 신청 뒤 기관 처리 결과를 고객에게 다시 알리는 절차를 더하려고 합니다. 기관 처리 시간과 의견을 회신해 주세요.',
   -1, 'OPEN', 'hong.sungmin@kftc.example.com', '홍성민', 9, NULL, 10, FALSE, NULL),
  ('CMS', 'CMS 출금이체 전문 개정 의견 요청',
   'CMS 출금이체 결과 전문의 오류 코드 체계를 정리하려고 합니다. 붙임 코드표를 보고 기관 시스템에 미치는 영향을 알려 주세요.',
   5, 'OPEN', 'park.cms@kftc.example.com', '박하늘', 4, NULL, 5, FALSE, NULL),
  ('LOAN_TRANSFER', '대출이동 신규기관 접수시간 연장',
   '대출이동 신규기관 접수 마감 시각을 17시에서 18시로 늦추는 안에 대한 의견을 요청했습니다.',
   -30, 'CLOSED', 'admin@kftc.example.com', '데모 관리자', 45, 28, 46, FALSE, NULL)
) AS r (business, title, content, due_days, status, owner_email, owner_name, opened_days, closed_days, created_days, cancelled, cancel_reason);

-- 섹션. 오픈뱅킹 전문 검토 요청은 반영 가능 여부(답)에 따라 "반영 계획" 이나 "반영이 어려운 이유" 섹션으로 가고,
-- "반영 계획" 을 마치면 회신이 끝난다(구글폼의 답변에 따른 섹션 이동). 나머지 요청은 제목 없는 섹션 하나다.
INSERT INTO opinion_section (request_id, sort_order, title, description, jump_type, jump_order, created_at, created_by, updated_at, updated_by)
SELECT r.id, s.sort_order, s.title, s.description, s.jump_type, s.jump_order, r.created_at, r.created_by, r.created_at, r.created_by
FROM (VALUES
  (0, '반영 가능 여부', '계좌 별칭 항목(30바이트)을 기관 시스템에 반영할 수 있는지 알려 주세요.', CAST(NULL AS VARCHAR(10)), CAST(NULL AS INT)),
  (1, '반영 계획', '반영할 수 있는 기관만 답합니다.', 'END', NULL),
  (2, '반영이 어려운 이유', '반영하기 어려운 기관만 답합니다.', NULL, NULL)
) AS s (sort_order, title, description, jump_type, jump_order)
JOIN opinion_request r ON r.title = '오픈뱅킹 연계 전문 항목 추가 검토';

INSERT INTO opinion_section (request_id, sort_order, created_at, created_by, updated_at, updated_by)
SELECT r.id, 0, r.created_at, r.created_by, r.created_at, r.created_by
FROM opinion_request r
WHERE r.title <> '오픈뱅킹 연계 전문 항목 추가 검토';

-- 질문. 수수료 개편 요청에 객관식(하나·여러 개)과 주관식을 하나씩 둔다.
INSERT INTO opinion_question (request_id, section_id, sort_order, question_type, title, required, created_at, created_by, updated_at, updated_by)
SELECT r.id, s.id, q.sort_order, q.question_type, q.title, q.required, r.created_at, r.created_by, r.created_at, r.created_by
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', 0, 'SINGLE_CHOICE',   '개편안을 언제부터 적용하는 것이 좋습니까?', TRUE),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 1, 'MULTIPLE_CHOICE', '기관에서 영향을 받는 업무를 모두 골라 주세요.', TRUE),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 2, 'TEXT',            '월평균 계좌통합관리 조회 건수를 적어 주세요.', FALSE)
) AS q (title_of_request, sort_order, question_type, title, required)
JOIN opinion_request r ON r.title = q.title_of_request
JOIN opinion_section s ON s.request_id = r.id AND s.sort_order = 0;

-- 오픈뱅킹 전문 검토 요청의 섹션별 질문.
INSERT INTO opinion_question (request_id, section_id, sort_order, question_type, title, required, created_at, created_by, updated_at, updated_by)
SELECT r.id, s.id, q.sort_order, q.question_type, q.title, q.required, r.created_at, r.created_by, r.created_at, r.created_by
FROM (VALUES
  (0, 0, 'SINGLE_CHOICE', '기관 시스템에 계좌 별칭 항목을 반영할 수 있습니까?', TRUE),
  (1, 1, 'TEXT',          '반영에 필요한 개발 기간을 적어 주세요.', TRUE),
  (1, 2, 'SINGLE_CHOICE', '반영 방식을 골라 주세요.', FALSE),
  (2, 3, 'TEXT',          '반영하기 어려운 이유와 대안을 적어 주세요.', TRUE)
) AS q (section_order, sort_order, question_type, title, required)
JOIN opinion_request r ON r.title = '오픈뱅킹 연계 전문 항목 추가 검토'
JOIN opinion_section s ON s.request_id = r.id AND s.sort_order = q.section_order;

-- 나머지 요청은 동의 여부(객관식, 필수)와 의견(주관식)을 묻는다. 결제원 담당자가 흔히 넣는 질문 모양이다.
INSERT INTO opinion_question (request_id, section_id, sort_order, question_type, title, required, created_at, created_by, updated_at, updated_by)
SELECT r.id, s.id, q.sort_order, q.question_type, q.title, q.required, r.created_at, r.created_by, r.created_at, r.created_by
FROM (VALUES
  (0, 'SINGLE_CHOICE', '이 안에 동의하십니까?', TRUE),
  (1, 'TEXT',          '조건이나 다른 의견이 있으면 이유와 함께 적어 주세요.', FALSE)
) AS q (sort_order, question_type, title, required)
CROSS JOIN opinion_request r
JOIN opinion_section s ON s.request_id = r.id AND s.sort_order = 0
WHERE r.title NOT IN ('계좌통합관리 수수료 체계 개편 의견 요청', '오픈뱅킹 연계 전문 항목 추가 검토', '4분기 계좌통합관리업무 회비 사전확인');

-- 회비 사전확인 요청은 확인 결과(객관식, 필수)와 문의사항(주관식, 선택)을 묻는다.
INSERT INTO opinion_question (request_id, section_id, sort_order, question_type, title, required, created_at, created_by, updated_at, updated_by)
SELECT r.id, s.id, q.sort_order, q.question_type, q.title, q.required, r.created_at, r.created_by, r.created_at, r.created_by
FROM (VALUES
  (0, 'SINGLE_CHOICE', '붙임 엑셀의 기관 실적과 4분기 총 회비를 확인해 주세요.', TRUE),
  (1, 'TEXT',          '확인이 필요한 내용이나 문의사항이 있으면 적어 주세요.', FALSE)
) AS q (sort_order, question_type, title, required)
JOIN opinion_request r ON r.title = '4분기 계좌통합관리업무 회비 사전확인'
JOIN opinion_section s ON s.request_id = r.id AND s.sort_order = 0;

INSERT INTO opinion_question_option (question_id, sort_order, label, created_at, created_by, updated_at, updated_by)
SELECT q.id, o.sort_order, o.label, q.created_at, q.created_by, q.created_at, q.created_by
FROM (VALUES
  (0, 0, '내년 1월'), (0, 1, '내년 7월'), (0, 2, '내후년 1월'),
  (1, 0, '수수료 정산'), (1, 1, '고객 안내'), (1, 2, '전산 개발'), (1, 3, '영향 없음')
) AS o (question_order, sort_order, label)
JOIN opinion_request r ON r.title = '계좌통합관리 수수료 체계 개편 의견 요청'
JOIN opinion_question q ON q.request_id = r.id AND q.sort_order = o.question_order;

-- 오픈뱅킹 전문 검토 요청의 선택지. 첫 질문은 답에 따라 섹션 2(반영 계획) 또는 섹션 3(반영이 어려운 이유)으로 간다.
INSERT INTO opinion_question_option (question_id, sort_order, label, jump_type, jump_order, created_at, created_by, updated_at, updated_by)
SELECT q.id, o.sort_order, o.label, o.jump_type, o.jump_order, q.created_at, q.created_by, q.created_at, q.created_by
FROM (VALUES
  (0, 0, '반영 가능', 'SECTION', 1),
  (0, 1, '반영 어려움', 'SECTION', 2),
  (2, 0, '정기 배포', CAST(NULL AS VARCHAR(10)), CAST(NULL AS INT)),
  (2, 1, '별도 긴급 배포', NULL, NULL)
) AS o (question_order, sort_order, label, jump_type, jump_order)
JOIN opinion_request r ON r.title = '오픈뱅킹 연계 전문 항목 추가 검토'
JOIN opinion_question q ON q.request_id = r.id AND q.sort_order = o.question_order;

INSERT INTO opinion_question_option (question_id, sort_order, label, created_at, created_by, updated_at, updated_by)
SELECT q.id, o.sort_order, o.label, q.created_at, q.created_by, q.created_at, q.created_by
FROM (VALUES (0, '동의'), (1, '조건부 동의'), (2, '이견')) AS o (sort_order, label)
JOIN opinion_question q ON q.sort_order = 0 AND q.title = '이 안에 동의하십니까?';

INSERT INTO opinion_question_option (question_id, sort_order, label, created_at, created_by, updated_at, updated_by)
SELECT q.id, o.sort_order, o.label, q.created_at, q.created_by, q.created_at, q.created_by
FROM (VALUES (0, '의견없음'), (1, '확인 필요')) AS o (sort_order, label)
JOIN opinion_request r ON r.title = '4분기 계좌통합관리업무 회비 사전확인'
JOIN opinion_question q ON q.request_id = r.id AND q.sort_order = 0;

-- 첨부자료. 회비 사전확인 요청에 기관별 실적·회비 엑셀(가상 수치)을 붙인다.
-- 저장소 키 자리의 seed:파일이름 은 서버가 뜰 때 SampleAttachmentLoader 가 db/seed/files/ 의 파일을 저장소에 넣고 실제 키·크기로 바꾼다.
INSERT INTO attachment (owner_type, owner_id, original_name, content_type, size_bytes, storage_key, created_at, created_by, updated_at, updated_by)
SELECT 'OPINION_REQUEST', r.id, '4분기_계좌통합관리업무_회비_사전확인.xlsx',
       'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 0, 'seed:account-info-fee-precheck.xlsx',
       r.created_at, r.created_by, r.created_at, r.created_by
FROM opinion_request r
WHERE r.title = '4분기 계좌통합관리업무 회비 사전확인';

-- 대상 기관.
INSERT INTO opinion_target (request_id, organization_id, created_at, created_by, updated_at, updated_by)
SELECT r.id, o.id, r.created_at, r.created_by, r.created_at, r.created_by
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', '004'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '088'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '050'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '240'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '048'),
  ('4분기 계좌통합관리업무 회비 사전확인', '004'),
  ('4분기 계좌통합관리업무 회비 사전확인', '088'),
  ('4분기 계좌통합관리업무 회비 사전확인', '240'),
  ('4분기 계좌통합관리업무 회비 사전확인', '050'),
  ('4분기 계좌통합관리업무 회비 사전확인', '048'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       '004'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       '240'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       '088'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       '050'),
  ('계좌정보 일괄해지 화면 문구 개선',        '004'),
  ('계좌정보 일괄해지 화면 문구 개선',        '240'),
  ('계좌정보 일괄해지 화면 문구 개선',        '088'),
  ('계좌정보 일괄해지 화면 문구 개선',        '050'),
  ('계좌통합관리 수수료 고지 방식 변경',      '004'),
  ('계좌통합관리 수수료 고지 방식 변경',      '240'),
  ('야간 배치 시간 조정 의견 요청',           '004'),
  ('야간 배치 시간 조정 의견 요청',           '088'),
  ('자동이체 통합조회 해지 절차 개선',        '088'),
  ('자동이체 통합조회 해지 절차 개선',        '004'),
  ('자동이체 통합조회 해지 절차 개선',        '071'),
  ('자동이체 통합조회 해지 절차 개선',        '048'),
  ('CMS 출금이체 전문 개정 의견 요청',        '088'),
  ('CMS 출금이체 전문 개정 의견 요청',        '071'),
  ('CMS 출금이체 전문 개정 의견 요청',        '020'),
  ('CMS 출금이체 전문 개정 의견 요청',        '048'),
  ('대출이동 신규기관 접수시간 연장',         '004'),
  ('대출이동 신규기관 접수시간 연장',         '020'),
  ('대출이동 신규기관 접수시간 연장',         '090')
) AS t (title, org_code)
JOIN opinion_request r ON r.title = t.title
JOIN organization o ON o.code = t.org_code;

-- 회신 담당자. 계정 시드의 기관 담당자이며 그 업무를 맡은 사람만 고른다(연락처 시드에도 같은 이메일로 있다).
INSERT INTO opinion_target_recipient (target_id, email, name, sub_business, created_at, created_by, updated_at, updated_by)
SELECT t.id, u.email, u.name, b.sub_business, t.created_at, t.created_by, t.created_at, t.created_by
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'kim.minjun@kb.example.com'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'choi.yujin@samsung.example.com'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'kim.minjun@kb.example.com'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'choi.yujin@samsung.example.com'),
  ('계좌정보 일괄해지 화면 문구 개선',        'kim.minjun@kb.example.com'),
  ('계좌정보 일괄해지 화면 문구 개선',        'choi.yujin@samsung.example.com'),
  ('계좌통합관리 수수료 고지 방식 변경',      'kim.minjun@kb.example.com'),
  ('야간 배치 시간 조정 의견 요청',           'kim.minjun@kb.example.com'),
  ('자동이체 통합조회 해지 절차 개선',        'lee.junho@shinhan.example.com'),
  ('CMS 출금이체 전문 개정 의견 요청',        'lee.junho@shinhan.example.com')
) AS p (title, email)
JOIN opinion_request r ON r.title = p.title
JOIN user_account u ON u.email = p.email
JOIN opinion_target t ON t.request_id = r.id AND t.organization_id = u.organization_id
JOIN user_account_business b ON b.account_id = u.id AND b.business = r.business;

-- 회비 사전확인 요청의 회신 담당자는 연락처(R__7)에서 계좌통합관리 세부업무 "회비" 담당자를 고른 것이다(계정 없는 담당자).
-- 연락처 시드는 이 파일 뒤에 돌아서, 같은 사람(이름·이메일·세부업무)을 여기에 적는다.
INSERT INTO opinion_target_recipient (target_id, email, name, sub_business, created_at, created_by, updated_at, updated_by)
SELECT t.id, p.email, p.name, '회비', t.created_at, t.created_by, t.created_at, t.created_by
FROM (VALUES
  ('004', 'bae.sua@kb.example.com',          '배수아'),
  ('088', 'oh.taeyun@shinhan.example.com',   '오태윤'),
  ('240', 'kwon.dohyun@samsung.example.com', '권도현'),
  ('050', 'maeng.soyul@fsb.example.com',     '맹소율'),
  ('048', 'pi.jaehyun@cu.example.com',       '피재현')
) AS p (org_code, email, name)
JOIN organization o ON o.code = p.org_code
JOIN opinion_request r ON r.title = '4분기 계좌통합관리업무 회비 사전확인'
JOIN opinion_target t ON t.request_id = r.id AND t.organization_id = o.id;

-- 기관 회신. submitted_days 가 있으면 제출한 회신이다. 회비 사전확인은 기관의 회비 담당자(계정 없음)가 회신했고, KB국민은행은 아직 회신하지 않았고(같은 기관 계정 김민준으로 회신해 볼 수 있다) 신협은 임시저장이다. 삼성증권은 수수료 개편 회신을 제출한 뒤 수정 요청을 해 두었다.
INSERT INTO opinion_reply (target_id, status, replier_email, replier_name, submitted_at,
                           revision_reason, revision_requested_at, revision_rejection_reason, created_at, created_by, updated_at, updated_by)
SELECT t.id, p.status, p.replier_email, p.replier_name,
       CASE WHEN p.submitted_days IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CURRENT_TIMESTAMP - p.submitted_days * INTERVAL '1' DAY END,
       p.revision_reason,
       CASE WHEN p.revision_reason IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CURRENT_TIMESTAMP - INTERVAL '5' HOUR END,
       CAST(NULL AS VARCHAR(500)),
       CURRENT_TIMESTAMP - COALESCE(p.submitted_days, 1) * INTERVAL '1' DAY, p.replier_email,
       CURRENT_TIMESTAMP - COALESCE(p.submitted_days, 1) * INTERVAL '1' DAY, p.replier_email
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', '088', 'SUBMITTED', 'park.jihoon@shinhan.example.com', '박지훈', 5, CAST(NULL AS VARCHAR(500))),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '050', 'SUBMITTED', 'yoon.seoyeon@fsb.example.com', '윤서연', 3, NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '240', 'REVISION_REQUESTED', 'choi.yujin@samsung.example.com', '최유진', 2, '내부 검토 결과 적용 시기를 내년 7월로 바꾸고 싶습니다.'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '048', 'DRAFT', 'kang.doyoon@cu.example.com', '강도윤', NULL, NULL),
  ('4분기 계좌통합관리업무 회비 사전확인', '088', 'SUBMITTED', 'oh.taeyun@shinhan.example.com', '오태윤', 2, NULL),
  ('4분기 계좌통합관리업무 회비 사전확인', '240', 'SUBMITTED', 'kwon.dohyun@samsung.example.com', '권도현', 2, NULL),
  ('4분기 계좌통합관리업무 회비 사전확인', '050', 'SUBMITTED', 'maeng.soyul@fsb.example.com', '맹소율', 1, NULL),
  ('4분기 계좌통합관리업무 회비 사전확인', '048', 'DRAFT', 'pi.jaehyun@cu.example.com', '피재현', NULL, NULL),
  ('오픈뱅킹 연계 전문 항목 추가 검토', '088', 'SUBMITTED', 'park.jihoon@shinhan.example.com', '박지훈', 1, NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '004', 'SUBMITTED', 'kim.minjun@kb.example.com', '김민준', 30, NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '240', 'SUBMITTED', 'choi.yujin@samsung.example.com', '최유진', 29, NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '088', 'SUBMITTED', 'park.jihoon@shinhan.example.com', '박지훈', 27, NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '050', 'SUBMITTED', 'yoon.seoyeon@fsb.example.com', '윤서연', 26, NULL),
  ('자동이체 통합조회 해지 절차 개선', '004', 'SUBMITTED', 'han.gana2@kb.example.com', '한가람', 6, NULL),
  ('자동이체 통합조회 해지 절차 개선', '071', 'SUBMITTED', 'yoon.haeun@epost.example.com', '윤하은', 4, NULL),
  ('CMS 출금이체 전문 개정 의견 요청', '020', 'SUBMITTED', 'jung.jaewon@woori.example.com', '정재원', 1, NULL),
  ('대출이동 신규기관 접수시간 연장', '004', 'SUBMITTED', 'kim.minjun@kb.example.com', '김민준', 35, NULL),
  ('대출이동 신규기관 접수시간 연장', '090', 'SUBMITTED', 'seok.daon@kakaobank.example.com', '석다온', 34, NULL),
  ('대출이동 신규기관 접수시간 연장', '020', 'SUBMITTED', 'jung.jaewon@woori.example.com', '정재원', 33, NULL)
) AS p (title, org_code, status, replier_email, replier_name, submitted_days, revision_reason)
JOIN opinion_request r ON r.title = p.title
JOIN organization o ON o.code = p.org_code
JOIN opinion_target t ON t.request_id = r.id AND t.organization_id = o.id;

-- 질문 답. 제출한 기관의 답과 신협이 수수료 개편 요청에 임시저장한 답이다. 객관식은 고른 선택지마다 한 행이다.
INSERT INTO opinion_reply_answer (reply_id, question_id, stage, option_id, text_value, created_at, created_by, updated_at, updated_by)
SELECT p.id, q.id, a.stage, o.id, a.text_value, p.created_at, p.created_by, p.created_at, p.created_by
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', '088', 'SUBMITTED', 0, '내년 1월', CAST(NULL AS VARCHAR(4000))),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '088', 'SUBMITTED', 1, '수수료 정산', NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '088', 'SUBMITTED', 2, NULL, '약 12만 건'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '050', 'SUBMITTED', 0, '내년 7월', NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '050', 'SUBMITTED', 1, '수수료 정산', NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '050', 'SUBMITTED', 1, '전산 개발', NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '240', 'SUBMITTED', 0, '내년 1월', NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '240', 'SUBMITTED', 1, '영향 없음', NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '240', 'SUBMITTED', 2, NULL, '약 3만 건'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '048', 'DRAFT', 0, '내후년 1월', NULL),
  ('4분기 계좌통합관리업무 회비 사전확인', '088', 'SUBMITTED', 0, '의견없음', NULL),
  ('4분기 계좌통합관리업무 회비 사전확인', '240', 'SUBMITTED', 0, '확인 필요', NULL),
  ('4분기 계좌통합관리업무 회비 사전확인', '240', 'SUBMITTED', 1, NULL, '8월 실적(398,200건)에 시스템 점검 중 재조회된 건이 포함된 것 같습니다. 중복 건을 빼고 다시 산정해 주실 수 있을까요?'),
  ('4분기 계좌통합관리업무 회비 사전확인', '050', 'SUBMITTED', 0, '확인 필요', NULL),
  ('4분기 계좌통합관리업무 회비 사전확인', '050', 'SUBMITTED', 1, NULL, '중앙회 경유 회원 저축은행 조회분이 함께 집계된 것인지 확인 부탁드립니다. 기본회비가 기관별로 부과되는지도 궁금합니다.'),
  ('4분기 계좌통합관리업무 회비 사전확인', '048', 'DRAFT', 0, '의견없음', NULL),
  ('오픈뱅킹 연계 전문 항목 추가 검토', '088', 'SUBMITTED', 0, '반영 가능', NULL),
  ('오픈뱅킹 연계 전문 항목 추가 검토', '088', 'SUBMITTED', 1, NULL, '약 2개월. 다음 분기 정기 배포에 반영할 수 있습니다.'),
  ('오픈뱅킹 연계 전문 항목 추가 검토', '088', 'SUBMITTED', 2, '정기 배포', NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '004', 'SUBMITTED', 0, '동의', NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '240', 'SUBMITTED', 0, '조건부 동의', NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '240', 'SUBMITTED', 1, NULL, '"해지" 대신 "연결 끊기" 표현을 함께 써 주세요.'),
  ('계좌정보 일괄해지 화면 문구 개선', '088', 'SUBMITTED', 0, '동의', NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '050', 'SUBMITTED', 0, '이견', NULL),
  ('계좌정보 일괄해지 화면 문구 개선', '050', 'SUBMITTED', 1, NULL, '고객 문의가 늘 수 있어 지금 문구를 유지하기를 바랍니다.'),
  ('자동이체 통합조회 해지 절차 개선', '004', 'SUBMITTED', 0, '동의', NULL),
  ('자동이체 통합조회 해지 절차 개선', '071', 'SUBMITTED', 0, '조건부 동의', NULL),
  ('자동이체 통합조회 해지 절차 개선', '071', 'SUBMITTED', 1, NULL, '처리 결과 통지는 영업일 기준 2일 안으로 해 주세요.'),
  ('CMS 출금이체 전문 개정 의견 요청', '020', 'SUBMITTED', 0, '동의', NULL),
  ('대출이동 신규기관 접수시간 연장', '004', 'SUBMITTED', 0, '동의', NULL),
  ('대출이동 신규기관 접수시간 연장', '090', 'SUBMITTED', 0, '조건부 동의', NULL),
  ('대출이동 신규기관 접수시간 연장', '090', 'SUBMITTED', 1, NULL, '18시 연장 시 상담 인력 배치가 필요해 시행을 한 달 늦춰 주세요.'),
  ('대출이동 신규기관 접수시간 연장', '020', 'SUBMITTED', 0, '동의', NULL)
) AS a (title, org_code, stage, question_order, option_label, text_value)
JOIN opinion_request r ON r.title = a.title
JOIN organization g ON g.code = a.org_code
JOIN opinion_target t ON t.request_id = r.id AND t.organization_id = g.id
JOIN opinion_reply p ON p.target_id = t.id
JOIN opinion_question q ON q.request_id = r.id AND q.sort_order = a.question_order
LEFT JOIN opinion_question_option o ON o.question_id = q.id AND o.label = a.option_label;

-- 리마인드 메일 이력.
INSERT INTO opinion_reminder (request_id, sender_name, organization_count, recipient_count, organization_names, created_at, created_by, updated_at, updated_by)
SELECT r.id, m.sender_name, m.organization_count, m.recipient_count, m.organization_names,
       CURRENT_TIMESTAMP - m.sent_days * INTERVAL '1' DAY, r.owner_email, CURRENT_TIMESTAMP - m.sent_days * INTERVAL '1' DAY, r.owner_email
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', '홍성민', 2, 1, 'KB국민은행, 신협', 2),
  ('4분기 계좌통합관리업무 회비 사전확인',     '홍성민', 2, 2, 'KB국민은행, 신협', 1),
  ('자동이체 통합조회 해지 절차 개선',        '홍성민', 1, 1, '신한은행', 1)
) AS m (title, sender_name, organization_count, recipient_count, organization_names, sent_days)
JOIN opinion_request r ON r.title = m.title;
