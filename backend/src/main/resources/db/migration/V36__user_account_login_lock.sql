-- 로그인 실패 잠금(개인정보의 안전성 확보조치 기준 제5조). 연달아 틀린 횟수와 잠금이 풀리는 일시.
ALTER TABLE user_account ADD COLUMN failed_login_count INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE user_account ADD COLUMN locked_until TIMESTAMP;
