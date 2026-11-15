-- 결제원 담당자가 올린 첨부파일의 반출 결재(같은 팀 팀장). 참가기관은 외부망에서 보므로 결제원 파일은 외부망 반출이다.
-- 결재가 필요 없는 파일(참가기관이 올린 파일, 이 변경 전에 올린 파일)은 NOT_REQUIRED 다.
ALTER TABLE attachment ADD COLUMN approval_status VARCHAR(20) DEFAULT 'NOT_REQUIRED' NOT NULL;
ALTER TABLE attachment ADD COLUMN uploader_name VARCHAR(50);
ALTER TABLE attachment ADD COLUMN uploader_team VARCHAR(100);
ALTER TABLE attachment ADD COLUMN approver_email VARCHAR(200);
ALTER TABLE attachment ADD COLUMN approver_name VARCHAR(50);
ALTER TABLE attachment ADD COLUMN decided_at TIMESTAMP;
ALTER TABLE attachment ADD COLUMN rejection_reason VARCHAR(500);
CREATE INDEX idx_attachment_approver ON attachment (approver_email, approval_status);
CREATE INDEX idx_attachment_uploader ON attachment (created_by, approval_status);
ALTER TABLE attachment ADD CONSTRAINT ck_attachment_approval_status CHECK (approval_status IN ('NOT_REQUIRED', 'PENDING', 'APPROVED', 'REJECTED'));
