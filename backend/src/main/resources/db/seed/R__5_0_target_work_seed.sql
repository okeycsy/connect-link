-- 일시중단 대상 업무 기준정보 샘플. 금융결제원의 공개 업무명을 분류별로 넣었고 관련 레일 업무는 시연용 가정이다. 실제 목록은 관리 화면에서 관리자가 정한다.
-- 이미 있는 이름(V25 의 네 업무 포함)은 건드리지 않고 없는 것만 더한다(관리 화면에서 고친 값을 덮지 않는다). 관련 레일 업무도 없는 것만 더한다.
-- 일시중단 샘플(R__5_suspension_seed)보다 먼저 돈다.
INSERT INTO target_work (name, category, sort_order, active, created_at, created_by, updated_at, updated_by)
SELECT t.name, t.category, t.sort_order, TRUE, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('계좌통합관리', '정보서비스', 10),
  ('자동이체통합관리', '정보서비스', 20),
  ('대출이동', '정보서비스', 30),
  ('계좌이동', '정보서비스', 40),
  ('안심차단', '정보서비스', 50),
  ('내 카드 한눈에', '정보서비스', 60),
  ('휴면예금 찾기', '정보서비스', 70),
  ('금융정보 조회', '정보서비스', 80),
  ('CMS', '지급결제', 90),
  ('타행환공동망', '지급결제', 100),
  ('CD공동망', '지급결제', 110),
  ('전자금융공동망', '지급결제', 120),
  ('직불카드공동망', '지급결제', 130),
  ('펌뱅킹', '지급결제', 140),
  ('오픈뱅킹', '지급결제', 150),
  ('외화자금이체', '지급결제', 160),
  ('소액외화송금', '지급결제', 170),
  ('어음교환', '지급결제', 180),
  ('전자어음', '지급결제', 190),
  ('전자채권', '지급결제', 200),
  ('대량지급', '지급결제', 210),
  ('납부자자동이체', '지급결제', 220),
  ('지로', '지로·공과금', 230),
  ('인터넷지로', '지로·공과금', 240),
  ('공과금 자동납부', '지로·공과금', 250),
  ('전자고지', '지로·공과금', 260),
  ('국세 납부', '지로·공과금', 270),
  ('지방세 납부', '지로·공과금', 280),
  ('관세 납부', '지로·공과금', 290),
  ('국고금 지급', '지로·공과금', 300),
  ('계좌간편결제', '전자상거래', 310),
  ('전자금융 결제대행', '전자상거래', 320),
  ('B2B 전자결제', '전자상거래', 330),
  ('현금IC카드', '전자상거래', 340),
  ('모바일현금카드', '전자상거래', 350),
  ('금융인증서', '인증', 360),
  ('공동인증서', '인증', 370),
  ('바이오정보 분산관리', '인증', 380),
  ('전자서명 검증', '인증', 390),
  ('대외계 회선', '대외계·인프라', 400),
  ('금융공동망 백업센터', '대외계·인프라', 410),
  ('파일 일괄전송', '대외계·인프라', 420)
) AS t (name, category, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM target_work w WHERE w.name = t.name);

INSERT INTO target_work_business (target_work_id, business, created_at, created_by, updated_at, updated_by)
SELECT w.id, r.business, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('계좌통합관리', 'ACCOUNT_INFO'),
  ('자동이체통합관리', 'AUTO_TRANSFER'),
  ('대출이동', 'LOAN_TRANSFER'),
  ('계좌이동', 'AUTO_TRANSFER'),
  ('안심차단', 'ACCOUNT_INFO'),
  ('내 카드 한눈에', 'ACCOUNT_INFO'),
  ('휴면예금 찾기', 'ACCOUNT_INFO'),
  ('CMS', 'CMS'),
  ('납부자자동이체', 'AUTO_TRANSFER'),
  ('공과금 자동납부', 'CMS'),
  ('파일 일괄전송', 'ACCOUNT_INFO'),
  ('파일 일괄전송', 'AUTO_TRANSFER'),
  ('파일 일괄전송', 'LOAN_TRANSFER'),
  ('파일 일괄전송', 'CMS')
) AS r (name, business)
JOIN target_work w ON w.name = r.name
WHERE NOT EXISTS (SELECT 1 FROM target_work_business b WHERE b.target_work_id = w.id AND b.business = r.business);
