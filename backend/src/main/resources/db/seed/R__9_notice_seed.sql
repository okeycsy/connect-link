-- 공지·자료실 샘플. 전부 가상 데이터다(규약·시행세칙 이름과 내용도 예시다). local, demo 프로필(과 테스트)에서만 읽는다.
-- 전 업무 공통 공지 둘(하나는 고정), 계좌통합관리에 공지 둘(하나는 회의 개최 안내)·규약·시행세칙, 그 밖 업무에 공지 하나씩이 있다.
DELETE FROM attachment WHERE owner_type = 'NOTICE';
DELETE FROM notice;

INSERT INTO notice (business, category, title, content, pinned, effective_date, author_email, author_name,
                    created_at, created_by, updated_at, updated_by)
VALUES
  (NULL, 'NOTICE', '연결이 이용 안내',
   '연결이는 금융결제원 업무 담당자와 참가기관 담당자가 연락처, 의견 회신, 기관 테스트, 일시중단을 함께 관리하는 곳입니다.
처음 쓰시는 분은 로그인 화면 아래 이용 가이드를 먼저 읽어 주세요.',
   TRUE, NULL, 'admin@kftc.example.com', '데모 관리자',
   CURRENT_TIMESTAMP - 20 * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - 20 * INTERVAL '1' DAY, 'seed'),
  (NULL, 'NOTICE', '연말 시스템 정기 점검 일정 안내',
   '12월 마지막 주말에 정기 점검이 있습니다. 점검 시간에는 연결이 접속이 잠시 끊길 수 있습니다. 자세한 시간은 점검 일주일 전에 다시 알려 드립니다.',
   FALSE, NULL, 'admin@kftc.example.com', '데모 관리자',
   CURRENT_TIMESTAMP - 3 * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - 3 * INTERVAL '1' DAY, 'seed'),
  ('ACCOUNT_INFO', 'RULE', '계좌통합관리업무 규약(예시)',
   '계좌통합관리업무에 참가하는 기관과 금융결제원의 권리·의무를 정한 규약입니다(샘플 문서).
개정 사항은 첨부 파일의 신구 대비표를 참고해 주세요.',
   TRUE, CAST(CURRENT_DATE - 60 * INTERVAL '1' DAY AS DATE), 'hong.sungmin@kftc.example.com', '홍성민',
   CURRENT_TIMESTAMP - 60 * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - 60 * INTERVAL '1' DAY, 'seed'),
  ('ACCOUNT_INFO', 'ENFORCEMENT_RULE', '계좌통합관리업무 시행세칙(예시)',
   '규약을 시행하는 데 필요한 처리 절차, 전문 형식, 장애 시 연락 체계를 정한 시행세칙입니다(샘플 문서).',
   FALSE, CAST(CURRENT_DATE - 60 * INTERVAL '1' DAY AS DATE), 'hong.sungmin@kftc.example.com', '홍성민',
   CURRENT_TIMESTAMP - 59 * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - 59 * INTERVAL '1' DAY, 'seed'),
  ('ACCOUNT_INFO', 'NOTICE', '본인계좌 일괄지급정지 전문 개선 일정 안내',
   '다음 달부터 본인계좌 일괄지급정지 전문 일부 항목이 바뀝니다. 기관 테스트 메뉴의 "본인계좌 일괄지급정지 전문 개선 2차" 테스트에 결과를 등록해 주세요.',
   FALSE, NULL, 'hong.sungmin@kftc.example.com', '홍성민',
   CURRENT_TIMESTAMP - 7 * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - 7 * INTERVAL '1' DAY, 'seed'),
  ('ACCOUNT_INFO', 'NOTICE', '계좌통합관리업무 전문 변경 회의 개최 안내',
   '다음 달 셋째 주에 전문 변경 회의를 엽니다. 일시와 장소는 첨부 파일을 참고해 주세요.
참석 여부와 참석자는 아래 "회신하기"로 알려 주세요.',
   FALSE, NULL, 'hong.sungmin@kftc.example.com', '홍성민',
   CURRENT_TIMESTAMP - 1 * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - 1 * INTERVAL '1' DAY, 'seed'),
  ('AUTO_TRANSFER', 'NOTICE', '자동이체 출금동의 확인 절차 변경 안내',
   '출금동의 확인 절차가 바뀝니다. 변경 내용은 자료실의 시행세칙을 참고해 주세요.',
   FALSE, NULL, 'hong.sungmin@kftc.example.com', '홍성민',
   CURRENT_TIMESTAMP - 5 * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - 5 * INTERVAL '1' DAY, 'seed'),
  ('CMS', 'NOTICE', 'CMS 정산 마감 시각 안내',
   'CMS 정산 마감 시각을 다시 안내드립니다. 마감 뒤 들어온 건은 다음 영업일에 처리됩니다.',
   FALSE, NULL, 'park.cms@kftc.example.com', '박하늘',
   CURRENT_TIMESTAMP - 2 * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - 2 * INTERVAL '1' DAY, 'seed');

-- 회의 개최 안내는 참석 회신을 받는 의견 요청(R__4)을 잇는다.
UPDATE notice
SET opinion_request_id = (SELECT r.id FROM opinion_request r WHERE r.title = '계좌통합관리업무 전문 변경 회의 참석 회신')
WHERE title = '계좌통합관리업무 전문 변경 회의 개최 안내';
