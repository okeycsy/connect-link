-- 가입 때 남기는 개인정보 처리 기록. 필수 항목은 계약 이행(개인정보 보호법 제15조 제1항 제4호)으로 처리해 동의 대신 수집·이용 안내를 확인한
-- 일시와 그때 처리방침 시행일(판)을 남기고, 휴대폰번호는 선택 동의(같은 항 제1호)라 동의한 일시를 따로 둔다.
-- 이 기능 전에 만든 계정(샘플·기존 가입)은 기록이 없다.
ALTER TABLE user_account ADD COLUMN privacy_confirmed_at TIMESTAMP;
ALTER TABLE user_account ADD COLUMN privacy_policy_version VARCHAR(20);
ALTER TABLE user_account ADD COLUMN mobile_phone_agreed_at TIMESTAMP;
