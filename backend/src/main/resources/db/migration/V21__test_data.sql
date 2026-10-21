-- 다. 기관 테스트: 테스트 전 대상 기관별 테스트데이터 준비. 기관 담당자가 생성 여부(미생성·생성 완료)와 메모를 적고, 만든 테스트데이터 파일은
-- 첨부(owner_type 'TEST_DATA', owner_id = test_plan_target.id)로 올린다. 결제원 담당자는 전 기관 현황을 본다.
ALTER TABLE test_plan_target ADD COLUMN test_data_status VARCHAR(20) DEFAULT 'NOT_CREATED' NOT NULL;
ALTER TABLE test_plan_target ADD COLUMN test_data_note VARCHAR(2000);
ALTER TABLE test_plan_target ADD COLUMN test_data_reported_by_email VARCHAR(200);
ALTER TABLE test_plan_target ADD COLUMN test_data_reported_by_name VARCHAR(50);
ALTER TABLE test_plan_target ADD COLUMN test_data_reported_at TIMESTAMP;
ALTER TABLE test_plan_target ADD CONSTRAINT ck_test_plan_target_test_data_status CHECK (test_data_status IN ('NOT_CREATED', 'CREATED'));

-- 테스트데이터는 수행 기간 전에 따로 받는다. 결제원 담당자가 테스트마다 요청 여부, 회신 기한(수행 시작일 이전), 요청 사항을 정하고,
-- 요청 메일을 보낸 마지막 일시를 남긴다(미생성 기관에 다시 보낼 수 있다).
ALTER TABLE test_plan ADD COLUMN test_data_requested BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE test_plan ADD COLUMN test_data_due_date DATE;
ALTER TABLE test_plan ADD COLUMN test_data_instruction VARCHAR(2000);
ALTER TABLE test_plan ADD COLUMN test_data_requested_at TIMESTAMP;
