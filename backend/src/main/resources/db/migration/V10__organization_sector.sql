-- 참가기관의 업권(은행, 저축은행, 증권 …). 결제원·이용기관은 비워 둔다.
ALTER TABLE organization ADD COLUMN sector VARCHAR(30);
