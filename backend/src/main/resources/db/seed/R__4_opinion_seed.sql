-- 의견 요청 샘플. 전부 가상 데이터다. local, demo 프로필(과 테스트)에서만 읽는다.
-- 기관(R__1)·계정(R__2) 시드 뒤에 돈다. 날짜는 오늘 기준 상대값이라 시연 때 늘 임박·초과·마감이 함께 보인다.
-- 회신 담당자는 계정 시드의 기관 담당자이고, 계정이 없는 기관의 회신은 그때 쓴 사람 이름(가상)만 남긴다.
DELETE FROM opinion_reminder;
DELETE FROM opinion_reply;
DELETE FROM opinion_target_recipient;
DELETE FROM opinion_target;
DELETE FROM opinion_reply_form;
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
   '내년 1월부터 계좌통합관리 조회 수수료를 건별에서 구간별 정액으로 바꾸려고 합니다. 붙임 개편안을 보시고 기관 의견을 회신해 주세요. 조건부 동의나 이견이면 그 사유와 원하는 적용 시기를 세부의견에 적어 주세요.',
   3, 'OPEN', 'hong.gildong@kftc.example.com', '홍길동', 7, CAST(NULL AS INT), 8, FALSE, CAST(NULL AS VARCHAR(500))),
  ('ACCOUNT_INFO', '오픈뱅킹 연계 전문 항목 추가 검토',
   '계좌정보 통합조회 응답 전문에 계좌 별칭 항목(30바이트)을 더하는 안을 검토하고 있습니다. 기관 시스템 반영 가능 여부와 필요한 개발 기간을 알려 주세요.',
   12, 'OPEN', 'hong.gildong@kftc.example.com', '홍길동', 2, NULL, 3, FALSE, NULL),
  ('ACCOUNT_INFO', '계좌정보 일괄해지 화면 문구 개선',
   '일괄해지 화면의 안내 문구를 고객이 이해하기 쉬운 표현으로 바꾸려고 합니다. 붙임 문구안에 대한 의견을 주세요.',
   -25, 'CLOSED', 'hong.gildong@kftc.example.com', '홍길동', 40, 20, 41, FALSE, NULL),
  ('ACCOUNT_INFO', '계좌통합관리 수수료 고지 방식 변경',
   '수수료 고지서를 우편에서 전자 고지로 바꾸는 안에 대한 의견을 요청합니다.',
   -2, 'CLOSED', 'hong.gildong@kftc.example.com', '홍길동', 15, 10, 16, TRUE,
   '수수료 체계 개편 의견 요청과 함께 다루기로 해서 이 요청은 취소합니다.'),
  ('ACCOUNT_INFO', '야간 배치 시간 조정 의견 요청',
   '야간 정산 배치 시작 시각을 23시에서 23시 30분으로 늦추는 안입니다. 기관 영향 범위를 확인해 주세요.',
   20, 'DRAFT', 'hong.gildong@kftc.example.com', '홍길동', NULL, NULL, 1, FALSE, NULL),
  ('AUTO_TRANSFER', '자동이체 통합조회 해지 절차 개선',
   '자동이체 통합조회에서 해지 신청 뒤 기관 처리 결과를 고객에게 다시 알리는 절차를 더하려고 합니다. 기관 처리 시간과 의견을 회신해 주세요.',
   -1, 'OPEN', 'hong.gildong@kftc.example.com', '홍길동', 9, NULL, 10, FALSE, NULL),
  ('CMS', 'CMS 출금이체 전문 개정 의견 요청',
   'CMS 출금이체 결과 전문의 오류 코드 체계를 정리하려고 합니다. 붙임 코드표를 보고 기관 시스템에 미치는 영향을 알려 주세요.',
   5, 'OPEN', 'park.cms@kftc.example.com', '박하늘', 4, NULL, 5, FALSE, NULL),
  ('LOAN_TRANSFER', '대출이동 신규기관 접수시간 연장',
   '대출이동 신규기관 접수 마감 시각을 17시에서 18시로 늦추는 안에 대한 의견을 요청했습니다.',
   -30, 'CLOSED', 'admin@kftc.example.com', '데모 관리자', 45, 28, 46, FALSE, NULL)
) AS r (business, title, content, due_days, status, owner_email, owner_name, opened_days, closed_days, created_days, cancelled, cancel_reason);

-- 업권별 회신 양식.
INSERT INTO opinion_reply_form (request_id, name, description, sort_order, created_at, created_by, updated_at, updated_by)
SELECT r.id, f.name, f.description, f.sort_order, r.created_at, r.created_by, r.created_at, r.created_by
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', '은행권', '은행·저축은행용 양식입니다. 기관별 월 조회 건수를 함께 적어 주세요.', 0),
  ('계좌통합관리 수수료 체계 개편 의견 요청', '비은행권', '증권·상호금융용 양식입니다.', 1)
) AS f (title, name, description, sort_order)
JOIN opinion_request r ON r.title = f.title;

-- 대상 기관. 양식은 이름으로 잇는다.
INSERT INTO opinion_target (request_id, organization_id, form_id, created_at, created_by, updated_at, updated_by)
SELECT r.id, o.id, f.id, r.created_at, r.created_by, r.created_at, r.created_by
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'GANA',  '은행권'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'DARA',  '은행권'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'MABA',  '은행권'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'CHAKA', '은행권'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'SAA',   '비은행권'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'NADA',  '비은행권'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'GANA',  CAST(NULL AS VARCHAR(50))),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'SAA',   NULL),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'DARA',  NULL),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'CHAKA', NULL),
  ('계좌정보 일괄해지 화면 문구 개선',        'GANA',  NULL),
  ('계좌정보 일괄해지 화면 문구 개선',        'SAA',   NULL),
  ('계좌정보 일괄해지 화면 문구 개선',        'DARA',  NULL),
  ('계좌정보 일괄해지 화면 문구 개선',        'MABA',  NULL),
  ('계좌통합관리 수수료 고지 방식 변경',      'GANA',  NULL),
  ('계좌통합관리 수수료 고지 방식 변경',      'SAA',   NULL),
  ('야간 배치 시간 조정 의견 요청',           'GANA',  NULL),
  ('야간 배치 시간 조정 의견 요청',           'DARA',  NULL),
  ('자동이체 통합조회 해지 절차 개선',        'DARA',  NULL),
  ('자동이체 통합조회 해지 절차 개선',        'GANA',  NULL),
  ('자동이체 통합조회 해지 절차 개선',        'JACHA', NULL),
  ('자동이체 통합조회 해지 절차 개선',        'NADA',  NULL),
  ('CMS 출금이체 전문 개정 의견 요청',        'DARA',  NULL),
  ('CMS 출금이체 전문 개정 의견 요청',        'JACHA', NULL),
  ('CMS 출금이체 전문 개정 의견 요청',        'TAPA',  NULL),
  ('CMS 출금이체 전문 개정 의견 요청',        'NADA',  NULL),
  ('대출이동 신규기관 접수시간 연장',         'GANA',  NULL),
  ('대출이동 신규기관 접수시간 연장',         'MABA',  NULL),
  ('대출이동 신규기관 접수시간 연장',         'TAPA',  NULL),
  ('대출이동 신규기관 접수시간 연장',         'PAHA',  NULL)
) AS t (title, org_code, form_name)
JOIN opinion_request r ON r.title = t.title
JOIN organization o ON o.code = t.org_code
LEFT JOIN opinion_reply_form f ON f.request_id = r.id AND f.name = t.form_name;

-- 회신 담당자. 계정 시드의 기관 담당자이며 그 업무를 맡은 사람만 고른다.
INSERT INTO opinion_target_recipient (target_id, account_id, email, name, sub_business, created_at, created_by, updated_at, updated_by)
SELECT t.id, u.id, u.email, u.name, b.sub_business, t.created_at, t.created_by, t.created_at, t.created_by
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'kim.gana@gana.example.com'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'choi.saa@saa.example.com'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'kim.gana@gana.example.com'),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'choi.saa@saa.example.com'),
  ('계좌정보 일괄해지 화면 문구 개선',        'kim.gana@gana.example.com'),
  ('계좌정보 일괄해지 화면 문구 개선',        'choi.saa@saa.example.com'),
  ('계좌통합관리 수수료 고지 방식 변경',      'kim.gana@gana.example.com'),
  ('야간 배치 시간 조정 의견 요청',           'kim.gana@gana.example.com'),
  ('자동이체 통합조회 해지 절차 개선',        'lee.dara@dara.example.com'),
  ('CMS 출금이체 전문 개정 의견 요청',        'lee.dara@dara.example.com')
) AS p (title, email)
JOIN opinion_request r ON r.title = p.title
JOIN user_account u ON u.email = p.email
JOIN opinion_target t ON t.request_id = r.id AND t.organization_id = u.organization_id
JOIN user_account_business b ON b.account_id = u.id AND b.business = r.business;

-- 기관 회신. submitted_days 가 있으면 제출한 회신이다. 사아증권은 수수료 개편 회신을 제출한 뒤 수정 요청을 해 두었다.
INSERT INTO opinion_reply (target_id, status, reply_type, content, draft_type, draft_content, replier_email, replier_name, submitted_at,
                           revision_reason, revision_requested_at, revision_rejection_reason, created_at, created_by, updated_at, updated_by)
SELECT t.id, p.status, p.reply_type, p.content, p.draft_type, p.draft_content, p.replier_email, p.replier_name,
       CASE WHEN p.submitted_days IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CURRENT_TIMESTAMP - p.submitted_days * INTERVAL '1' DAY END,
       p.revision_reason,
       CASE WHEN p.revision_reason IS NULL THEN CAST(NULL AS TIMESTAMP) ELSE CURRENT_TIMESTAMP - INTERVAL '5' HOUR END,
       CAST(NULL AS VARCHAR(500)),
       CURRENT_TIMESTAMP - COALESCE(p.submitted_days, 1) * INTERVAL '1' DAY, p.replier_email,
       CURRENT_TIMESTAMP - COALESCE(p.submitted_days, 1) * INTERVAL '1' DAY, p.replier_email
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'DARA',  'SUBMITTED', 'AGREE', CAST(NULL AS VARCHAR(4000)), CAST(NULL AS VARCHAR(20)), CAST(NULL AS VARCHAR(4000)),
   'park.dara@dara.example.com', '박다라', 5, CAST(NULL AS VARCHAR(500))),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'MABA',  'SUBMITTED', 'CONDITIONAL',
   '구간별 정액제에 동의합니다. 다만 저축은행은 조회 건수가 적어 최저 구간 금액을 30% 낮춰 주시기를 요청합니다.', NULL, NULL,
   'jung.maba@maba.example.com', '정마바', 4, NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'CHAKA', 'SUBMITTED', 'DISAGREE',
   '올해 예산이 이미 확정되어 1월 적용은 어렵습니다. 적용 시기를 7월로 미뤄 주시기 바랍니다.', NULL, NULL,
   'yoon.chaka@chaka.example.com', '윤차카', 3, NULL),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'SAA',   'REVISION_REQUESTED', 'AGREE', NULL, NULL, NULL,
   'choi.saa@saa.example.com', '최사아', 2, '내부 검토 결과 적용 시기 조건을 더하려고 합니다. 조건부 동의로 바꾸고 싶습니다.'),
  ('계좌통합관리 수수료 체계 개편 의견 요청', 'NADA',  'DRAFT', NULL, NULL, 'CONDITIONAL', '상호금융 중앙회와 협의 중입니다.',
   'kang.nada@nada.example.com', '강나다', NULL, NULL),
  ('오픈뱅킹 연계 전문 항목 추가 검토',       'DARA',  'SUBMITTED', 'AGREE', '다음 분기 정기 배포에 반영할 수 있습니다.', NULL, NULL,
   'park.dara@dara.example.com', '박다라', 1, NULL),
  ('계좌정보 일괄해지 화면 문구 개선',        'GANA',  'SUBMITTED', 'AGREE', NULL, NULL, NULL,
   'kim.gana@gana.example.com', '김가나', 30, NULL),
  ('계좌정보 일괄해지 화면 문구 개선',        'SAA',   'SUBMITTED', 'CONDITIONAL', '"해지" 대신 "연결 끊기" 표현을 함께 써 주세요.', NULL, NULL,
   'choi.saa@saa.example.com', '최사아', 29, NULL),
  ('계좌정보 일괄해지 화면 문구 개선',        'DARA',  'SUBMITTED', 'AGREE', NULL, NULL, NULL,
   'park.dara@dara.example.com', '박다라', 27, NULL),
  ('계좌정보 일괄해지 화면 문구 개선',        'MABA',  'SUBMITTED', 'DISAGREE', '고객 문의가 늘 수 있어 지금 문구를 유지하기를 바랍니다.', NULL, NULL,
   'jung.maba@maba.example.com', '정마바', 26, NULL),
  ('자동이체 통합조회 해지 절차 개선',        'GANA',  'SUBMITTED', 'AGREE', NULL, NULL, NULL,
   'han.gana2@gana.example.com', '한가람', 6, NULL),
  ('자동이체 통합조회 해지 절차 개선',        'JACHA', 'SUBMITTED', 'CONDITIONAL', '처리 결과 통지는 영업일 기준 2일 안으로 해 주세요.', NULL, NULL,
   'yoon.jacha@jacha.example.com', '윤자차', 4, NULL),
  ('CMS 출금이체 전문 개정 의견 요청',        'TAPA',  'SUBMITTED', 'AGREE', NULL, NULL, NULL,
   'jung.tapa@tapa.example.com', '정타파', 1, NULL),
  ('대출이동 신규기관 접수시간 연장',         'GANA',  'SUBMITTED', 'AGREE', NULL, NULL, NULL,
   'kim.gana@gana.example.com', '김가나', 35, NULL),
  ('대출이동 신규기관 접수시간 연장',         'MABA',  'SUBMITTED', 'CONDITIONAL', '18시 연장 시 상담 인력 배치가 필요해 시행을 한 달 늦춰 주세요.', NULL, NULL,
   'jung.maba@maba.example.com', '정마바', 34, NULL),
  ('대출이동 신규기관 접수시간 연장',         'TAPA',  'SUBMITTED', 'AGREE', NULL, NULL, NULL,
   'jung.tapa@tapa.example.com', '정타파', 33, NULL)
) AS p (title, org_code, status, reply_type, content, draft_type, draft_content, replier_email, replier_name, submitted_days, revision_reason)
JOIN opinion_request r ON r.title = p.title
JOIN organization o ON o.code = p.org_code
JOIN opinion_target t ON t.request_id = r.id AND t.organization_id = o.id;

-- 리마인드 메일 이력.
INSERT INTO opinion_reminder (request_id, sender_name, organization_count, recipient_count, organization_names, created_at, created_by, updated_at, updated_by)
SELECT r.id, m.sender_name, m.organization_count, m.recipient_count, m.organization_names,
       CURRENT_TIMESTAMP - m.sent_days * INTERVAL '1' DAY, r.owner_email, CURRENT_TIMESTAMP - m.sent_days * INTERVAL '1' DAY, r.owner_email
FROM (VALUES
  ('계좌통합관리 수수료 체계 개편 의견 요청', '홍길동', 2, 1, '가나은행, 나다상호금융', 2),
  ('자동이체 통합조회 해지 절차 개선',        '홍길동', 1, 1, '다라은행', 1)
) AS m (title, sender_name, organization_count, recipient_count, organization_names, sent_days)
JOIN opinion_request r ON r.title = m.title;
