-- 공지에 관련 의견 요청 하나를 잇는다(예: 회의 개최 안내 + 참석 회신 요청). 의견 요청 모듈과 따로 지우므로 외래 키를 두지 않는다.
ALTER TABLE notice ADD COLUMN opinion_request_id BIGINT;
