-- 탈퇴. 탈퇴한 계정 행은 지우지 않고 탈퇴 일시와 함께 남긴다(가입 승인 기록과 탈퇴 건수를 위해).
-- 이름·부서·연락처·담당 업무 같은 개인 정보는 탈퇴할 때 지우고, 회사 이메일은 같은 이메일로 다시 가입할 수 있게 계정마다 다른 빈 주소로 바꾼다.
ALTER TABLE user_account ADD COLUMN withdrawn_at TIMESTAMP;
