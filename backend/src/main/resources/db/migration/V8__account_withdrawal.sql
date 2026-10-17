-- 탈퇴. 탈퇴한 계정 행은 지우지 않고 탈퇴 일시와 함께 남긴다(가입 승인 기록과 탈퇴 건수를 위해).
-- 이름·부서·연락처·담당 업무 같은 개인 정보는 탈퇴할 때 지우고, 회사 이메일은 같은 이메일로 다시 가입할 수 있게 계정마다 다른 빈 주소로 바꾼다.
-- 결제원 담당자가 탈퇴 처리했으면 처리한 사람(그때의 이메일과 이름)과 사유를 남긴다. 본인이 탈퇴했으면 비어 있다.
ALTER TABLE user_account ADD COLUMN withdrawn_at TIMESTAMP;
ALTER TABLE user_account ADD COLUMN withdrawn_by_email VARCHAR(200);
ALTER TABLE user_account ADD COLUMN withdrawn_by_name VARCHAR(50);
ALTER TABLE user_account ADD COLUMN withdrawal_reason VARCHAR(500);
