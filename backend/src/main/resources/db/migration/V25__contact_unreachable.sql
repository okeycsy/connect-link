-- 연락 불가 표시. 야간 관제 등 결제원 담당자가 담당자에게 전화했는데 닿지 않으면 그 행에 남겨 두고, 담당자 정보를 고치거나 연락이 닿아 해제하면 지운다.
-- 한 행에 지금 표시 하나만 둔다(지난 표시·해제는 변경 이력에 남는다). 남기는 사람은 결제원 담당자라 "결제원(이수진 주임)" 처럼 보이려고 그때의 이름·직급을 남긴다.
ALTER TABLE contact ADD COLUMN unreachable_at TIMESTAMP;
ALTER TABLE contact ADD COLUMN unreachable_note VARCHAR(500);
ALTER TABLE contact ADD COLUMN unreachable_by_email VARCHAR(200);
ALTER TABLE contact ADD COLUMN unreachable_by_name VARCHAR(50);
ALTER TABLE contact ADD COLUMN unreachable_by_job_title VARCHAR(50);
