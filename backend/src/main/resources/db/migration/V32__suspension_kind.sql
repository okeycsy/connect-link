-- 일시중단 종류. 기관 일시중단(ORGANIZATION)과 결제원 작업 안내(KFTC_WORK). 결제원 작업의 기관은 결제원이다.
ALTER TABLE suspension ADD COLUMN kind VARCHAR(20) DEFAULT 'ORGANIZATION' NOT NULL;
