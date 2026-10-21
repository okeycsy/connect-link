-- 글쓴이 직급. 결제원 의견·기관 결과·확정 기록처럼 사람이 쓴 내용을 "결제원(김민찬 과장)", "KB국민은행(김민준 대리)"처럼 보이려고
-- 쓴 그때의 직급을 이름 옆에 남긴다(이름처럼 비정규화, 계정이 바뀌거나 탈퇴해도 그대로). 이 칼럼이 생기기 전에 쓴 건은 비어 있고 이름만 보인다.
ALTER TABLE test_item_result ADD COLUMN reported_by_job_title VARCHAR(50);
ALTER TABLE test_item_result ADD COLUMN feedback_by_job_title VARCHAR(50);
ALTER TABLE test_plan_target ADD COLUMN test_data_reported_by_job_title VARCHAR(50);
ALTER TABLE suspension ADD COLUMN registrant_job_title VARCHAR(50);
ALTER TABLE suspension ADD COLUMN confirmed_by_job_title VARCHAR(50);
