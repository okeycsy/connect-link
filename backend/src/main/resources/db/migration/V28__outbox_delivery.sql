-- 발송함의 실제 발송. 발송기가 PENDING 건을 메일(SMTP)·매터모스트(웹훅)로 보내고 SENT 로 바꾼다.
-- 실패하면 횟수와 마지막 오류를 남기고 다시 시도하며, 정해진 횟수를 넘기면 FAILED 다.
ALTER TABLE notification_outbox ADD COLUMN attempts INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE notification_outbox ADD COLUMN last_error VARCHAR(500);

CREATE INDEX ix_notification_outbox_status ON notification_outbox (status, id);
