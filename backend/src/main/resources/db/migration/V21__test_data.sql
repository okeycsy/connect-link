-- 다. 기관 테스트: 테스트 전 대상 기관별 테스트데이터 준비. 기관 담당자가 생성 여부(미생성·생성 완료)와 메모를 적고, 만든 테스트데이터 파일은
-- 첨부(owner_type 'TEST_DATA', owner_id = test_plan_target.id)로 올린다. 결제원 담당자는 전 기관 현황을 본다.
ALTER TABLE test_plan_target ADD COLUMN test_data_status VARCHAR(20) DEFAULT 'NOT_CREATED' NOT NULL;
ALTER TABLE test_plan_target ADD COLUMN test_data_note VARCHAR(2000);
ALTER TABLE test_plan_target ADD COLUMN test_data_reported_by_email VARCHAR(200);
ALTER TABLE test_plan_target ADD COLUMN test_data_reported_by_name VARCHAR(50);
ALTER TABLE test_plan_target ADD COLUMN test_data_reported_at TIMESTAMP;
ALTER TABLE test_plan_target ADD CONSTRAINT ck_test_plan_target_test_data_status CHECK (test_data_status IN ('NOT_CREATED', 'CREATED'));
