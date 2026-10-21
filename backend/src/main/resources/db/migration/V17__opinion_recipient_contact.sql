-- 의견 요청 회신 담당자를 계정이 아니라 담당자 연락처에서 고른다. 연락처와 계정은 회사 이메일로만 잇기 때문에
-- 회신 담당자도 이메일을 키로 삼고, 계정이 없는 담당자(후임자 등)도 고를 수 있게 계정 ID 칼럼을 없앤다.
ALTER TABLE opinion_target_recipient DROP CONSTRAINT uk_opinion_target_recipient;
ALTER TABLE opinion_target_recipient DROP COLUMN account_id;
ALTER TABLE opinion_target_recipient ADD CONSTRAINT uk_opinion_target_recipient UNIQUE (target_id, email);
