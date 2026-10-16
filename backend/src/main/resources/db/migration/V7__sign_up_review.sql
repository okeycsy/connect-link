-- 가입 승인. 신청 일시(반려 뒤 다시 신청하면 새로 적는다), 승인·반려한 결제원 담당자와 일시, 반려 사유를 계정에 둔다.
-- 처리한 사람은 그때의 이메일과 이름을 그대로 남긴다(뒤에 이름이 바뀌어도 처리 기록은 그대로다).
ALTER TABLE user_account ADD COLUMN applied_at TIMESTAMP;
UPDATE user_account SET applied_at = created_at;
ALTER TABLE user_account ALTER COLUMN applied_at SET NOT NULL;

ALTER TABLE user_account ADD COLUMN reviewed_at TIMESTAMP;
ALTER TABLE user_account ADD COLUMN reviewer_email VARCHAR(200);
ALTER TABLE user_account ADD COLUMN reviewer_name VARCHAR(50);
ALTER TABLE user_account ADD COLUMN rejection_reason VARCHAR(500);

CREATE INDEX ix_user_account_status_applied_at ON user_account (status, applied_at);
