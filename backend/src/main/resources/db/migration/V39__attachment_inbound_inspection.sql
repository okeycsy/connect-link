-- 결제원 밖 계정(참가기관 담당자 등)이 올린 첨부파일의 반입 보안 검사. 외부망에서 올라온 파일을 결제원이 내부망에서 열어 보므로 검사를 통과해야 내려받는다.
-- 검사가 필요 없는 파일(결제원 계정이 올린 파일, 이 변경 전에 올린 파일)은 NOT_REQUIRED 다.
ALTER TABLE attachment ADD COLUMN inspection_status VARCHAR(20) DEFAULT 'NOT_REQUIRED' NOT NULL;
ALTER TABLE attachment ADD COLUMN inspection_requested_at TIMESTAMP;
ALTER TABLE attachment ADD COLUMN inspected_at TIMESTAMP;
ALTER TABLE attachment ADD COLUMN inspection_note VARCHAR(500);
CREATE INDEX idx_attachment_inspection ON attachment (inspection_status);
ALTER TABLE attachment ADD CONSTRAINT ck_attachment_inspection_status CHECK (inspection_status IN ('NOT_REQUIRED', 'PENDING', 'PASSED', 'BLOCKED'));
