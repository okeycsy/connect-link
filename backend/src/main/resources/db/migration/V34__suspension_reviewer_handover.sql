-- 조치 및 인수인계 사항을 확인한 결제원 담당자마다 따로 둔다. 전에는 일시중단에 한 칸이라 다음 사람이 쓰면 앞 사람 것을 덮었다.
ALTER TABLE suspension_reviewer ADD COLUMN handover VARCHAR(2000);
ALTER TABLE suspension_reviewer ADD COLUMN handover_at TIMESTAMP;

-- 이미 쓴 사항은 그것을 쓴(확정한) 담당자의 행으로 옮긴다.
UPDATE suspension_reviewer
SET handover = (SELECT s.handover FROM suspension s
                WHERE s.id = suspension_reviewer.suspension_id AND s.confirmed_by_email = suspension_reviewer.email),
    handover_at = (SELECT s.confirmed_at FROM suspension s
                   WHERE s.id = suspension_reviewer.suspension_id AND s.confirmed_by_email = suspension_reviewer.email)
WHERE EXISTS (SELECT 1 FROM suspension s
              WHERE s.id = suspension_reviewer.suspension_id
                AND s.confirmed_by_email = suspension_reviewer.email
                AND s.handover IS NOT NULL);

ALTER TABLE suspension DROP COLUMN handover;
